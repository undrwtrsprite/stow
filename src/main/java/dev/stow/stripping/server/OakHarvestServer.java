package dev.stow.stripping.server;

import dev.stow.stripping.StrippableLogs;
import dev.stow.stripping.protocol.OakHarvestProtocol;
import dev.stow.stripping.protocol.OakHarvestProtocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Optional companion. Every mutation uses the player's normal server interaction methods. */
public final class OakHarvestServer implements ModInitializer {
    private final Map<UUID,Job> jobs=new LinkedHashMap<>();
    private final Map<UUID,Long> lastRequest=new HashMap<>();
    private int maxLogs=16384;
    private long ticks;
    private static final int WORK_PER_TICK=512;
    private static final class Job {
        final ServerPlayer player;
        final ServerLevel level;
        final Request request;
        final ItemStack axe;
        final ConnectedLogs<BlockPos,BlockState> scan;
        List<Map.Entry<BlockPos,BlockState>> targets;
        int index,stripped,mined,age,validated;
        boolean mining,prepared;
        Job(ServerPlayer p,Request r,int limit){
            player=p;level=p.level();request=r;axe=identity(p.getMainHandItem());
            var log=level.getBlockState(r.anchor()).getBlock();
            scan=new ConnectedLogs<>(r.anchor(),limit,OakHarvestServer::neighbors,
                pos->{var state=level.getBlockState(pos);return state.is(log)?state:null;},
                pos->level.isOutsideBuildHeight(pos)||level.hasChunkAt(pos));
        }
    }
    @Override public void onInitialize(){
        loadConfig();OakHarvestProtocol.register();
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(r,c)->c.server().execute(()->request(c.player(),r)));
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{jobs.remove(h.player.getUUID());lastRequest.remove(h.player.getUUID());});
        ServerLifecycleEvents.SERVER_STOPPED.register(s->{jobs.clear();lastRequest.clear();ticks=0;});
    }
    private void loadConfig(){
        Path file=FabricLoader.getInstance().getConfigDir().resolve("stow-companion.properties");
        var properties=new Properties();
        try{
            if(Files.exists(file)){try(var in=Files.newInputStream(file)){properties.load(in);}}
            else {properties.setProperty("maxLogs","16384");try(var out=Files.newOutputStream(file)){properties.store(out,"Maximum connected logs per request (1..65536). No height or length limit.");}}
            maxLogs=Math.clamp(Integer.parseInt(properties.getProperty("maxLogs","16384")),1,65536);
        }catch(java.io.IOException|NumberFormatException e){System.err.println("[stow companion] Cannot read config; using 16384 logs: "+e.getMessage());}
    }
    private static ItemStack identity(ItemStack item){var copy=item.copy();if(copy.isDamageableItem())copy.setDamageValue(0);return copy;}
    private static boolean ready(ServerPlayer p){
        var mode=p.gameMode.getGameModeForPlayer();
        return p.isAlive()&&!p.hasDisconnected()&&(mode==GameType.SURVIVAL||mode==GameType.CREATIVE)
            &&p.containerMenu==p.inventoryMenu&&p.inventoryMenu.getCarried().isEmpty()&&!p.isUsingItem()
            &&p.getMainHandItem().is(ItemTags.AXES);
    }
    private static Iterable<BlockPos> neighbors(BlockPos pos){
        var result=new ArrayList<BlockPos>(26);
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)
            if(x!=0||y!=0||z!=0)result.add(pos.offset(x,y,z));
        return result;
    }
    private static boolean permitted(ServerPlayer p,BlockPos pos){
        var level=p.level();
        return !level.isOutsideBuildHeight(pos)&&level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos)
            &&level.mayInteract(p,pos)&&!p.blockActionRestricted(level,pos,p.gameMode.getGameModeForPlayer());
    }
    private static boolean visible(ServerPlayer p,BlockPos pos){
        if(!p.isWithinBlockInteractionRange(pos,0))return false;
        var eye=p.getEyePosition();var center=Vec3.atCenterOf(pos);
        for(var face:Direction.values()){
            var point=center.add(face.getStepX()*0.499,face.getStepY()*0.499,face.getStepZ()*0.499);
            var hit=p.level().clip(new ClipContext(eye,point,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,p));
            if(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(pos)
                &&eye.distanceToSqr(hit.getLocation())<=p.blockInteractionRange()*p.blockInteractionRange())return true;
        }
        return false;
    }
    private static void reply(ServerPlayer p,Request r,String status,int stripped,int mined){
        if(!p.hasDisconnected()&&ServerPlayNetworking.canSend(p,Result.TYPE))ServerPlayNetworking.send(p,new Result(r.id(),status,stripped,mined));
    }
    private void request(ServerPlayer p,Request r){
        var active=jobs.get(p.getUUID());
        if(r.cancel()){
            if(active!=null&&active.request.id()==r.id()){jobs.remove(p.getUUID());reply(p,r,"cancelled",active.stripped,active.mined);}return;
        }
        if(active!=null){reply(p,r,"busy",0,0);return;}
        long last=lastRequest.getOrDefault(p.getUUID(),ticks-10);
        if(ticks-last<10){reply(p,r,"busy",0,0);return;}
        lastRequest.put(p.getUUID(),ticks);
        if(!ready(p)||r.slot()!=p.getInventory().getSelectedSlot()){reply(p,r,"cancelled",0,0);return;}
        if(!permitted(p,r.anchor())||!visible(p,r.anchor())||!StrippableLogs.canStrip(p.level().getBlockState(r.anchor()))){
            reply(p,r,"aim",0,0);return;
        }
        jobs.put(p.getUUID(),new Job(p,r,maxLogs));
    }
    private void tick(MinecraftServer server){
        ticks++;int budget=WORK_PER_TICK;
        var iterator=jobs.values().iterator();
        while(iterator.hasNext()){
            var j=iterator.next();String status=null;
            if(++j.age>2400||!ready(j.player)||j.player.level()!=j.level
                ||j.request.slot()!=j.player.getInventory().getSelectedSlot()
                ||!ItemStack.isSameItemSameComponents(identity(j.player.getMainHandItem()),j.axe))status="cancelled";
            if(status==null&&j.targets==null&&budget>0){
                budget-=j.scan.advance(budget);
                if(j.scan.done()){
                    status=j.scan.error();
                    if(status==null){
                        j.targets=new ArrayList<>(j.scan.logs().entrySet());
                        // Reserve both strip and mine costs, leaving one durability point.
                        var axe=j.player.getMainHandItem();
                        if(!j.player.hasInfiniteMaterials()&&axe.isDamageableItem()
                            &&axe.getMaxDamage()-axe.getDamageValue()<=2L*j.targets.size())status="durability";
                    }
                }
            }
            // Validate the entire snapshot before changes, without a large unbudgeted preflight loop.
            while(status==null&&j.targets!=null&&!j.prepared&&budget>0){
                if(j.validated==j.targets.size()){j.prepared=true;break;}
                var target=j.targets.get(j.validated++);budget--;
                if(!permitted(j.player,target.getKey())||j.level.getBlockState(target.getKey())!=target.getValue())status="rejected";
            }
            while(status==null&&j.targets!=null&&j.prepared&&budget>0){
                if(!ready(j.player)||j.player.level()!=j.level
                    ||j.request.slot()!=j.player.getInventory().getSelectedSlot()
                    ||!ItemStack.isSameItemSameComponents(identity(j.player.getMainHandItem()),j.axe)){
                    status="cancelled";break;
                }
                if(j.index==j.targets.size()){
                    if(j.mining){status="done";break;}
                    j.mining=true;j.index=0;
                }
                var target=j.targets.get(j.index++);var pos=target.getKey();budget--;
                if(!permitted(j.player,pos)){status="rejected";break;}
                var state=j.level.getBlockState(pos);
                if(j.mining&&state.isAir())continue; // Timber may already have harvested this position.
                var expected=j.mining?StrippableLogs.strippedState(target.getValue()):target.getValue();
                if(state!=expected){status="rejected";break;}
                var axe=j.player.getMainHandItem();
                if(!j.player.hasInfiniteMaterials()&&axe.isDamageableItem()&&axe.getMaxDamage()-axe.getDamageValue()<=1){status="durability";break;}
                if(j.mining){
                    if(!j.player.gameMode.destroyBlock(pos)){status="rejected";break;}
                    j.mined++;
                }else{
                    var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
                    var result=j.player.gameMode.useItemOn(j.player,j.level,axe,InteractionHand.MAIN_HAND,hit);
                    if(!result.consumesAction()||j.level.getBlockState(pos)!=StrippableLogs.strippedState(state)){
                        status="rejected";break;
                    }
                    j.stripped++;
                }
            }
            if(status!=null){iterator.remove();reply(j.player,j.request,status,j.stripped,j.mined);}
        }
    }
}
