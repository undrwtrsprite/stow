package dev.stow.client.ui;

import dev.stow.Stow;
import dev.stow.StowConfig.DockCorner;
import dev.stow.client.hud.SurvivalDock;
import dev.stow.client.memory.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Drag a representative dock; edit a draft until Save, so Cancel never changes live placement. */
public final class DockLayoutScreen extends MemoryScreenBase {
    private DockCorner corner=Stow.config.dockCorner;private int offsetX=Stow.config.dockOffsetX,offsetY=Stow.config.dockOffsetY,scale=Stow.config.dockScale;
    private boolean dragging;private double grabX,grabY;
    public DockLayoutScreen(Screen parent){super(Component.translatable("stow.dock.layout"),parent,null);}
    @Override protected int sidebarWidth(){return 0;}
    @Override protected void init(){
        layoutDialog(height);int left=contentLeft(),w=contentWidth();
        addRenderableWidget(new PlannerButton(Component.translatable("stow.dock.corner."+corner.name().toLowerCase(java.util.Locale.ROOT)),b->{corner=DockCorner.values()[(corner.ordinal()+1)%DockCorner.values().length];b.setMessage(Component.translatable("stow.dock.corner."+corner.name().toLowerCase(java.util.Locale.ROOT)));},left+8,44,w-112,22,false));
        addRenderableWidget(new PlannerButton(Component.literal("−"),b->scale=Math.max(60,scale-10),left+w-98,44,26,22,false));
        addRenderableWidget(new PlannerButton(Component.literal("+"),b->scale=Math.min(150,scale+10),left+w-38,44,26,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b->onClose(),left+8,height-30,66,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("controls.reset"),b->{corner=DockCorner.BOTTOM_RIGHT;offsetX=8;offsetY=62;scale=100;rebuildWidgets();},left+w-150,height-30,68,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("stow.common.save"),b->{Stow.config.dockCorner=corner;Stow.config.dockOffsetX=offsetX;Stow.config.dockOffsetY=offsetY;Stow.config.dockScale=scale;Stow.config.save();onClose();},left+w-76,height-30,68,22,true));
    }
    public SurvivalDock.Bounds previewBounds(){return SurvivalDock.bounds(width,height,140,72,corner,offsetX,offsetY,scale);}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){var box=previewBounds();if(e.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&e.x()>=box.x()&&e.x()<box.x()+box.width()&&e.y()>=box.y()&&e.y()<box.y()+box.height()){dragging=true;grabX=e.x()-box.x();grabY=e.y()-box.y();return true;}return super.mouseClicked(e,twice);}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(!dragging)return super.mouseDragged(e,dx,dy);var box=previewBounds();int x=Math.clamp((int)(e.x()-grabX),4,width-box.width()-4),y=Math.clamp((int)(e.y()-grabY),78,height-box.height()-36);
        boolean right=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.TOP_RIGHT,bottom=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.BOTTOM_LEFT;
        offsetX=right?width-x-box.width():x;offsetY=bottom?height-y-box.height():y;return true;}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging){dragging=false;return true;}return super.mouseReleased(e);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        extractMenuBackdrop(g,delta);int left=contentLeft(),w=contentWidth();g.text(font,scale+"%",left+w-70,51,0xFFFFFFFF);
        g.text(font,Component.translatable("stow.dock.drag"),left+8,72,0xFFAAAAAA);
        var box=previewBounds();g.pose().pushMatrix();g.pose().translate(box.x(),box.y());g.pose().scale(box.scale(),box.scale());
        g.text(font,"Main build",0,0,0xFFBDC9CC);g.item(MemoryItems.icon("minecraft:iron_pickaxe"),0,14);g.text(font,"12% · 30",22,18,0xFFFFD486);
        g.item(MemoryItems.icon("minecraft:cobblestone"),0,34);g.text(font,"524 / 1000",22,38,0xFFFFFFFF);g.item(MemoryItems.icon("minecraft:glass"),0,54);g.text(font,"64 / 64",22,58,0xFF8DE8B2);g.pose().popMatrix();
        if(dragging||x>=box.x()&&x<box.x()+box.width()&&y>=box.y()&&y<box.y()+box.height())g.outline(box.x()-3,box.y()-3,box.width()+6,box.height()+6,0xFF88D6C1);
        super.extractRenderState(g,x,y,delta);
    }
}
