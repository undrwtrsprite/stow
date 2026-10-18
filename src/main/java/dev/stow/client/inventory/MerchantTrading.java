package dev.stow.client.inventory;

import dev.stow.client.mixin.MerchantScreenAccessor;
import dev.stow.companion.protocol.MerchantTradeProtocol;
import dev.stow.companion.protocol.MerchantTradeProtocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/** Repeat the selected offer's vanilla refill and quick-move, paced batches between server updates. */
public final class MerchantTrading {
    // Bounds unusual modded offers that return their own payment or replenish stock.
    private static final int MAX_BATCHES=128;
    private static Session active;
    private static int nextRequest,pendingId=-1,pendingAge;
    private static MerchantScreen pendingScreen;
    private MerchantTrading(){}
    public static void register(){
        MerchantTradeProtocol.register();
        ClientPlayNetworking.registerGlobalReceiver(Result.TYPE,(r,c)->c.client().execute(()->{
            if(r.id()==pendingId){pendingId=-1;pendingScreen=null;}
        }));
    }
    public static void cancel(){active=null;pendingId=-1;pendingScreen=null;}
    public static boolean start(MerchantScreen screen){
        var mc=Minecraft.getInstance();var menu=screen.getMenu();
        int selected=((MerchantScreenAccessor)screen).stow_selectedOffer();
        if(mc.player==null||mc.gameMode==null||mc.player.containerMenu!=menu||!menu.getCarried().isEmpty()
                ||selected<0||selected>=menu.getOffers().size()||!menu.getSlot(2).hasItem())return false;
        if(menu.getClass()==MerchantMenu.class&&ClientPlayNetworking.canSend(Request.TYPE)){
            pendingId=++nextRequest;pendingAge=0;pendingScreen=screen;
            ClientPlayNetworking.send(new Request(pendingId,menu.containerId,menu.getStateId(),selected));
            return true;
        }
        active=new Session(screen,selected);
        if(!active.trade(mc))cancel();
        return true;
    }
    public static void tick(Minecraft mc){
        if(pendingId>=0){
            if(mc.player==null||mc.gui.screen()!=pendingScreen||++pendingAge>=100)cancel();
            return; // Never replay a request whose server outcome is unknown.
        }
        if(active==null)return;
        var session=active;var menu=session.menu;
        if(mc.player==null||mc.gameMode==null||mc.getConnection()==null||mc.player.containerMenu!=menu
                ||mc.gui.screen()!=session.screen||!menu.getCarried().isEmpty()
                ||((MerchantScreenAccessor)session.screen).stow_selectedOffer()!=session.selected
                ||session.selected>=menu.getOffers().size()||menu.getOffers().get(session.selected)!=session.offer
                ||session.offer.isOutOfStock()||session.batches>=MAX_BATCHES
                ||InventorySorting.busy(menu)||SmartDeposit.busy(menu)){
            cancel();return;
        }
        // Give ordinary server slot updates time to settle between batches.
        if(session.waitTicks++<3)return;
        session.waitTicks=0;
        if(!session.matches()){cancel();return;}
        // Match clicking this same offer in vanilla, on both sides of the connection.
        // The packet precedes the next output click, so the server refills before trading.
        menu.tryMoveItems(session.selected);
        mc.getConnection().send(new ServerboundSelectTradePacket(session.selected));
        if(!session.trade(mc))cancel();
    }
    private static final class Session {
        final MerchantScreen screen;final MerchantMenu menu;final int selected;final MerchantOffer offer;
        final ItemStack costA,costB,result;
        List<ItemStack> expected;int batches,waitTicks;
        Session(MerchantScreen screen,int selected){
            this.screen=screen;menu=screen.getMenu();this.selected=selected;offer=menu.getOffers().get(selected);
            costA=offer.getCostA().copy();costB=offer.getCostB().copy();result=offer.getResult().copy();
        }
        boolean matches(){
            if(!ItemStack.matches(costA,offer.getCostA())||!ItemStack.matches(costB,offer.getCostB())
                    ||!ItemStack.matches(result,offer.getResult()))return false;
            for(int i=0;i<expected.size();i++)if(!ItemStack.matches(expected.get(i),menu.getSlot(i).getItem()))return false;
            return true;
        }
        boolean trade(Minecraft mc){
            if(offer.isOutOfStock()||!ItemStack.matches(menu.getSlot(2).getItem(),result)||!menu.getSlot(2).mayPickup(mc.player))return false;
            int uses=offer.getUses();
            mc.gameMode.handleContainerInput(menu.containerId,2,0,ContainerInput.QUICK_MOVE,mc.player);
            batches++;
            expected=menu.slots.stream().map(s->s.getItem().copy()).toList();
            // A full inventory or rejected transfer must never trigger another refill.
            return offer.getUses()>uses&&menu.getCarried().isEmpty()&&!offer.isOutOfStock();
        }
    }
}
