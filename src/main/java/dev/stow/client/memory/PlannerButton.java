package dev.stow.client.memory;

import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Quiet, compact controls for the planner, using Minecraft's normal font and item models. */
public final class PlannerButton extends Button {
    private final boolean primary;
    private boolean navigation;
    private String itemId;
    private BooleanSupplier selected=() -> false;
    public PlannerButton(Component label,OnPress action,int x,int y,int width,int height,boolean primary) {
        super(x,y,width,height,label,action,DEFAULT_NARRATION);this.primary=primary;
    }
    public static Factory plan(Component label,OnPress action){return new Factory(label,action);}
    public static final class Factory {
        private final Component label;private final OnPress action;private int x,y,w,h;
        Factory(Component label,OnPress action){this.label=label;this.action=action;}
        public Factory pos(int x,int y){this.x=x;this.y=y;return this;}
        public Factory size(int w,int h){this.w=w;this.h=h;return this;}
        public PlannerButton build(){return new PlannerButton(label,action,x,y,w,h,false);}
    }
    public PlannerButton navigation(){navigation=true;return this;}
    public PlannerButton item(String id){itemId=id;return this;}
    public PlannerButton selected(BooleanSupplier selected){this.selected=selected;return this;}
    @Override protected void extractContents(GuiGraphicsExtractor graphics,int mx,int my,float delta) {
        boolean chosen=selected.getAsBoolean(),hover=isHoveredOrFocused();var font=Minecraft.getInstance().font;
        if(navigation){graphics.text(font,font.plainSubstrByWidth(getMessage().getString(),width-8),getX()+4,getY()+(height-9)/2,chosen?0xFFFFFF55:hover?0xFFFFFFFF:0xFFCCCCCC);return;}
        setOverrideRenderHighlightedSprite(() -> selected.getAsBoolean() || isHoveredOrFocused());
        extractDefaultSprite(graphics);
        int textColor=!active?0xFFA0A0A0:chosen?0xFFFFFF55:0xFFFFFFFF;
        if(itemId!=null) {
            graphics.item(MemoryItems.icon(itemId),getX()+6,getY()+(height-16)/2);
            graphics.text(font,font.plainSubstrByWidth(getMessage().getString(),width-36),getX()+28,getY()+(height-9)/2,textColor);
        } else graphics.centeredText(font,font.plainSubstrByWidth(getMessage().getString(),width-8),getX()+width/2,getY()+(height-9)/2,textColor);
    }
}
