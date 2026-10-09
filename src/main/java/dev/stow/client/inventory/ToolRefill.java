package dev.stow.client.inventory;

import dev.stow.Stow;
import dev.stow.StowConfig.ToolRefillMode;
import dev.stow.client.ui.UiNotifications;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Server-confirmed broken tools and cursor-free swaps of a worn tool with a spare. */
public final class ToolRefill {
    private static ItemStack observed=ItemStack.EMPTY,broken=ItemStack.EMPTY;
    private static int observedSlot=-1,brokenSlot=-1,age,observedAge;
    private static long warned;
    private ToolRefill(){}
    public static int remaining(ItemStack stack){return stack.isDamageableItem()?stack.getMaxDamage()-stack.getDamageValue():Integer.MAX_VALUE;}
    public static void clear(){observed=broken=ItemStack.EMPTY;observedSlot=brokenSlot=-1;age=observedAge=0;warned=0;}
    private static boolean ready(Minecraft mc){return mc.player!=null&&mc.level!=null&&mc.player.isAlive()&&mc.gameMode!=null&&mc.gui.screen()==null&&!mc.player.getAbilities().instabuild&&!mc.player.isSpectator()&&mc.player.containerMenu==mc.player.inventoryMenu&&mc.player.inventoryMenu.getCarried().isEmpty()&&!InventorySorting.busy(mc.player.inventoryMenu)&&!SmartDeposit.busy(mc.player.inventoryMenu);}
    public static boolean eligible(ItemStack stack){return stack.isDamageableItem()&&stack.get(DataComponents.EQUIPPABLE)==null;}
    private static boolean sameProfile(ItemStack a,ItemStack b){var first=a.copy();var second=b.copy();first.setDamageValue(0);second.setDamageValue(0);return ItemStack.isSameItemSameComponents(first,second);}
    public static Slot replacement(AbstractContainerMenu menu,Inventory inventory,int selected,ItemStack match){
        return replacement(menu,inventory,selected,match,1);
    }
    private static Slot replacement(AbstractContainerMenu menu,Inventory inventory,int selected,ItemStack match,int minimum){
        return menu.slots.stream().filter(slot->slot.container==inventory&&slot.getContainerSlot()>=0&&slot.getContainerSlot()<36&&slot.getContainerSlot()!=selected&&slot.isActive()&&!PinnedSlots.isPinned(slot)&&slot.hasItem()&&slot.getItem().getItem()==match.getItem()&&remaining(slot.getItem())>minimum&&slot.mayPickup(inventory.player))
            .sorted(Comparator.<Slot>comparingInt(slot->sameProfile(match,slot.getItem())?0:1).thenComparingInt(slot->slot.getContainerSlot()<9?1:0).thenComparing(Comparator.comparingInt((Slot slot)->remaining(slot.getItem())).reversed())).findFirst().orElse(null);
    }
    private static boolean replace(Minecraft mc,ItemStack match,int minimum){
        if(!ready(mc))return false;var inventory=mc.player.getInventory();int selected=inventory.getSelectedSlot();var menu=mc.player.inventoryMenu;
        var source=replacement(menu,inventory,selected,match,minimum);if(source==null)return false;
        var current=inventory.getSelectedItem();if(!current.isEmpty()&&current.getItem()!=match.getItem()||!source.mayPlace(current))return false;
        var target=menu.slots.stream().filter(slot->slot.container==inventory&&slot.getContainerSlot()==selected).findFirst().orElse(null);
        if(target==null||!target.mayPlace(source.getItem()))return false;
        ItemStack expected=source.getItem().copy();
        mc.gameMode.handleContainerInput(menu.containerId,source.index,selected,ContainerInput.SWAP,mc.player);
        if(!ItemStack.matches(expected,inventory.getSelectedItem()))return false;
        HandRefill.cancel();UiNotifications.show("stow.tool-refill.replaced",expected.getHoverName());return true;
    }
    public static int miningCost(ItemStack stack){var tool=stack.get(DataComponents.TOOL);return tool==null?1:Math.max(1,tool.damagePerBlock());}
    public static int attackCost(ItemStack stack){var weapon=stack.get(DataComponents.WEAPON);return weapon==null?1:Math.max(1,weapon.itemDamagePerAttack());}
    public static int useCost(ItemStack stack){
        if(stack.is(net.minecraft.world.item.Items.FISHING_ROD))return 5;
        if(stack.is(net.minecraft.world.item.Items.CARROT_ON_A_STICK))return 7;
        if(stack.is(net.minecraft.world.item.Items.CROSSBOW))return 3;
        return 1;
    }
    public static int useOnCost(ItemStack stack){
        var item=stack.getItem();
        var transformer=stack.get(DataComponents.BLOCK_TRANSFORMER);
        if(transformer!=null)return transformer.value().transforms().stream().mapToInt(net.minecraft.core.component.BlockTransformer.BlockTransformData::itemDamagePerUse).max().orElse(0);
        return item instanceof net.minecraft.world.item.BrushItem||item instanceof net.minecraft.world.item.FlintAndSteelItem||item instanceof net.minecraft.world.item.ShearsItem?1:0;
    }
    /** True means cancel an action that would consume the reserved durability. */
    public static boolean protect(Minecraft mc,int cost){
        if(!Stow.config.toolRefill||Stow.config.toolRefillMode!=ToolRefillMode.KEEP_ONE||mc.player==null||mc.player.getAbilities().instabuild)return false;
        var held=mc.player.getInventory().getSelectedItem();if(!eligible(held)||remaining(held)>Math.max(1,cost))return false;
        if(ready(mc)&&replace(mc,held,Math.max(1,cost)))return false;
        if(System.currentTimeMillis()-warned>5000){warned=System.currentTimeMillis();UiNotifications.show("stow.tool-refill.protected");}return true;
    }
    /** Called only for the local player's main-hand break event, never for an ordinary drop. */
    public static void broke(Minecraft mc){
        if(!Stow.config.toolRefill||Stow.config.toolRefillMode!=ToolRefillMode.AFTER_BREAK||mc.player==null)return;
        int selected=mc.player.getInventory().getSelectedSlot();var held=mc.player.getInventory().getSelectedItem();
        var item=eligible(held)?held:observedSlot==selected?observed:ItemStack.EMPTY;
        if(eligible(item)){broken=item.copyWithCount(1);brokenSlot=selected;age=0;}
    }
    public static void tick(Minecraft mc){
        if(mc.player==null){clear();return;}var inventory=mc.player.getInventory();int selected=inventory.getSelectedSlot();
        if(!broken.isEmpty()){
            if(!Stow.config.toolRefill||Stow.config.toolRefillMode!=ToolRefillMode.AFTER_BREAK||selected!=brokenSlot||++age>40){broken=ItemStack.EMPTY;}
            else if(inventory.getSelectedItem().isEmpty()&&ready(mc)){if(!replace(mc,broken,1))UiNotifications.show("stow.tool-refill.no-spare");broken=ItemStack.EMPTY;}
            else if(!inventory.getSelectedItem().isEmpty()&&inventory.getSelectedItem().getItem()!=broken.getItem())broken=ItemStack.EMPTY;
        }
        if(Stow.config.toolRefill&&Stow.config.toolRefillMode==ToolRefillMode.KEEP_ONE&&ready(mc)&&remaining(inventory.getSelectedItem())<=1)protect(mc,1);
        if(selected!=observedSlot){observed=ItemStack.EMPTY;observedSlot=selected;observedAge=0;}
        var held=inventory.getSelectedItem();
        if(eligible(held)){observed=held.copy();observedAge=0;}
        // The empty-slot update and break event may arrive on different client ticks.
        else if(!held.isEmpty()||++observedAge>40)observed=ItemStack.EMPTY;
    }
}
