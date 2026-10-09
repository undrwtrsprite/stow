package dev.stow.client.inventory;

import dev.stow.stripping.protocol.OakHarvestProtocol;
import dev.stow.stripping.protocol.OakHarvestProtocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** Only the explicit shortcut can request server harvesting. No companion means strip-only fallback. */
public final class CompanionHarvest {
    private static Request pending;
    private static int nextId,age;
    private static boolean cancelling;
    public static void register(){
        OakHarvestProtocol.register();
        ClientPlayNetworking.registerGlobalReceiver(Result.TYPE,(r,c)->c.client().execute(()->{
            if(pending==null||pending.id()!=r.id())return;
            clear();
            dev.stow.client.ui.UiNotifications.show(Component.translatable("stow.harvest."+r.status(),r.stripped(),r.mined()));
        }));
    }
    public static boolean available(){return ClientPlayNetworking.canSend(Request.TYPE);}
    public static boolean busy(){return pending!=null;}
    public static void start(Minecraft mc,BlockPos anchor){
        pending=new Request(++nextId,anchor.immutable(),mc.player.getInventory().getSelectedSlot(),false);
        age=0;cancelling=false;HandRefill.cancel();ClientPlayNetworking.send(pending);
        dev.stow.client.ui.UiNotifications.show(Component.translatable("stow.harvest.started"));
    }
    public static void cancel(){
        if(pending==null||cancelling)return;
        if(available())ClientPlayNetworking.send(new Request(pending.id(),pending.anchor(),pending.slot(),true));
        cancelling=true;
    }
    public static void tick(Minecraft mc,boolean ready){
        if(pending==null)return;
        if(mc.player==null||mc.level==null||!available()){clear();return;}
        if(!ready||mc.player.getInventory().getSelectedSlot()!=pending.slot())cancel();
        // Server jobs expire after 120 seconds. A stalled connection never triggers local harvesting.
        if(++age>2600){clear();dev.stow.client.ui.UiNotifications.show(Component.translatable("stow.harvest.timeout"));}
    }
    public static void clear(){pending=null;age=0;cancelling=false;}
    private CompanionHarvest(){}
}
