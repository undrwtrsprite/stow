package dev.stow.companion.server;

import dev.stow.companion.protocol.MerchantTradeProtocol;
import dev.stow.companion.protocol.MerchantTradeProtocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;

/** Executes the same merchant selection and quick-move operations entirely on the server. */
public final class MerchantTradeServer implements ModInitializer {
    private final Map<UUID,Integer> lastRequest=new HashMap<>();
    @Override public void onInitialize(){
        MerchantTradeProtocol.register();if(!enabled())return;
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(r,c)->c.server().execute(()->{
            var p=c.player();int batches=0;
            int last=lastRequest.getOrDefault(p.getUUID(),p.tickCount-2);
            if(p.tickCount-last>=2){lastRequest.put(p.getUUID(),p.tickCount);batches=apply(p,r);}
            p.containerMenu.sendAllDataToRemote();
            if(ServerPlayNetworking.canSend(p,Result.TYPE))ServerPlayNetworking.send(p,new Result(r.id(),batches));
        }));
        ServerPlayConnectionEvents.DISCONNECT.register((h,s)->lastRequest.remove(h.player.getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(s->lastRequest.clear());
    }
    private static boolean enabled(){
        var file=FabricLoader.getInstance().getConfigDir().resolve("stow-companion.properties");var props=new Properties();
        try{
            if(Files.exists(file))try(var in=Files.newInputStream(file)){props.load(in);}
            if(!props.containsKey("instantVillagerTrading")){
                props.setProperty("instantVillagerTrading","true");
                try(var out=Files.newOutputStream(file)){props.store(out,"stow companion server settings");}
            }
            return !"false".equalsIgnoreCase(props.getProperty("instantVillagerTrading","true").trim());
        }catch(java.io.IOException e){System.err.println("[stow companion] Cannot read trading configuration: "+e.getMessage());return true;}
    }
    public static int apply(ServerPlayer p,Request r){
        if(!p.isAlive()||p.isSpectator()||p.containerMenu.getClass()!=MerchantMenu.class)return 0;
        var menu=(MerchantMenu)p.containerMenu;
        if(menu.containerId!=r.menuId()||menu.getStateId()!=r.stateId()||!menu.stillValid(p)
                ||!menu.getCarried().isEmpty()||r.offer()<0||r.offer()>=menu.getOffers().size())return 0;
        var offer=menu.getOffers().get(r.offer());int batches=0;
        while(batches<MerchantTradeProtocol.MAX_BATCHES&&!offer.isOutOfStock()&&menu.stillValid(p)
                &&p.containerMenu==menu&&menu.getCarried().isEmpty()){
            menu.setSelectionHint(r.offer());menu.tryMoveItems(r.offer());
            if(menu.getSlot(2).getItem().isEmpty()||!menu.getSlot(2).mayPickup(p))break;
            int uses=offer.getUses();
            menu.clicked(2,0,ContainerInput.QUICK_MOVE,p);
            if(offer.getUses()<=uses)break;
            batches++;
        }
        menu.broadcastChanges();return batches;
    }
}
