package dev.stow.client.memory;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Explicit lines, wrapped to the scaled viewport; never render literal newline glyphs. */
public final class PlannerTooltips {
    public static net.minecraft.client.gui.components.Tooltip undo(ChestMemoryStore store){
        if(store==null || !store.canUndo())return null;
        String label=store.undoLabel();
        if(label.startsWith("Remove "))label=MemoryItems.name(label.substring(7));else if(label.startsWith("Forget "))label=label.substring(7);else if(label.startsWith("Delete "))label=label.substring(7);
        return net.minecraft.client.gui.components.Tooltip.create(Component.translatable("stow.need.undo-restore",label));
    }
    public static void show(GuiGraphicsExtractor graphics,int mx,int my,Component... lines) {
        var mc=Minecraft.getInstance();List<FormattedCharSequence> wrapped=new ArrayList<>();
        int width=Math.min(260,Math.max(80,mc.getWindow().getGuiScaledWidth()-32));
        for(Component line:lines)wrapped.addAll(mc.font.split(line,width));
        graphics.setTooltipForNextFrame(mc.font,wrapped,mx,my);
    }
}
