package dev.stow.client.inventory;

import dev.stow.Stow;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** A confirmed block placement schedules one cursor-safe top-up after the use packet. */
public final class HandRefill {
    private record Request(Player player,int selected,ItemStack item,int remaining) {}
    private static Request pending;
    private HandRefill(){}
    public static void cancel(){pending=null;}
    public static ItemStack stockItem(Player player){
        return player.getInventory().getSelectedItem();
    }
    public static void placed(Player player,int selected,ItemStack before){
        if(before.isEmpty()||!(before.getItem() instanceof BlockItem)||player.getAbilities().instabuild)return;
        var inventory=player.getInventory();if(inventory.getSelectedSlot()!=selected)return;
        ItemStack now=inventory.getSelectedItem();
        if(!now.isEmpty()&&!ItemStack.isSameItemSameComponents(before,now))return;
        int remaining=now.getCount();if(remaining>=before.getCount())return;
        if(Stow.config.handRefill&&remaining<=Stow.config.refillThreshold)pending=new Request(player,selected,before.copyWithCount(1),remaining);
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
        Request request=pending;pending=null;if(request==null||!Stow.config.handRefill||mc.player!=request.player()||mc.gameMode==null||mc.gui.screen()!=null||mc.player.getAbilities().instabuild)return;
        var menu=mc.player.inventoryMenu;var inventory=mc.player.getInventory();var now=inventory.getSelectedItem();
        if(menu==null||mc.player.containerMenu!=menu||!menu.getCarried().isEmpty()||InventorySorting.busy(menu)||SmartDeposit.busy(menu)||inventory.getSelectedSlot()!=request.selected()||now.getCount()!=request.remaining()||!now.isEmpty()&&!ItemStack.isSameItemSameComponents(now,request.item()))return;
        topUp(menu,inventory,request.selected(),request.item(),(slot,button)->mc.gameMode.handleContainerInput(menu.containerId,slot,button,ContainerInput.PICKUP,mc.player));
    }
}
