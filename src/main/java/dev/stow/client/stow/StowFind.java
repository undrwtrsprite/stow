package dev.stow.client.stow;

import dev.stow.client.inventory.SmartDeposit;
import dev.stow.client.memory.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

/** Hover-find (unbound by default): glow the nearest known source, or open Find when there is none. */
public final class StowFind {
    private StowFind(){}
    public static void findHovered(Screen parent,ItemStack stack){
        var mc=Minecraft.getInstance();var store=ChestMemory.currentStore();if(stack.isEmpty())return;
        String id=SmartDeposit.itemId(stack);var index=StowIndex.of(store);
        var nearest=index==null?null:index.nearest(id);
        if(nearest!=null){
            var session=StowSession.get(ChestMemory.currentWorldKey());session.recentItem(id);
            ChestMemory.show(store,nearest,id);closeAll();return;
        }
        mc.gui.setScreen(StowScreen.open(parent,stack,StowSession.Segment.FIND));
    }
    /** Close every screen, telling the server when a container menu is open. */
    public static void closeAll(){
        var mc=Minecraft.getInstance();
        if(mc.player!=null&&mc.player.containerMenu!=mc.player.inventoryMenu)mc.player.closeContainer();
        mc.gui.setScreen(null);
    }
}
