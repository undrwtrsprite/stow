package dev.stow.client.inventory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Fill or empty a carried bundle with vanilla clicks, once per crossed slot. */
public final class CarriedBundleDrag {
    private AbstractContainerMenu menu;
    private ItemStack expected=ItemStack.EMPTY;
    private boolean unpack;
    private final Set<Slot> visited=new HashSet<>();

    public boolean begin(AbstractContainerMenu menu,Player player,boolean unpack){
        clear();
        if(menu==null||player==null||!(menu.getCarried().getItem() instanceof BundleItem)
                ||!menu.getCarried().has(DataComponents.BUNDLE_CONTENTS))return false;
        this.menu=menu;this.unpack=unpack;expected=menu.getCarried().copy();return true;
    }

    public boolean visit(Slot slot,Player player,BiConsumer<Integer,Integer> click){
        if(menu==null||player==null||!ItemStack.matches(expected,menu.getCarried())){clear();return false;}
        if(slot==null||slot.index<0||slot.index>=menu.slots.size()||menu.slots.get(slot.index)!=slot
                ||visited.contains(slot)||!slot.isActive()||PinnedSlots.isPinned(slot)
                ||ContainerScreenHelper.scope(slot,false,true)==ContainerScreenHelper.INVALID_SCOPE)return false;
        var contents=menu.getCarried().get(DataComponents.BUNDLE_CONTENTS);
        if(unpack){
            if(!slot.getItem().isEmpty()||contents.isEmpty())return false;
            var selected=contents.getSelectedItem();var item=(selected==null?contents.items().getFirst():selected).create();
            if(!slot.mayPlace(item)||Math.min(slot.getMaxStackSize(item),item.getMaxStackSize())<item.getCount())return false;
        }else{
            if(!slot.hasItem()||!slot.mayPickup(player)||contents.asMutable().tryInsert(slot.getItem().copy())==0)return false;
        }
        var before=slot.getItem().copy();
        click.accept(slot.index,unpack?1:0);
        // The cursor must remain the same bundle; incoming changes stop the next operation.
        if(!(menu.getCarried().getItem() instanceof BundleItem)){clear();return false;}
        expected=menu.getCarried().copy();
        boolean moved=!ItemStack.matches(before,slot.getItem());if(moved)visited.add(slot);return moved;
    }

    public void clear(){menu=null;expected=ItemStack.EMPTY;visited.clear();}
}
