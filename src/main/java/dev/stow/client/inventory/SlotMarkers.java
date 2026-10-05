package dev.stow.client.inventory;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class SlotMarkers {
    private SlotMarkers(){}
    public static void star(GuiGraphicsExtractor g,int x,int y,int color){
        g.fill(x+3,y,x+4,y+2,color);g.fill(x,y+2,x+7,y+3,color);g.fill(x+1,y+3,x+6,y+4,color);g.fill(x+2,y+4,x+5,y+5,color);
        g.fill(x+1,y+5,x+3,y+6,color);g.fill(x+4,y+5,x+6,y+6,color);g.fill(x+1,y+6,x+2,y+7,color);g.fill(x+5,y+6,x+6,y+7,color);
    }
    /** A shaped padlock with a dark silhouette and a keyhole, readable over any item. */
    public static void lock(GuiGraphicsExtractor g,int x,int y){
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(.45f,.45f);x=0;y=0;
        int dark=0xFF20262A,gold=0xFFFFD45A;
        g.fill(x+2,y,x+7,y+1,dark);g.fill(x+1,y+1,x+3,y+5,dark);g.fill(x+6,y+1,x+8,y+5,dark);
        g.fill(x+3,y+1,x+6,y+2,gold);g.fill(x+2,y+2,x+3,y+4,gold);g.fill(x+6,y+2,x+7,y+4,gold);
        g.fill(x,y+4,x+9,y+10,dark);g.fill(x+1,y+5,x+8,y+9,gold);g.fill(x+4,y+6,x+5,y+8,dark);g.pose().popMatrix();
    }
}
