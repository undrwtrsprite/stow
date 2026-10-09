package dev.stow.client.inventory;

import dev.stow.Stow;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Vanilla tool components and held-item attributes, including enchantment modifiers. */
public final class ToolPicker {
    private ToolPicker(){}
    private static double attribute(Player player,Holder<Attribute> attribute,ItemStack candidate){
        var copy=new AttributeInstance(attribute,ignored->{});
        if(player!=null){var current=player.getAttribute(attribute);if(current!=null)copy.replaceFrom(current);
            player.getInventory().getSelectedItem().forEachModifier(EquipmentSlot.MAINHAND,(key,modifier)->{if(key.equals(attribute))copy.removeModifier(modifier.id());});}
        candidate.forEachModifier(EquipmentSlot.MAINHAND,(key,modifier)->{if(key.equals(attribute))copy.addOrUpdateTransientModifier(modifier);});
        return copy.getValue();
    }
    public static double speed(Player player,ItemStack item,BlockState block){
        double speed=item.getDestroySpeed(block);if(speed>1)speed+=attribute(player,Attributes.MINING_EFFICIENCY,item);
        return speed*attribute(player,Attributes.BLOCK_BREAK_SPEED,item);
    }
    private static Slot slot(AbstractContainerMenu menu,Inventory inventory,int index){return menu.slots.stream().filter(s->s.container==inventory&&s.getContainerSlot()==index).findFirst().orElse(null);}
    public static int best(Inventory inventory,AbstractContainerMenu menu,BlockState block,Player player){
        return best(inventory,menu,block,player,36);
    }
    /** Automatic mining only selects hotbar slots; manual picking can also retrieve backpack tools. */
    public static int bestHotbar(Inventory inventory,AbstractContainerMenu menu,BlockState block,Player player){
        return best(inventory,menu,block,player,9);
    }
    private static int best(Inventory inventory,AbstractContainerMenu menu,BlockState block,Player player,int limit){
        int selected=inventory.getSelectedSlot(),best=selected;ItemStack held=inventory.getSelectedItem();boolean correct=!block.requiresCorrectToolForDrops()||held.isCorrectToolForDrops(block);double fastest=speed(player,held,block);
        if(Stow.config.toolRefill&&Stow.config.toolRefillMode==dev.stow.StowConfig.ToolRefillMode.KEEP_ONE&&held.isDamageableItem()&&ToolRefill.remaining(held)<=ToolRefill.miningCost(held)){fastest=-1;correct=false;}
        for(int index=0;index<limit;index++){
            var item=inventory.getItem(index);if(item.isEmpty()||!item.has(DataComponents.TOOL)&&speed(player,item,block)<=1)continue;
            if(Stow.config.toolRefill&&Stow.config.toolRefillMode==dev.stow.StowConfig.ToolRefillMode.KEEP_ONE&&item.isDamageableItem()&&ToolRefill.remaining(item)<=ToolRefill.miningCost(item))continue;
            Slot source=slot(menu,inventory,index);if(source==null||!source.isActive()||index>=9&&(PinnedSlots.isPinned(source)||!source.mayPickup(inventory.player)))continue;
            boolean drops=!block.requiresCorrectToolForDrops()||item.isCorrectToolForDrops(block);double value=speed(player,item,block);
            if(drops&&!correct||drops==correct&&value>fastest+1e-6){best=index;fastest=value;correct=drops;}
            else if(drops==correct&&Math.abs(value-fastest)<=1e-6&&best!=selected&&index<9&&best>=9)best=index;
        }
        return best;
    }
    public static boolean autoSwitch(Minecraft mc,net.minecraft.core.BlockPos pos){
        if(!Stow.config.autoTool||mc.player==null||mc.gameMode==null||mc.level==null||mc.gui.screen()!=null
                ||mc.gameMode.getPlayerMode()!=net.minecraft.world.level.GameType.SURVIVAL||mc.player.getAbilities().instabuild
                ||mc.options.keyShift.isDown()||mc.player.isUsingItem())return false;
        var menu=mc.player.inventoryMenu;var inventory=mc.player.getInventory();
        if(menu==null||mc.player.containerMenu!=menu||!menu.getCarried().isEmpty()||InventorySorting.busy(menu)||SmartDeposit.busy(menu))return false;
        var block=mc.level.getBlockState(pos);if(block.isAir()||block.getDestroySpeed(mc.level,pos)<0)return false;
        int best=bestHotbar(inventory,menu,block,mc.player);
        if(best==inventory.getSelectedSlot())return false;
        inventory.setSelectedSlot(best);HandRefill.cancel();return true;
    }
    public static int destination(Inventory inventory,AbstractContainerMenu menu){
        int selected=inventory.getSelectedSlot();Slot current=slot(menu,inventory,selected);if(current!=null&&!PinnedSlots.isPinned(current))return selected;
        for(int i=0;i<9;i++){Slot target=slot(menu,inventory,i);if(target!=null&&!PinnedSlots.isPinned(target)&&!target.hasItem())return i;}
        for(int i=0;i<9;i++){Slot target=slot(menu,inventory,i);if(target!=null&&!PinnedSlots.isPinned(target))return i;}return -1;
    }
    public static boolean pick(Minecraft mc){
        if(!Stow.config.toolPick||mc.player==null||mc.gameMode==null||mc.level==null||mc.gui.screen()!=null||mc.player.getAbilities().instabuild||mc.player.isSpectator()||!(mc.hitResult instanceof BlockHitResult hit))return false;
        var menu=mc.player.inventoryMenu;var inventory=mc.player.getInventory();if(menu==null||mc.player.containerMenu!=menu||!menu.getCarried().isEmpty()||InventorySorting.busy(menu)||SmartDeposit.busy(menu))return false;
        var block=mc.level.getBlockState(hit.getBlockPos());if(block.isAir()||block.getDestroySpeed(mc.level,hit.getBlockPos())<0)return false;
        int best=best(inventory,menu,block,mc.player);if(best==inventory.getSelectedSlot())return inventory.getSelectedItem().has(DataComponents.TOOL)&&inventory.getSelectedItem().getDestroySpeed(block)>1;
        if(best<9)inventory.setSelectedSlot(best);
        else{
            int destination=destination(inventory,menu);if(destination<0)return false;
            Slot source=slot(menu,inventory,best),target=slot(menu,inventory,destination);ItemStack tool=source.getItem().copy();
            if(!target.mayPlace(tool)||!source.mayPlace(target.getItem()))return false;
            mc.gameMode.handleContainerInput(menu.containerId,source.index,destination,ContainerInput.SWAP,mc.player);
            if(!ItemStack.matches(inventory.getItem(destination),tool))return false;inventory.setSelectedSlot(destination);
        }
        HandRefill.cancel();return true;
    }
}
