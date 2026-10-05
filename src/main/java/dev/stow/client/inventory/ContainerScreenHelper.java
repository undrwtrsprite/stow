package dev.stow.client.inventory;

import dev.stow.Stow;
import dev.stow.StowConfig.HotbarScoping;
import dev.stow.client.network.InteractionManager.*;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Mouse Wheelie slot scopes, with stow's pin and equipment exclusions. */
public final class ContainerScreenHelper<T extends AbstractContainerScreen<?>> {
    public static final int INVALID_SCOPE=Integer.MIN_VALUE;
    @FunctionalInterface public interface ClickFactory { InteractionEvent create(Slot slot,int button,ContainerInput input); }
    private final T screen;
    private final ClickFactory factory;
    private ContainerScreenHelper(T screen,ClickFactory factory){this.screen=screen;this.factory=factory;}
    public static <T extends AbstractContainerScreen<?>> ContainerScreenHelper<T> of(T screen,ClickFactory factory){return new ContainerScreenHelper<>(screen,factory);}
    public InteractionEvent createClickEvent(Slot slot,int button,ContainerInput input){return factory.create(slot,button,input);}
    public int getScope(Slot slot,boolean preferSmaller){return scope(slot,screen instanceof InventoryScreen,preferSmaller);}
    public static int scope(Slot slot,boolean playerScreen,boolean preferSmaller){
        if(slot==null||slot.container==null||!slot.isActive()||slot.getContainerSlot()<0||slot.getContainerSlot()>=slot.container.getContainerSize()||!slot.mayPlace(ItemStack.EMPTY))return INVALID_SCOPE;
        if(slot.container instanceof Inventory){
            int position=slot.getContainerSlot();
            if(position>=36)return INVALID_SCOPE;
            if(playerScreen)return position<9?0:1;
            if(position<9&&(Stow.config.hotbarScoping==HotbarScoping.SOFT&&preferSmaller))return -1;
            return 0;
        }
        return playerScreen?3:1;
    }
}
