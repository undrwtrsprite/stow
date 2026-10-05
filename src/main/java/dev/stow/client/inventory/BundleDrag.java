package dev.stow.client.inventory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

/** Unpack one bundle stack per crossed slot using ordinary vanilla pickup clicks. */
public final class BundleDrag {
    private AbstractContainerMenu menu;
    private Slot source;
    private ItemStack expected=ItemStack.EMPTY;
    private final Set<Slot> visited=new HashSet<>();
    public boolean begin(AbstractContainerMenu menu,Slot source,Player player){
        clear();
        if(menu==null||source==null||player==null||!menu.getCarried().isEmpty()||!member(menu,source)
                ||PinnedSlots.isPinned(source)||!source.isActive()||!source.mayPickup(player)||!(source.getItem().getItem() instanceof BundleItem))return false;
        var contents=source.getItem().get(DataComponents.BUNDLE_CONTENTS);
        if(contents==null||contents.isEmpty())return false;
        this.menu=menu;this.source=source;expected=source.getItem().copy();return true;
    }
    private static boolean member(AbstractContainerMenu menu,Slot slot){return slot.index>=0&&slot.index<menu.slots.size()&&menu.slots.get(slot.index)==slot;}
    public Slot source(){return source;}
    public boolean valid(Player player){
        return source!=null&&player!=null&&menu.getCarried().isEmpty()&&source.isActive()&&!PinnedSlots.isPinned(source)
                &&source.mayPickup(player)&&ItemStack.matches(expected,source.getItem());
    }
    public boolean visit(Slot target,Player player,BiConsumer<Integer,Integer> click){
        if(!valid(player)){clear();return false;}
        if(target==null||target==source||!member(menu,target)||visited.contains(target)||!target.isActive()
                ||PinnedSlots.isPinned(target)||ContainerScreenHelper.scope(target,false,true)==ContainerScreenHelper.INVALID_SCOPE)return false;
        var contents=source.getItem().get(DataComponents.BUNDLE_CONTENTS);
        if(contents==null||contents.isEmpty())return false;
        var selected=contents.getSelectedItem();var item=(selected==null?contents.items().getFirst():selected).create();
        var present=target.getItem();
        if(!target.mayPlace(item)||!present.isEmpty()&&!ItemStack.isSameItemSameComponents(present,item)
                ||Math.min(item.getMaxStackSize(),target.getMaxStackSize(item))-present.getCount()<item.getCount())return false;
        var before=present.copy();
        click.accept(source.index,1); // Vanilla takes the selected bundle stack onto the cursor.
        if(ItemStack.matches(item,menu.getCarried()))click.accept(target.index,0);
        // A rejected destination never drops items or leaves a partial unpacking operation.
        if(!menu.getCarried().isEmpty())click.accept(source.index,0);
        if(!menu.getCarried().isEmpty()){clear();return false;}
        expected=source.getItem().copy();
        boolean moved=!ItemStack.matches(before,target.getItem());if(moved)visited.add(target);return moved;
    }
    public void clear(){menu=null;source=null;expected=ItemStack.EMPTY;visited.clear();}
}
