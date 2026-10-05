package dev.stow.client.memory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import dev.stow.Stow;
import dev.stow.client.ui.UIIcons;

/** Uniform action glyphs; full labels remain available to tooltips and narration. */
public final class InventoryIconButton extends Button {
    public enum Kind { DRAG,CHESTS,MATERIALS,SORT,PALETTE,DEPOSIT,GLOW,HUD }
    private final Kind kind;
    private boolean selected;
    public void setHudSelected(boolean value){selected=value;}
    public InventoryIconButton(Kind kind,Component label,OnPress action,int x,int y){super(x,y,20,20,label,action,DEFAULT_NARRATION);this.kind=kind;}
    @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
        extractDefaultSprite(g);
        var icon=switch(kind){
            case CHESTS->UIIcons.Kind.STORAGE;case MATERIALS->UIIcons.Kind.MATERIALS;
            case SORT->UIIcons.Kind.SORT;case PALETTE->UIIcons.Kind.COMMAND;
            case DEPOSIT->UIIcons.Kind.DEPOSIT;case GLOW->Stow.config.chestGlow?UIIcons.Kind.EYE:UIIcons.Kind.EYE_OFF;
            case HUD->UIIcons.Kind.STAR;
            case DRAG->switch(Stow.config.dragMode){case MATCHING_ONLY->UIIcons.Kind.COPY;case ALL_ITEMS->UIIcons.Kind.GRID;case BOTH->UIIcons.Kind.BOTH;};
        };
        int color=!active?0xFF737A80:kind==Kind.HUD?(selected?0xFFFFD486:0xFF999999):0xFFE7ECEF;
        UIIcons.draw(g,icon,getX()+(getWidth()-UIIcons.SIZE)/2,getY()+(getHeight()-UIIcons.SIZE)/2,color);
    }
}
