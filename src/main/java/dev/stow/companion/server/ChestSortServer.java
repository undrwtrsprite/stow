package dev.stow.companion.server;

import dev.stow.companion.protocol.ChestSortProtocol;
import dev.stow.companion.protocol.ChestSortProtocol.*;
import static dev.stow.companion.protocol.ChestSortProtocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;

/** Validated one-tick reordering of ordinary storage, without a client click queue. */
public final class ChestSortServer implements ModInitializer {
    private final Map<UUID,Integer> lastRequest=new HashMap<>();
    @Override public void onInitialize(){
        ChestSortProtocol.register();if(!enabled())return;
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(r,c)->c.server().execute(()->{
            var player=c.player();int last=lastRequest.getOrDefault(player.getUUID(),player.tickCount-2);
            int status;
            if(player.tickCount-last<2)status=BUSY;
            else{lastRequest.put(player.getUUID(),player.tickCount);status=apply(player,r);}
            player.containerMenu.sendAllDataToRemote();
            if(ServerPlayNetworking.canSend(player,Result.TYPE))ServerPlayNetworking.send(player,new Result(r.id(),status));
        }));
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->lastRequest.remove(h.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->lastRequest.clear());
    }
    private static boolean enabled(){
        var file=FabricLoader.getInstance().getConfigDir().resolve("stow-companion.properties");var properties=new Properties();
        try{
            if(Files.exists(file))try(var in=Files.newInputStream(file)){properties.load(in);}
            if(!properties.containsKey("fastChestSorting")){
                properties.setProperty("fastChestSorting","true");
                try(var out=Files.newOutputStream(file)){properties.store(out,"stow companion: fastChestSorting enables validated vanilla-storage sorting; maxLogs limits harvesting.");}
            }
            return !"false".equalsIgnoreCase(properties.getProperty("fastChestSorting","true").trim());
        }catch(java.io.IOException e){System.err.println("[stow companion] Cannot read fast-sort configuration: "+e.getMessage());return true;}
    }
    private static boolean supported(AbstractContainerMenu menu){
        if(menu.getClass()!=ChestMenu.class&&menu.getClass()!=ShulkerBoxMenu.class||menu.slots.isEmpty())return false;
        var type=menu.getSlot(0).container.getClass();
        return type==ChestBlockEntity.class||type==BarrelBlockEntity.class||type==ShulkerBoxBlockEntity.class
            ||type==CompoundContainer.class||type==PlayerEnderChestContainer.class;
    }
    /** All checks finish before any slot changes. Client-provided stacks can only redistribute existing items. */
    public static int apply(Player player,Request request){
        var menu=player.containerMenu;
        if(!player.isAlive()||player.isSpectator()||menu.containerId!=request.menuId()||!menu.stillValid(player)||!menu.getCarried().isEmpty())return CHANGED;
        if(!supported(menu))return FALLBACK;
        if(menu.getStateId()!=request.stateId()||request.changes().isEmpty()||request.changes().size()>MAX_SLOTS)return CHANGED;
        Container storage=menu.getSlot(0).container;var seen=new HashSet<Integer>();var available=new ArrayList<ItemStack>();
        for(var change:request.changes()){
            if(change.slot()<0||change.slot()>=menu.slots.size()||!seen.add(change.slot()))return CHANGED;
            var slot=menu.getSlot(change.slot());var result=change.after();
            if(slot.container!=storage||!slot.isActive()||!slot.mayPickup(player)||!ItemStack.matches(slot.getItem(),change.before()))return CHANGED;
            if(!result.isEmpty()&&(!slot.mayPlace(result)||result.getCount()<1||result.getCount()>slot.getMaxStackSize(result)))return CHANGED;
            available.add(slot.getItem().copy());
        }
        for(var change:request.changes()){
            var result=change.after();int needed=result.getCount();
            if(result.isEmpty())continue;
            for(var source:available)if(ItemStack.isSameItemSameComponents(source,result)){
                int taken=Math.min(needed,source.getCount());source.shrink(taken);needed-=taken;if(needed==0)break;
            }
            if(needed!=0)return CHANGED;
        }
        if(available.stream().anyMatch(s->!s.isEmpty()))return CHANGED;
        for(var change:request.changes())menu.getSlot(change.slot()).setByPlayer(change.after().copy());
        menu.broadcastChanges();return DONE;
    }
}
