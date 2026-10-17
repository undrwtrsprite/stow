package dev.stow.client.inventory;

import dev.stow.Stow;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;

/** Confirmed block/rocket consumption schedules a cursor-safe top-up of the hand that used it. */
public final class HandRefill {
    private record Request(Player player,int selected,int target,ItemStack item,int beforeCount,int remaining) {}
    private static Request pending;
    private static int age;
    private HandRefill(){}
    public static void cancel(){pending=null;age=0;}
    public static ItemStack stockItem(Player player){
        return player.getInventory().getSelectedItem();
    }
    public static void placed(Player player,int selected,ItemStack before){
        used(player,InteractionHand.MAIN_HAND,selected,before);
    }
    public static void used(Player player,InteractionHand hand,int selected,ItemStack before){
        if(!Stow.config.handRefill||before.isEmpty()||!(before.getItem() instanceof BlockItem||before.getItem() instanceof FireworkRocketItem)||player.getAbilities().instabuild)return;
        var inventory=player.getInventory();if(hand==InteractionHand.MAIN_HAND&&inventory.getSelectedSlot()!=selected)return;
        int target=hand==InteractionHand.MAIN_HAND?selected:Inventory.SLOT_OFFHAND;
        ItemStack now=inventory.getItem(target);
        if(!now.isEmpty()&&!ItemStack.isSameItemSameComponents(before,now))return;
        int remaining=now.getCount();
        // Vanilla rockets shrink only on the server. Wait for its inventory update before moving reserves.
        if(remaining==before.getCount()&&before.getItem() instanceof FireworkRocketItem)remaining=-1;
        else if(remaining>=before.getCount())return;
        if(remaining>=0&&remaining>Stow.config.refillThreshold||remaining<0&&before.getCount()-1>Stow.config.refillThreshold)return;
        pending=new Request(player,selected,target,before.copyWithCount(1),before.getCount(),remaining);age=0;
    }
    public static List<Slot> sources(AbstractContainerMenu menu,Inventory inventory,int selected,ItemStack match){
        return menu.slots.stream().filter(s->s.container==inventory&&s.getContainerSlot()>=0&&s.getContainerSlot()<36&&s.getContainerSlot()!=selected&&s.isActive()&&!PinnedSlots.isPinned(s)&&s.hasItem()&&ItemStack.isSameItemSameComponents(match,s.getItem()))
            .sorted(Comparator.<Slot>comparingInt(s->s.getContainerSlot()<9?1:0).thenComparing(Comparator.comparingInt((Slot s)->s.getItem().getCount()).reversed())).toList();
    }
    /** Uses the normal inventory menu and finishes each source with an empty cursor. */
    public static int topUp(AbstractContainerMenu menu,Inventory inventory,int selected,ItemStack match,BiConsumer<Integer,Integer> click){
        if(!menu.getCarried().isEmpty())return 0;
        Slot target=menu.slots.stream().filter(s->s.container==inventory&&s.getContainerSlot()==selected).findFirst().orElse(null);
        if(target==null||!target.isActive()||!target.mayPlace(match)||target.hasItem()&&!ItemStack.isSameItemSameComponents(match,target.getItem()))return 0;
        int original=target.getItem().getCount(),limit=Math.min(match.getMaxStackSize(),target.getMaxStackSize(match));
        for(Slot source:sources(menu,inventory,selected,match)){
            if(target.getItem().getCount()>=limit)break;
            if(!source.mayPickup(inventory.player))continue;
            click.accept(source.index,0);if(menu.getCarried().isEmpty())break;
            click.accept(target.index,0);if(!menu.getCarried().isEmpty())click.accept(source.index,0);
            if(!menu.getCarried().isEmpty())break;
        }
        return target.getItem().getCount()-original;
    }
    public static void tick(Minecraft mc){
        Request request=pending;if(request==null)return;
        if(!Stow.config.handRefill||mc.player!=request.player()||mc.gameMode==null||mc.gui.screen()!=null||mc.player.getAbilities().instabuild){cancel();return;}
        var menu=mc.player.inventoryMenu;var inventory=mc.player.getInventory();var now=inventory.getItem(request.target());
        if(menu==null||mc.player.containerMenu!=menu||!menu.getCarried().isEmpty()||InventorySorting.busy(menu)||SmartDeposit.busy(menu)
            ||request.target()!=Inventory.SLOT_OFFHAND&&inventory.getSelectedSlot()!=request.selected()
            ||!now.isEmpty()&&!ItemStack.isSameItemSameComponents(now,request.item())){cancel();return;}
        if(request.remaining()<0&&now.getCount()==request.beforeCount()){
            if(++age>100)cancel();return;
        }
        cancel();
        if(request.remaining()<0?now.getCount()>=request.beforeCount():now.getCount()!=request.remaining())return;
        if(now.getCount()>Stow.config.refillThreshold)return;
        topUp(menu,inventory,request.target(),request.item(),(slot,button)->mc.gameMode.handleContainerInput(menu.containerId,slot,button,ContainerInput.PICKUP,mc.player));
    }
}
