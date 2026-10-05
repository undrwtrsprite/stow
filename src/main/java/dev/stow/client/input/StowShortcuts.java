package dev.stow.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stow.Stow;
import dev.stow.StowConfig.Shortcut;
import dev.stow.client.hud.SurvivalDock;
import dev.stow.client.inventory.*;
import dev.stow.client.memory.*;
import dev.stow.client.ui.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class StowShortcuts {
    private static final int MODIFIERS=InputConstants.MOD_SHIFT|InputConstants.MOD_CONTROL|InputConstants.MOD_ALT|InputConstants.MOD_SUPER;
    public enum Action {
        PALETTE,PIN,TRACK,SEARCH,SORT,DEPOSIT,KEEP,EQUIPMENT,PROJECTS,CYCLE_DRAG,DOCK;
        public Component title(){return Component.translatable("stow.action."+name().toLowerCase(Locale.ROOT));}
        public Component group(){return Component.translatable("stow.shortcuts."+switch(this){case PIN,TRACK,SEARCH,SORT,CYCLE_DRAG->"inventory";case DEPOSIT,KEEP->"storage";default->"workspace";});}
        public Shortcut defaults(){return switch(this){
            case PALETTE->new Shortcut(false,InputConstants.KEY_K,InputConstants.MOD_CONTROL);
            case PIN->new Shortcut(false,InputConstants.KEY_P,0);
            case TRACK->new Shortcut(false,InputConstants.KEY_N,0);
            case SEARCH->new Shortcut(false,InputConstants.KEY_F,InputConstants.MOD_CONTROL);
            case SORT->new Shortcut(true,InputConstants.MOUSE_BUTTON_MIDDLE,0);
            default->new Shortcut(false,-1,0);
        };}
    }
    private StowShortcuts(){}
    public static Shortcut binding(Action action){var saved=Stow.config.shortcuts.get(action.name());return saved==null?action.defaults():saved;}
    public static Shortcut chord(KeyEvent event){return new Shortcut(false,event.key(),event.modifiers()&MODIFIERS);}
    public static Shortcut chord(MouseButtonInfo event){return new Shortcut(true,event.button(),event.modifiers()&MODIFIERS);}
    public static boolean matches(Action action,Shortcut event){
        var bound=binding(action);if(bound.code()<0||bound.mouse()!=event.mouse()||bound.code()!=event.code())return false;
        int modifiers=event.modifiers()&MODIFIERS;
        if(action==Action.SORT){
            if((modifiers&bound.modifiers())!=bound.modifiers()||(modifiers&~(bound.modifiers()|InputConstants.MOD_SHIFT|InputConstants.MOD_CONTROL))!=0)return false;
            if(modifiers!=bound.modifiers()&&Arrays.stream(Action.values()).anyMatch(other->other!=action&&binding(other).equals(event)))return false;
        }else if(bound.modifiers()!=modifiers)return false;
        return Arrays.stream(Action.values()).noneMatch(other->other!=action&&binding(other).equals(bound));
    }
    public static String label(Shortcut key){
        if(key==null||key.code()<0)return Component.translatable("stow.shortcuts.unbound").getString();
        String modifiers=((key.modifiers()&InputConstants.MOD_CONTROL)!=0?"Ctrl + ":"")+((key.modifiers()&InputConstants.MOD_ALT)!=0?"Alt + ":"")+((key.modifiers()&InputConstants.MOD_SHIFT)!=0?"Shift + ":"")+((key.modifiers()&InputConstants.MOD_SUPER)!=0?"Super + ":"");
        return modifiers+(key.mouse()?InputConstants.Type.MOUSE:InputConstants.Type.KEYBOARD).getOrCreate(key.code()).getDisplayName().getString();
    }
    public static List<String> conflicts(Action action,Shortcut key,Map<String,Shortcut> draft){
        if(key.code()<0)return List.of();var found=new ArrayList<String>();
        for(var other:Action.values())if(other!=action&&key.equals(draft.getOrDefault(other.name(),other.defaults())))found.add(other.title().getString());
        for(var vanilla:Minecraft.getInstance().options.keyMappings){
            InputConstants.Key assigned=InputConstants.getKey(vanilla.saveString());
            if(assigned.getType()==(key.mouse()?InputConstants.Type.MOUSE:InputConstants.Type.KEYBOARD)&&assigned.getValue()==key.code())found.add(Component.translatable(vanilla.getName()).getString());
        }
        return List.copyOf(found);
    }
    public static boolean dispatch(Action action,Screen parent,Slot hovered){
        var mc=Minecraft.getInstance();var container=parent instanceof AbstractContainerScreen<?> screen?screen:null;
        if(mc.player!=null&&!mc.player.containerMenu.getCarried().isEmpty())return false;
        if(container!=null&&(InventorySorting.busy(container.getMenu())||SmartDeposit.busy(container.getMenu())))return false;
        var store=ChestMemory.currentStore();ItemStack item=hovered!=null&&hovered.hasItem()?hovered.getItem().copy():mc.player==null?ItemStack.EMPTY:mc.player.getInventory().getSelectedItem().copy();
        switch(action){
            case PALETTE->mc.gui.setScreen(new CommandPaletteScreen(parent,item));
            case PIN->{return PinnedSlots.toggle(hovered);}
            case TRACK->{if(item.isEmpty()||store==null)return false;MaterialPlanner.quickAdd(parent,store,item);}
            case DEPOSIT->{return container!=null&&SmartDeposit.start(container);}
            case KEEP->mc.gui.setScreen(new KeepAmountsScreen(parent,item.isEmpty()?"":SmartDeposit.itemId(item)));
            case EQUIPMENT->mc.gui.setScreen(new EquipmentScreen(parent));
            case PROJECTS->{if(store==null)return false;mc.gui.setScreen(new ProjectsScreen(parent,store));}
            case CYCLE_DRAG->Stow.config.cycleDragMode();
            case DOCK->{Stow.config.dockEnabled=!Stow.config.dockEnabled;Stow.config.save();}
            default->{return false;}
        }
        return true;
    }
    public static boolean inventoryKey(Screen parent,Slot hovered,KeyEvent event){
        var chord=chord(event);
        for(Action action:Action.values())if(action!=Action.SEARCH&&matches(action,chord)){
            if(action==Action.SORT&&parent instanceof AbstractContainerScreen<?> screen)return InventorySorting.start(screen,hovered,event.hasShiftDown(),event.hasControlDown());
            return dispatch(action,parent,hovered);
        }
        return false;
    }
    public static boolean inventoryMouse(Screen parent,Slot hovered,MouseButtonEvent event){
        var chord=chord(event.buttonInfo());
        for(Action action:Action.values())if(action!=Action.SEARCH&&matches(action,chord)){
            if(action==Action.SORT&&parent instanceof AbstractContainerScreen<?> screen)return InventorySorting.start(screen,hovered,event.hasShiftDown(),event.hasControlDown());
            return dispatch(action,parent,hovered);
        }
        return false;
    }
    public static boolean global(Shortcut chord){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.gui.screen()!=null)return false;
        for(Action action:List.of(Action.PALETTE,Action.EQUIPMENT,Action.PROJECTS,Action.DOCK))if(matches(action,chord))return dispatch(action,null,null);
        return false;
    }
}
