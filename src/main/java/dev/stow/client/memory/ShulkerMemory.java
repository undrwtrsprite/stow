package dev.stow.client.memory;

import dev.stow.Stow;
import dev.stow.client.mixin.ClientLevelAccessor;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import static dev.stow.client.memory.ChestMemoryStore.*;

/** Track known placed-box contents and authoritative removals without scanning the world. */
public final class ShulkerMemory {
    private record Placement(ClientLevel level,BlockPos pos,Block block,String title,String color,List<MemoryItem> items,long until,int sequence) {}
    private static final Map<BlockPos,Placement> placements=new LinkedHashMap<>();
    private static Placement prepared;
    private ShulkerMemory(){}

    public static void register(){
        ClientChunkEvents.CHUNK_LOAD.register((level,chunk)->{
            if(level!=Minecraft.getInstance().level)return;
            var store=ChestMemory.currentStore();if(store==null)return;
            for(var chest:store.shulkersInChunk(level.dimension().identifier().toString(),chunk.getPos().x(),chunk.getPos().z()))
                updated(chest.location().pos(),chunk.getBlockState(chest.location().pos()));
        });
    }
    public static String color(BlockState state){
        if(!(state.getBlock() instanceof ShulkerBoxBlock box))return null;
        return box.getColor()==null?"undyed":box.getColor().getName();
    }
    public static void prepare(LocalPlayer player,InteractionHand hand,BlockHitResult hit){
        prepared=null;var mc=Minecraft.getInstance();var stack=player.getItemInHand(hand);
        if(mc.level==null||!(stack.getItem() instanceof BlockItem item)||!(item.getBlock() instanceof ShulkerBoxBlock))return;
        var context=new BlockPlaceContext(player,hand,stack,hit);if(!context.canPlace())return;
        var pos=context.getClickedPos().immutable();
        if(mc.level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock)return;
        Map<String,MemoryItem> contents=new LinkedHashMap<>();
        stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).itemCopies().limit(27).filter(s->!s.isEmpty()).forEach(s->{
            String id=BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),name=s.getHoverName().getString(),key=id+'\0'+name;
            var old=contents.get(key);contents.put(key,new MemoryItem(id,name,s.getCount()+(old==null?0:old.count())));
        });
        prepared=new Placement(mc.level,pos,item.getBlock(),stack.getHoverName().getString(),color(item.getBlock().defaultBlockState()),List.copyOf(contents.values()),System.nanoTime()+5_000_000_000L,-1);
    }
    public static void placed(InteractionResult result){
        var candidate=prepared;prepared=null;var mc=Minecraft.getInstance();
        if(candidate==null||!result.consumesAction()||mc.level!=candidate.level
                ||mc.level.getBlockState(candidate.pos).getBlock()!=candidate.block)return;
        int sequence=((ClientLevelAccessor)mc.level).stow_predictionHandler().currentSequence();
        placements.put(candidate.pos,new Placement(candidate.level,candidate.pos,candidate.block,candidate.title,candidate.color,candidate.items,candidate.until,sequence));
        while(placements.size()>64)placements.remove(placements.keySet().iterator().next());
    }
    /** Called only for server packets or completed chunk data, never client block prediction. */
    public static void updated(BlockPos pos,BlockState state){
        var mc=Minecraft.getInstance();var store=ChestMemory.observedStore();if(store==null||mc.level==null)return;
        var location=new Location(mc.level.dimension().identifier().toString(),pos.getX(),pos.getY(),pos.getZ(),"Shulker box");
        var old=store.get(location);String color=color(state);
        if(old!=null&&old.location().kind().equals("Shulker box")
                &&(color==null||old.shulkerColor()!=null&&!old.shulkerColor().equals(color))){
            ChestMemory.shulkerRemoved(old.location());
            if(store.invalidateShulker(location))ChestMemory.saveLater(store);
        }
        // A queued pre-placement air update must not discard a newer placement candidate.
        var placed=placements.get(pos);
        if(placed!=null&&placed.level==mc.level&&System.nanoTime()<placed.until&&state.getBlock()==placed.block){
            placements.remove(pos);
            store.remember(new SavedChest(location,placed.title,System.currentTimeMillis(),placed.items,placed.color),Stow.config.autoAssignChests);
            StorageOwners.request(store,location,dev.stow.companion.protocol.StorageOwnerProtocol.QUERY);
            ChestMemory.saveLater(store);
        }
    }
    public static void tick(Minecraft mc){
        if(!placements.isEmpty()){long now=System.nanoTime();placements.values().removeIf(p->p.level!=mc.level||now>=p.until);}
    }
    public static void acknowledged(Minecraft mc,int sequence){
        if(mc.level==null||placements.isEmpty())return;
        // Vanilla has now reconciled these predictions with the server's verified block state.
        for(var p:List.copyOf(placements.values()))if(p.level==mc.level&&p.sequence<=sequence&&mc.level.hasChunkAt(p.pos))
            {updated(p.pos,mc.level.getBlockState(p.pos));placements.remove(p.pos);}
    }
    public static void clear(){prepared=null;placements.clear();}
}
