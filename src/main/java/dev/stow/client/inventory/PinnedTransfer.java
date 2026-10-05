package dev.stow.client.inventory;

import dev.stow.Stow;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Route quick transfers through vanilla pickup transactions so protected destinations are never sent to the server. */
public final class PinnedTransfer {
    private PinnedTransfer(){}
    /** Crafting keeps vanilla bulk crafting and its destination selection. */
    public static boolean isCraftingResult(AbstractContainerMenu menu,Slot source){
        return source!=null&&(source instanceof ResultSlot||menu instanceof AbstractCraftingMenu crafting&&source==crafting.getResultSlot());
    }
    public static List<Slot> destinations(AbstractContainerMenu menu,Slot source,Inventory inventory){
        if(source.container==inventory){
            if(menu instanceof ChestMenu||menu instanceof ShulkerBoxMenu)return List.of();
            if(!(menu instanceof InventoryMenu)&&!(menu instanceof CraftingMenu)){
                var item=source.getItem();
                if(menu.slots.stream().anyMatch(s->s.container!=inventory&&s.isActive()&&s.mayPlace(item)&&(s.getItem().isEmpty()||ItemStack.isSameItemSameComponents(item,s.getItem())&&s.getItem().getCount()<Math.min(item.getMaxStackSize(),s.getMaxStackSize(item)))))return List.of();
            }
        }
        boolean sourceMain=source.container==inventory&&source.getContainerSlot()>=9&&source.getContainerSlot()<36;
        boolean sourceHotbar=source.container==inventory&&source.getContainerSlot()>=0&&source.getContainerSlot()<9;
        return menu.slots.stream().filter(s->s!=source&&s.container==inventory&&s.getContainerSlot()>=0&&s.getContainerSlot()<36)
            .filter(s->source.container!=inventory||!sourceMain&&!sourceHotbar||sourceMain&&s.getContainerSlot()<9||sourceHotbar&&s.getContainerSlot()>=9)
            .sorted(source.container==inventory?Comparator.comparingInt(s->s.index):Comparator.comparingInt((Slot s)->s.index).reversed()).toList();
    }
    public static boolean move(AbstractContainerMenu menu,Slot source,Player player,BiConsumer<Integer,Integer> click){
        if(!Stow.config.pinProtectTransfers||source==null||player==null||isCraftingResult(menu,source))return false;
        var destinations=destinations(menu,source,player.getInventory());if(destinations.stream().noneMatch(PinnedSlots::isPinned))return false;
        if(!menu.getCarried().isEmpty()||!source.hasItem()||!source.isActive()||!source.mayPickup(player))return true;
        // Non-stackable equipment may normally auto-equip; keep that behavior if its equipment slot is available.
        if(menu instanceof InventoryMenu&&source.container==player.getInventory()&&source.getContainerSlot()<36){
            var item=source.getItem();var equippable=item.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
            if(equippable!=null&&menu.slots.stream().anyMatch(s->s.container==player.getInventory()&&Inventory.EQUIPMENT_SLOT_MAPPING.get(s.getContainerSlot())==equippable.slot()&&!s.hasItem()&&s.mayPlace(item)))return false;
        }
        transfer(menu,source,player,click);
        return true;
    }
    /** One complete pickup cycle. Never leave a partial crafting output on the cursor. */
    private static boolean transfer(AbstractContainerMenu menu,Slot source,Player player,BiConsumer<Integer,Integer> click){
        var destinations=destinations(menu,source,player.getInventory());
        var item=source.getItem().copy();var targets=destinations.stream().filter(s->!PinnedSlots.isPinned(s)&&s.isActive()&&s.mayPlace(item)).toList();
        boolean hasRoom=targets.stream().anyMatch(s->s.getItem().isEmpty()||ItemStack.isSameItemSameComponents(s.getItem(),item)&&s.getItem().getCount()<Math.min(item.getMaxStackSize(),s.getMaxStackSize(item)));
        if(!hasRoom)return false;
        if(!source.mayPlace(item)){
            long capacity=targets.stream().filter(s->s.getItem().isEmpty()||ItemStack.isSameItemSameComponents(s.getItem(),item)).mapToLong(s->Math.max(0,Math.min(item.getMaxStackSize(),s.getMaxStackSize(item))-s.getItem().getCount())).sum();
            if(capacity<item.getCount())return false; // An output slot cannot safely receive an unfinished cursor stack.
        }
        click.accept(source.index,0);
        for(boolean empty:new boolean[]{false,true})for(var target:targets){
            if(menu.getCarried().isEmpty())return true;
            if(target.getItem().isEmpty()!=empty||!empty&&!ItemStack.isSameItemSameComponents(target.getItem(),menu.getCarried()))continue;
            if(target.getItem().getCount()>=Math.min(menu.getCarried().getMaxStackSize(),target.getMaxStackSize(menu.getCarried())))continue;
            click.accept(target.index,0);
        }
        if(!menu.getCarried().isEmpty())click.accept(source.index,0);
        return menu.getCarried().isEmpty();
    }
}
