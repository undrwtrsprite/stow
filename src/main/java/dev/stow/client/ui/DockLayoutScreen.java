package dev.stow.client.ui;

import dev.stow.Stow;
import dev.stow.StowConfig.DockCorner;
import dev.stow.StowConfig.HudAlignment;
import dev.stow.client.hud.BuildingStockHud;
import dev.stow.client.hud.SurvivalDock;
import dev.stow.client.memory.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Fullscreen, exact live-coordinate editor. Controls occupy the opposite corner. */
public final class DockLayoutScreen extends MemoryScreenBase {
    public enum Target { EQUIPMENT,PROJECT,STOCK }
    private final boolean project,stock;
    private HudAlignment alignment;
    private boolean followHotbar;
    private DockCorner corner;private int offsetX,offsetY,scale,panelX,panelY,panelWidth;
    private boolean dragging;private double grabX,grabY;
    public DockLayoutScreen(Screen parent){this(parent,Target.EQUIPMENT);}
    public DockLayoutScreen(Screen parent,boolean project){this(parent,project?Target.PROJECT:Target.EQUIPMENT);}
    public DockLayoutScreen(Screen parent,Target target){
        super(Component.translatable(target==Target.PROJECT?"stow.project.layout":target==Target.STOCK?"stow.stock.layout":"stow.dock.layout"),parent,null);project=target==Target.PROJECT;stock=target==Target.STOCK;
        corner=stock?Stow.config.stockCorner:project?Stow.config.projectCorner:Stow.config.dockCorner;
        offsetX=stock?Stow.config.stockOffsetX:project?Stow.config.projectOffsetX:Stow.config.dockOffsetX;offsetY=stock?Stow.config.stockOffsetY:project?Stow.config.projectOffsetY:Stow.config.dockOffsetY;scale=stock?Stow.config.stockScale:project?Stow.config.projectScale:Stow.config.dockScale;
        alignment=stock?Stow.config.stockAlignment:project?Stow.config.projectAlignment:Stow.config.dockAlignment;followHotbar=stock&&Stow.config.stockFollowHotbar;
    }
    @Override protected int sidebarWidth(){return 0;}
    @Override protected void init(){
        panelWidth=Math.min(278,width-16);
        boolean right=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.TOP_RIGHT,bottom=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.BOTTOM_LEFT;
        panelX=right?8:width-panelWidth-8;panelY=bottom?8:Math.max(8,height-158);
        int x=panelX+8,y=panelY+34,w=panelWidth-16;
        addRenderableWidget(new PlannerButton(cornerLabel(),b->{corner=DockCorner.values()[(corner.ordinal()+1)%DockCorner.values().length];followHotbar=false;rebuildWidgets();},x,y,w-100,22,false));
        addRenderableWidget(new PlannerButton(Component.literal("−"),b->scale=Math.max(60,scale-10),x+w-94,y,22,22,false));
        addRenderableWidget(new PlannerButton(Component.literal("+"),b->scale=Math.min(stock?200:150,scale+10),x+w-22,y,22,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("stow.hud.align."+alignment.name().toLowerCase(Locale.ROOT)),b->{alignment=HudAlignment.values()[(alignment.ordinal()+1)%HudAlignment.values().length];rebuildWidgets();},x,y+28,stock?(w-4)/2:w,22,false));
        if(stock)addRenderableWidget(new PlannerButton(Component.translatable(followHotbar?"stow.stock.follow":"stow.stock.free"),b->{followHotbar=!followHotbar;rebuildWidgets();},x+(w+4)/2,y+28,(w-4)/2,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("stow.dock.edge"),b->{offsetX=0;offsetY=0;followHotbar=false;},x,y+56,w,22,false));
        int cell=(w-8)/3;
        addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b->onClose(),x,y+84,cell,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("controls.reset"),b->{corner=project?DockCorner.TOP_RIGHT:DockCorner.BOTTOM_RIGHT;offsetX=8;offsetY=project?8:62;scale=100;alignment=stock?HudAlignment.LEFT:HudAlignment.RIGHT;followHotbar=stock;rebuildWidgets();},x+cell+4,y+84,cell,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("stow.common.save"),b->{
            if(stock){Stow.config.stockCorner=corner;Stow.config.stockOffsetX=offsetX;Stow.config.stockOffsetY=offsetY;Stow.config.stockScale=scale;Stow.config.stockAlignment=alignment;Stow.config.stockFollowHotbar=followHotbar;}
            else if(project){Stow.config.projectCorner=corner;Stow.config.projectOffsetX=offsetX;Stow.config.projectOffsetY=offsetY;Stow.config.projectScale=scale;Stow.config.projectAlignment=alignment;}
            else{Stow.config.dockCorner=corner;Stow.config.dockOffsetX=offsetX;Stow.config.dockOffsetY=offsetY;Stow.config.dockScale=scale;Stow.config.dockAlignment=alignment;}
            Stow.config.save();UiNotifications.show("stow.feedback.layout-saved");onClose();},x+2*(cell+4),y+84,cell,22,true));
    }
    private Component cornerLabel(){return Component.translatable("stow.dock.corner."+corner.name().toLowerCase(Locale.ROOT));}
    private Component heading(){var store=ChestMemory.currentStore();return project?Component.literal(store==null?Component.translatable("stow.project.preview").getString():store.projectName()):null;}
    private List<SurvivalDock.Row> previewRows(){
        if(stock){var mc=net.minecraft.client.Minecraft.getInstance();var item=mc.player==null?net.minecraft.world.item.ItemStack.EMPTY:mc.player.getInventory().getSelectedItem();if(item.isEmpty()||!(item.getItem() instanceof net.minecraft.world.item.BlockItem))return List.of(new SurvivalDock.Row(MemoryItems.icon("minecraft:cobblestone"),"1361",0xFFFFFFFF));return List.of(new SurvivalDock.Row(item.copyWithCount(1),Long.toString(BuildingStockHud.count(mc.player.getInventory(),item)),0xFFFFFFFF));}
        var live=project?SurvivalDock.projectRows(ChestMemory.currentStore()):SurvivalDock.equipmentRows();
        if(!live.isEmpty())return live;
        return project?List.of(new SurvivalDock.Row(MemoryItems.icon("minecraft:cobblestone"),"524 / 1000",0xFFFFD486),new SurvivalDock.Row(MemoryItems.icon("minecraft:oak_planks"),"256 / 256",0xFF9EE6C4)):
            List.of(new SurvivalDock.Row(MemoryItems.icon("minecraft:iron_pickaxe"),Stow.config.equipmentPercent?"12% · 30":"30",0xFFFFD486),new SurvivalDock.Row(MemoryItems.icon("minecraft:iron_helmet"),Stow.config.equipmentPercent?"4% · 10":"10",0xFFFFAA91));
    }
    public SurvivalDock.Bounds previewBounds(){var rows=previewRows();if(stock&&followHotbar){var b=BuildingStockHud.bounds(width,height,SurvivalDock.contentWidth(rows,null),(float)minecraft.getWindow().getGuiScale(),minecraft.player!=null&&minecraft.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT&&!minecraft.player.getOffhandItem().isEmpty(),scale);return new SurvivalDock.Bounds(b.x(),b.y(),(int)Math.ceil(SurvivalDock.contentWidth(rows,null)*b.scale()),(int)Math.ceil(20*b.scale()),b.scale());}return SurvivalDock.bounds(width,height,SurvivalDock.contentWidth(rows,heading()),SurvivalDock.contentHeight(rows,heading()),corner,offsetX,offsetY,scale);}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){if(super.mouseClicked(e,twice))return true;var box=previewBounds();if(e.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&e.x()>=box.x()&&e.x()<box.x()+box.width()&&e.y()>=box.y()&&e.y()<box.y()+box.height()){dragging=true;grabX=e.x()-box.x();grabY=e.y()-box.y();return true;}return false;}
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(!dragging)return super.mouseDragged(e,dx,dy);var box=previewBounds();int x=Math.clamp((int)(e.x()-grabX),0,Math.max(0,width-box.width())),y=Math.clamp((int)(e.y()-grabY),0,Math.max(0,height-box.height()));
        boolean right=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.TOP_RIGHT,bottom=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.BOTTOM_LEFT;
        offsetX=right?width-x-box.width():x;offsetY=bottom?height-y-box.height():y;followHotbar=false;return true;}
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging){dragging=false;return true;}return super.mouseReleased(e);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        if(minecraft.level==null)extractPanorama(g,delta);
        var box=previewBounds();SurvivalDock.drawAt(g,previewRows(),heading(),box,alignment);
        g.fill(panelX,panelY,panelX+panelWidth,panelY+148,0xE0202020);g.outline(panelX,panelY,panelWidth,148,0xFF808080);
        g.text(font,title,panelX+8,panelY+8,0xFFFFFFFF);
        g.text(font,Component.translatable("stow.dock.drag"),panelX+8,panelY+21,0xFFAAAAAA);
        g.centeredText(font,scale+"%",panelX+panelWidth-54,panelY+41,0xFFFFFFFF);
        if(dragging||x>=box.x()&&x<box.x()+box.width()&&y>=box.y()&&y<box.y()+box.height())g.outline(box.x(),box.y(),box.width(),box.height(),0xFF88D6C1);
        super.extractRenderState(g,x,y,delta);
    }
}
