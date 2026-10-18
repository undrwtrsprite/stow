package dev.stow.client.ui;

import dev.stow.Stow;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** Read-only snapshot of the box's item component; never opens a menu or queries the server. */
public final class ShulkerPreview implements ClientTooltipComponent {
    public record Contents(List<ItemStack> items) implements TooltipComponent {}
    private static final int WIDTH=166,HEIGHT=58;
    private final List<ItemStack> items;
    public ShulkerPreview(Contents contents){items=contents.items();}
    public static void register(){
        ClientTooltipComponentCallback.EVENT.register(data->data instanceof Contents contents?new ShulkerPreview(contents):null);
    }
    public static Optional<TooltipComponent> contents(ItemStack stack){
        if(!Stow.config.shulkerPreviews||!(stack.getItem() instanceof BlockItem block)||!(block.getBlock() instanceof ShulkerBoxBlock))return Optional.empty();
        var display=stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY,TooltipDisplay.DEFAULT);
        if(display.hideTooltip()||!display.shows(DataComponents.CONTAINER))return Optional.empty();
        var items=new ArrayList<>(stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).itemCopies().limit(27).toList());
        while(items.size()<27)items.add(ItemStack.EMPTY);
        return Optional.of(new Contents(List.copyOf(items)));
    }
    @Override public int getWidth(Font font){return WIDTH;}
    @Override public int getHeight(Font font){return HEIGHT;}
    @Override public void extractImage(Font font,int x,int y,int width,int height,GuiGraphicsExtractor graphics){
        int left=x+Math.max(0,(width-WIDTH)/2),top=y+2;
        graphics.fill(left,top,left+WIDTH,top+54,0xFF64566F);
        for(int i=0;i<27;i++){
            int sx=left+2+i%9*18,sy=top+i/9*18;
            graphics.fill(sx,sy,sx+18,sy+18,0xFF211D28);
            graphics.outline(sx,sy,18,18,0xFF51465D);
            var item=items.get(i);
            if(!item.isEmpty()){
                graphics.item(item,sx+1,sy+1);
                graphics.itemDecorations(font,item,sx+1,sy+1);
            }
        }
    }
}
