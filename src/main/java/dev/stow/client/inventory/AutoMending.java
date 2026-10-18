package dev.stow.client.inventory;

import dev.stow.client.ui.UiNotifications;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

/** Temporarily lends one inventory item to the offhand; repair itself remains server-owned. */
public final class AutoMending {
    private static boolean enabled;
    private static LocalPlayer owner;
    private static Loan loan;
    private AutoMending(){}
    public static boolean active(){return enabled;}
    public static boolean repairable(ItemStack item){
        return item.isDamaged()&&item.getEnchantments().keySet().stream().anyMatch(enchantment->enchantment.is(Enchantments.MENDING));
    }
    public static boolean toggle(Minecraft mc){
        if(enabled){stop(mc);UiNotifications.show("stow.mending.off");return true;}
        if(!ready(mc))return false;
        enabled=true;owner=mc.player;UiNotifications.show("stow.mending.on");return true;
    }
    public static void stop(Minecraft mc){enabled=false;if(ready(mc))restore(mc);}
    public static void clear(){enabled=false;owner=null;loan=null;}
    private static boolean ready(Minecraft mc){
        return mc.player!=null&&mc.level!=null&&mc.gameMode!=null&&mc.player.isAlive()&&!mc.player.isSpectator()
                &&mc.gui.screen()==null&&mc.gui.overlay()==null&&mc.isWindowActive()&&!mc.isPaused()
                &&mc.player.containerMenu==mc.player.inventoryMenu&&mc.player.inventoryMenu.getCarried().isEmpty()
                &&!InventorySorting.busy(mc.player.inventoryMenu)&&!SmartDeposit.busy(mc.player.inventoryMenu);
    }
    private static boolean sameTool(ItemStack expected,ItemStack current){
        if(expected.isEmpty()||current.isEmpty()||expected.getCount()!=current.getCount())return false;
        var a=expected.copy();var b=current.copy();a.setDamageValue(0);b.setDamageValue(0);
        return ItemStack.isSameItemSameComponents(a,b);
    }
    private static Slot source(Minecraft mc,int position){
        var inventory=mc.player.getInventory();
        return mc.player.inventoryMenu.slots.stream().filter(s->s.container==inventory&&s.getContainerSlot()==position).findFirst().orElse(null);
    }
    private static boolean intact(Minecraft mc){
        var source=source(mc,loan.position);
        return source!=null&&loan.position!=mc.player.getInventory().getSelectedSlot()&&ItemStack.matches(source.getItem(),loan.originalOffhand)
                &&sameTool(loan.borrowed,mc.player.getOffhandItem());
    }
    private static void swap(Minecraft mc,Slot source){
        HandRefill.cancel();
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,source.index,Inventory.SLOT_OFFHAND,ContainerInput.SWAP,mc.player);
    }
    private static void restore(Minecraft mc){
        if(loan==null)return;
        if(owner==mc.player&&intact(mc))swap(mc,source(mc,loan.position));
        loan=null;
    }
    public static void tick(Minecraft mc){
        if(!enabled&&loan==null)return;
        if(mc.player==null||mc.level==null||mc.player!=owner){clear();return;}
        if(!ready(mc)){
            // A busy inventory operation can finish first; menus/focus loss end activation.
            if(mc.gui.screen()!=null||!mc.isWindowActive()||!mc.player.isAlive()||mc.player.isSpectator()||mc.isPaused())enabled=false;
            return;
        }
        if(loan!=null){
            if(!intact(mc)){clear();UiNotifications.show("stow.mending.changed");return;}
            if(!enabled||!mc.player.getOffhandItem().isDamaged())restore(mc);
            else return;
        }
        if(!enabled)return;
        if(repairable(mc.player.getOffhandItem()))return; // Repair the player's existing offhand first.
        var inventory=mc.player.getInventory();var offhand=mc.player.getOffhandItem();
        Slot candidate=mc.player.inventoryMenu.slots.stream()
                .filter(s->s.container==inventory&&s.getContainerSlot()>=0&&s.getContainerSlot()<36
                        &&s.getContainerSlot()!=inventory.getSelectedSlot()&&s.isActive()&&!PinnedSlots.isPinned(s)
                        &&s.mayPickup(mc.player)&&s.mayPlace(offhand)&&repairable(s.getItem()))
                .max(Comparator.comparingInt(s->s.getItem().getDamageValue())).orElse(null);
        if(candidate==null){enabled=false;UiNotifications.show("stow.mending.done");return;}
        loan=new Loan(candidate.getContainerSlot(),offhand.copy(),candidate.getItem().copy());
        swap(mc,candidate);
        if(!intact(mc)){clear();UiNotifications.show("stow.mending.changed");}
    }
    private record Loan(int position,ItemStack originalOffhand,ItemStack borrowed){}
}
