package dev.stow.client.memory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import dev.stow.Stow;

/** Small native item icons; the full label remains available to tooltips and narration. */
public final class InventoryIconButton extends Button {
    public enum Kind { DRAG,CHESTS,MATERIALS,SORT,PALETTE,DEPOSIT }
    private final Kind kind;
    public InventoryIconButton(Kind kind,Component label,OnPress action,int x,int y){super(x,y,20,20,label,action,DEFAULT_NARRATION);this.kind=kind;}
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
        int x=getX(),y=getY(),color=active?0xFFE7ECEF:0xFF738088;
        g.fill(x,y,x+20,y+20,isHoveredOrFocused()?0xEE3B4A52:0xCC202A30);
        g.outline(x,y,20,20,isHoveredOrFocused()?0xFF88D6C1:0xFF53616A);
        if(kind==Kind.SORT){
            for(int i=0;i<3;i++)g.fill(x+4,y+5+i*4,x+12-i*2,y+7+i*4,color);
            g.fill(x+15,y+4,x+17,y+13,color);g.fill(x+13,y+11,x+19,y+13,color);g.fill(x+14,y+13,x+18,y+14,color);g.fill(x+15,y+14,x+17,y+15,color);
        }else if(kind==Kind.PALETTE){
            g.fill(x+4,y+5,x+6,y+7,color);g.fill(x+6,y+7,x+8,y+9,color);g.fill(x+4,y+9,x+6,y+11,color);
            g.fill(x+10,y+12,x+16,y+14,color);
        }else if(kind!=Kind.DRAG)g.item(MemoryItems.icon(kind==Kind.CHESTS?"minecraft:chest":kind==Kind.DEPOSIT?"minecraft:hopper":"minecraft:book"),x+2,y+2);
        else {
            // Geometry identifies the mode even in monochrome: equals, grid, or both.
            switch(Stow.config.dragMode) {
                case MATCHING_ONLY -> {g.fill(x+3,y+6,x+9,y+8,color);g.fill(x+3,y+11,x+9,y+13,color);}
                case ALL_ITEMS -> grid(g,x+3,y+5,3,color);
                case BOTH -> {
                    g.fill(x+3,y+3,x+9,y+5,color);g.fill(x+3,y+7,x+9,y+9,color);
                    grid(g,x+3,y+11,2,color);
                }
            }
            g.fill(x+11,y+9,x+17,y+11,color);
            g.fill(x+14,y+6,x+15,y+14,color);g.fill(x+15,y+7,x+16,y+13,color);g.fill(x+16,y+8,x+17,y+12,color);g.fill(x+17,y+9,x+18,y+11,color);
        }
    }
    private static void grid(GuiGraphicsExtractor g,int x,int y,int size,int color){
        for(int row=0;row<2;row++)for(int column=0;column<2;column++)g.fill(x+column*(size+1),y+row*(size+1),x+column*(size+1)+size,y+row*(size+1)+size,color);
    }
}
