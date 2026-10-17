package dev.stow.client.inventory;

import dev.stow.Stow;
import dev.stow.StowConfig.SortOrder;
import dev.stow.companion.protocol.ChestSortProtocol;
import dev.stow.companion.protocol.ChestSortProtocol.*;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.stow.client.inventory.sort.InventorySorter;
import dev.stow.client.network.InteractionManager;
import dev.stow.client.network.InteractionManager.ClickEvent;
import dev.stow.client.util.CreativeSearchOrder;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Uses companion-validated storage sorting when available, otherwise guarded Mouse Wheelie click cycles. */
public final class InventorySorting {
    private static Session active;
    private static AbstractContainerScreen<?> owner;
    private static int nextRequest;
    private InventorySorting(){}
    public static void register(){
        ChestSortProtocol.register();
        ClientPlayNetworking.registerGlobalReceiver(Result.TYPE,(result,context)->context.client().execute(()->{
            if(active==null||active.requestId!=result.id())return;
            if(result.status()==ChestSortProtocol.FALLBACK){active.requestId=-1;return;}
            dev.stow.client.ui.UiNotifications.show(result.status()==ChestSortProtocol.DONE?"stow.feedback.sort-done":"stow.feedback.sort-stopped");cancel();
        }));
    }
    public static boolean busy(AbstractContainerMenu menu){return active!=null&&active.menu==menu;}
    public static void cancel(){active=null;owner=null;}
    public static SortOrder selectedOrder(boolean shift,boolean control){return shift?Stow.config.shiftSortOrder:control?Stow.config.controlSortOrder:Stow.config.sortOrder;}
    public static boolean start(AbstractContainerScreen<?> screen,Slot hovered){return start(screen,hovered,false,false);}
    public static boolean start(AbstractContainerScreen<?> screen,Slot hovered,boolean shift,boolean control){
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.gameMode==null||hovered==null||SmartDeposit.busy(screen.getMenu())||!screen.getMenu().getCarried().isEmpty()||screen instanceof CreativeModeInventoryScreen)return false;
        var slots=scope(screen,hovered,mc.player);
        if(slots.size()<2)return false;
        SortOrder order=selectedOrder(shift,control);
        if(order==SortOrder.CREATIVE)CreativeSearchOrder.refreshItemSearchPositionLookup();
        cancel();owner=screen;
        active=new Session(screen.getMenu(),slots,order,(id,button)->mc.gameMode.handleContainerInput(screen.getMenu().containerId,id,button,ContainerInput.PICKUP,mc.player));
        active.tryCompanion(mc);
        return true;
    }
    public static void tick(Minecraft mc){
        if(active==null)return;
        if(mc.player==null||mc.player.containerMenu!=active.menu||mc.gui.screen()!=owner){cancel();return;}
        if(active.requestId>=0){if(++active.age>100){dev.stow.client.ui.UiNotifications.show("stow.feedback.sort-stopped");cancel();}return;}
        if(!active.step()){
            boolean completed=!active.failed&&active.position>=active.clicks.size()&&active.menu.getCarried().isEmpty();
            dev.stow.client.ui.UiNotifications.show(completed?"stow.feedback.sort-done":"stow.feedback.sort-stopped");cancel();
        }
    }
    public static List<Slot> scope(AbstractContainerScreen<?> screen,Slot hovered,Player player){return scope(screen.getMenu(),hovered,player,screen instanceof InventoryScreen);}
    public static List<Slot> scope(AbstractContainerMenu menu,Slot hovered,Player player){return scope(menu,hovered,player,false);}
    private static List<Slot> scope(AbstractContainerMenu menu,Slot hovered,Player player,boolean playerScreen){
        int scope=ContainerScreenHelper.scope(hovered,playerScreen,true);
        if(scope==ContainerScreenHelper.INVALID_SCOPE)return List.of();
        var slots=menu.slots.stream().filter(slot->ContainerScreenHelper.scope(slot,playerScreen,true)==scope&&!PinnedSlots.isPinned(slot)&&(player==null||slot.mayPickup(player))).toList();
        // A furnace/output or other restricted slot cannot accept an arbitrary permutation.
        for(Slot slot:slots)for(Slot source:slots)if(source.hasItem()&&(!slot.mayPlace(source.getItem())||slot.getMaxStackSize(source.getItem())<source.getItem().getCount()))return List.of();
        return slots;
    }
    /** Uses the same sorter as live play; callers supply vanilla clicks, including their button. */
    public static final class Session {
        private final AbstractContainerMenu menu;
        private final List<Slot> slots;
        private final BiConsumer<Integer,Integer> click;
        private final List<ClickEvent> clicks;
        private List<ItemStack> sorted=List.of();
        private int requestId=-1,age;
        private List<ItemStack> expected;
        private int position;
        private boolean failed;
        public Session(AbstractContainerMenu menu,List<Slot> slots,SortOrder order,BiConsumer<Integer,Integer> click){
            this.menu=menu;this.slots=List.copyOf(slots);this.click=click;expected=snapshot();
            var helper=ContainerScreenHelper.of(null,(slot,button,input)->new ClickEvent(menu.containerId,slot.index,button,input));
            var sorter=new InventorySorter(helper,null,this.slots);
            clicks=menu.getCarried().isEmpty()?InteractionManager.plan(()->sorter.sort(order.mode())):List.of();sorted=sorter.sortedStacks();
        }
        private void tryCompanion(Minecraft mc){
            if(clicks.isEmpty()||sorted.size()!=slots.size()||slots.size()>ChestSortProtocol.MAX_SLOTS
                ||menu.getClass()!=ChestMenu.class&&menu.getClass()!=ShulkerBoxMenu.class
                ||slots.stream().anyMatch(s->s.container==mc.player.getInventory())||!ClientPlayNetworking.canSend(Request.TYPE))return;
            var changes=new ArrayList<Change>();for(int i=0;i<slots.size();i++)changes.add(new Change(slots.get(i).index,expected.get(i),sorted.get(i)));
            var request=new Request(++nextRequest,menu.containerId,menu.getStateId(),changes);
            var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),mc.level.registryAccess());
            try{
                Request.CODEC.encode(buffer,request);if(buffer.readableBytes()>ChestSortProtocol.MAX_BYTES)return;
            }catch(RuntimeException tooLarge){return;}finally{buffer.release();}
            requestId=request.id();ClientPlayNetworking.send(request);
        }
        private List<ItemStack> snapshot(){return slots.stream().map(s->s.getItem().copy()).toList();}
        public boolean step(){
            if(failed||position>=clicks.size()||!menu.getCarried().isEmpty())return false;
            for(int i=0;i<slots.size();i++)if(!slots.get(i).isActive()||PinnedSlots.isPinned(slots.get(i))||!ItemStack.matches(slots.get(i).getItem(),expected.get(i)))return false;
            int sent=0;
            do {
                ClickEvent event=clicks.get(position++);click.accept(event.slotId(),event.button());sent++;
                if(position==clicks.size())break;
            }while(sent<5||!menu.getCarried().isEmpty());
            failed=!menu.getCarried().isEmpty();expected=snapshot();
            return !failed&&position<clicks.size();
        }
    }
}
