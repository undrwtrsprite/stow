package dev.stow.client.stow;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stow.Stow;
import dev.stow.StowConfig.DockCorner;
import dev.stow.StowConfig.HudAlignment;
import dev.stow.client.hud.AutomationHud;
import dev.stow.client.hud.BuildingStockHud;
import dev.stow.client.hud.SurvivalDock;
import dev.stow.client.memory.*;
import dev.stow.client.ui.UiNotifications;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import static dev.stow.client.stow.StowTheme.*;

/**
 * Redesign v2 §3.7: one editor for every Stow HUD element. Positions are kept in the existing
 * settings fields (project*, stock*, dock*), plus the new automation* fields. Nothing is written
 * until Save; Cancel and Esc discard.
 */
public final class ArrangeHudScreen extends Screen {
    public enum Element { PROJECT,STOCK,EQUIPMENT,AUTOMATION;
        Component title(){return Component.translatable("stow.ui.arrange."+name().toLowerCase(Locale.ROOT));} }
    private static final class Layout {
        DockCorner corner;int x,y,scale;HudAlignment align;boolean follow,enabled;
        Layout(DockCorner corner,int x,int y,int scale,HudAlignment align,boolean follow,boolean enabled){this.corner=corner;this.x=x;this.y=y;this.scale=scale;this.align=align;this.follow=follow;this.enabled=enabled;}
    }
    private final Screen parent;
    private final EnumMap<Element,Layout> layouts=new EnumMap<>(Element.class);
    private Element selected;
    private boolean dragging;private double grabX,grabY;
    private int panelX,panelY,panelW,panelH;
    private String guide="";private int guideX=-1,guideY=-1;

    public ArrangeHudScreen(Screen parent,Element element){
        super(Component.translatable("stow.ui.arrange.title"));this.parent=parent;selected=element==null?Element.PROJECT:element;
        var c=Stow.config;
        layouts.put(Element.PROJECT,new Layout(c.projectCorner,c.projectOffsetX,c.projectOffsetY,c.projectScale,c.projectAlignment,false,c.dockMaterials));
        layouts.put(Element.STOCK,new Layout(c.stockCorner,c.stockOffsetX,c.stockOffsetY,c.stockScale,c.stockAlignment,c.stockFollowHotbar,c.buildingStock));
        layouts.put(Element.EQUIPMENT,new Layout(c.dockCorner,c.dockOffsetX,c.dockOffsetY,c.dockScale,c.dockAlignment,false,c.dockEnabled&&c.equipmentWatch));
        layouts.put(Element.AUTOMATION,new Layout(c.automationCorner,c.automationOffsetX,c.automationOffsetY,c.automationScale,c.automationAlignment,false,c.automationHud));
    }
    private Layout cur(){return layouts.get(selected);}
    private int maxScale(Element e){return e==Element.STOCK?200:150;}
    private static DockCorner defaultCorner(Element e){return switch(e){case PROJECT->DockCorner.TOP_RIGHT;case AUTOMATION->DockCorner.TOP_LEFT;default->DockCorner.BOTTOM_RIGHT;};}

    // ---------------------------------------------------------------- preview data
    private Component heading(Element e){if(e!=Element.PROJECT)return null;var store=ChestMemory.currentStore();return Component.literal(store==null?Component.translatable("stow.project.preview").getString():store.projectName());}
    private List<SurvivalDock.Row> rows(Element e){
        return switch(e){
            case STOCK->{var mc=minecraft;var item=mc.player==null?ItemStack.EMPTY:mc.player.getInventory().getSelectedItem();
                if(item.isEmpty()||!(item.getItem() instanceof net.minecraft.world.item.BlockItem))yield List.of(new SurvivalDock.Row(MemoryItems.icon("minecraft:cobblestone"),"1361",0xFFFFFFFF));
                yield List.of(new SurvivalDock.Row(item.copyWithCount(1),Long.toString(BuildingStockHud.count(mc.player.getInventory(),item)),0xFFFFFFFF));}
            case PROJECT->{var live=SurvivalDock.projectRows(ChestMemory.currentStore());yield !live.isEmpty()?live:List.of(new SurvivalDock.Row(MemoryItems.icon("minecraft:cobblestone"),"524 / 1000",0xFFFFD486),new SurvivalDock.Row(MemoryItems.icon("minecraft:oak_planks"),"256 / 256",0xFF9EE6C4));}
            case EQUIPMENT->{var live=SurvivalDock.equipmentRows();yield !live.isEmpty()?live:List.of(new SurvivalDock.Row(MemoryItems.icon("minecraft:iron_pickaxe"),Stow.config.equipmentPercent?"12%":"30",0xFFFFD486),new SurvivalDock.Row(MemoryItems.icon("minecraft:iron_helmet"),Stow.config.equipmentPercent?"4%":"10",0xFFFFAA91));}
            case AUTOMATION->AutomationHud.previewRows();
        };
    }
    private SurvivalDock.Bounds bounds(Element e){
        var l=layouts.get(e);var rows=rows(e);var heading=heading(e);
        if(e==Element.STOCK&&l.follow){
            int cw=SurvivalDock.contentWidth(rows,null);
            var b=BuildingStockHud.bounds(width,height,cw,(float)minecraft.getWindow().getGuiScale(),minecraft.player!=null&&minecraft.player.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT&&!minecraft.player.getOffhandItem().isEmpty(),l.scale);
            return new SurvivalDock.Bounds(b.x(),b.y(),(int)Math.ceil(cw*b.scale()),(int)Math.ceil(20*b.scale()),b.scale());
        }
        return SurvivalDock.bounds(width,height,SurvivalDock.contentWidth(rows,heading),SurvivalDock.contentHeight(rows,heading),l.corner,l.x,l.y,l.scale);
    }
    private int[] hotbar(){return new int[]{width/2-91,height-40,182,40};}
    private static boolean overlaps(SurvivalDock.Bounds a,int x,int y,int w,int h){return a.x()<x+w&&x<a.x()+a.width()&&a.y()<y+h&&y<a.y()+a.height();}
    private String overlap(Element e){
        var b=bounds(e);int[] hb=hotbar();
        if(!(e==Element.STOCK&&layouts.get(e).follow)&&overlaps(b,hb[0],hb[1],hb[2],hb[3]))return Component.translatable("stow.ui.arrange.overlap-hotbar").getString();
        for(var other:Element.values()){if(other==e||!layouts.get(other).enabled)continue;var o=bounds(other);if(overlaps(b,o.x(),o.y(),o.width(),o.height()))return Component.translatable("stow.ui.arrange.overlap",other.title()).getString();}
        return "";
    }

    // ---------------------------------------------------------------- panel
    private int fitW(Component c,int min){return Math.max(min,font.width(c)+14);}
    @Override protected void init(){
        panelW=Math.min(232,width-16);panelH=Math.min(height-16,262);
        var b=bounds(selected);boolean elementLeft=b.x()+b.width()/2<width/2,elementTop=b.y()+b.height()/2<height/2;
        panelX=elementLeft?width-panelW-8:8;panelY=elementTop?height-panelH-8:8;
        if(panelY<8)panelY=8;
        int x=panelX+8,y=panelY+30,w=panelW-16;
        for(var e:Element.values()){
            var l=layouts.get(e);
            addRenderableWidget(new PlannerButton(e.title(),btn->{selected=e;rebuildWidgets();},x,y,w-46,18,false).selected(()->selected==e));
            var toggle=addRenderableWidget(new PlannerButton(Component.translatable(l.enabled?"options.on":"options.off"),btn->{l.enabled=!l.enabled;rebuildWidgets();},x+w-42,y,42,18,false));
            toggle.setTooltip(Tooltip.create(Component.translatable("stow.ui.arrange.toggle-tip",e.title())));
            y+=20;
        }
        y+=6;var l=cur();
        // corner grid
        int cw=(w-4)/2;
        DockCorner[] grid={DockCorner.TOP_LEFT,DockCorner.TOP_RIGHT,DockCorner.BOTTOM_LEFT,DockCorner.BOTTOM_RIGHT};
        for(int i=0;i<4;i++){var corner=grid[i];
            addRenderableWidget(new PlannerButton(Component.translatable("stow.dock.corner."+corner.name().toLowerCase(Locale.ROOT)),btn->{setCorner(corner);},x+(i%2)*(cw+4),y+(i/2)*20,cw,18,false).selected(()->cur().corner==corner&&!cur().follow));}
        y+=44;
        addRenderableWidget(new PlannerButton(Component.literal("−"),btn->{l.scale=Math.max(60,l.scale-10);},x+w-76,y,20,18,false)).setTooltip(Tooltip.create(Component.translatable("stow.ui.arrange.smaller")));
        addRenderableWidget(new PlannerButton(Component.literal("+"),btn->{l.scale=Math.min(maxScale(selected),l.scale+10);},x+w-20,y,20,18,false)).setTooltip(Tooltip.create(Component.translatable("stow.ui.arrange.larger")));
        y+=22;
        Component align=Component.translatable("stow.hud.align."+l.align.name().toLowerCase(Locale.ROOT));
        int half=(w-4)/2;
        addRenderableWidget(new PlannerButton(align,btn->{l.align=HudAlignment.values()[(l.align.ordinal()+1)%HudAlignment.values().length];rebuildWidgets();},x,y,selected==Element.STOCK?half:w,18,false));
        if(selected==Element.STOCK)addRenderableWidget(new PlannerButton(Component.translatable(l.follow?"stow.stock.follow":"stow.stock.free"),btn->{l.follow=!l.follow;rebuildWidgets();},x+half+4,y,half,18,false));
        y+=20;
        addRenderableWidget(new PlannerButton(Component.translatable("stow.dock.edge"),btn->{l.x=0;l.y=0;l.follow=false;},x,y,w,18,false));
        y+=24;
        int third=(w-8)/3;
        addRenderableWidget(new PlannerButton(Component.translatable("controls.reset"),btn->reset(),x,y,third,20,false)).setTooltip(Tooltip.create(Component.translatable("stow.ui.arrange.reset-tip",selected.title())));
        addRenderableWidget(new PlannerButton(Component.translatable("gui.cancel"),btn->onClose(),x+third+4,y,third,20,false));
        addRenderableWidget(new PlannerButton(Component.translatable("stow.common.save"),btn->save(),x+2*(third+4),y,third,20,true));
        panelH=y+26-panelY;
    }
    /** Changing the anchor keeps the element where it is on screen. */
    private void setCorner(DockCorner corner){
        var l=cur();var b=bounds(selected);l.corner=corner;l.follow=false;
        boolean right=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.TOP_RIGHT,bottom=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.BOTTOM_LEFT;
        l.x=Math.clamp(right?width-b.x()-b.width():b.x(),0,1000);l.y=Math.clamp(bottom?height-b.y()-b.height():b.y(),0,1000);
        rebuildWidgets();
    }
    private void reset(){
        var l=cur();l.corner=defaultCorner(selected);l.scale=100;
        switch(selected){
            case PROJECT->{l.x=8;l.y=8;l.align=HudAlignment.RIGHT;}
            case STOCK->{l.x=8;l.y=62;l.align=HudAlignment.LEFT;l.follow=true;}
            case EQUIPMENT->{l.x=8;l.y=62;l.align=HudAlignment.RIGHT;}
            case AUTOMATION->{l.x=8;l.y=8;l.align=HudAlignment.LEFT;}
        }
        rebuildWidgets();
    }
    private void save(){
        var c=Stow.config;
        var p=layouts.get(Element.PROJECT);c.projectCorner=p.corner;c.projectOffsetX=p.x;c.projectOffsetY=p.y;c.projectScale=p.scale;c.projectAlignment=p.align;c.dockMaterials=p.enabled;
        var s=layouts.get(Element.STOCK);c.stockCorner=s.corner;c.stockOffsetX=s.x;c.stockOffsetY=s.y;c.stockScale=s.scale;c.stockAlignment=s.align;c.stockFollowHotbar=s.follow;c.buildingStock=s.enabled;
        var e=layouts.get(Element.EQUIPMENT);c.dockCorner=e.corner;c.dockOffsetX=e.x;c.dockOffsetY=e.y;c.dockScale=e.scale;c.dockAlignment=e.align;c.dockEnabled=e.enabled;c.equipmentWatch=e.enabled;
        var a=layouts.get(Element.AUTOMATION);c.automationCorner=a.corner;c.automationOffsetX=a.x;c.automationOffsetY=a.y;c.automationScale=a.scale;c.automationAlignment=a.align;c.automationHud=a.enabled;
        c.save();UiNotifications.show("stow.feedback.layout-saved");onClose();
    }

    // ---------------------------------------------------------------- rendering and input
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(minecraft.level==null)extractPanorama(g,delta);
        g.fill(0,0,width,height,0x30000000);
        int[] hb=hotbar();g.outline(hb[0],hb[1],hb[2],hb[3],0x40FFFFFF);
        if(dragging&&guideX>=0){g.verticalLine(guideX,0,height,GOLD);g.text(font,guide,guideX+3,4,GOLD);}
        if(dragging&&guideY>=0){g.horizontalLine(0,width,guideY,GOLD);g.text(font,guide,4,guideY+3,GOLD);}
        for(var e:Element.values()){
            var l=layouts.get(e);var b=bounds(e);
            if(l.enabled)SurvivalDock.drawAt(g,rows(e),heading(e),b,l.align);
            String warning=l.enabled?overlap(e):"";
            int color=!warning.isEmpty()?CLAY:e==selected?GOLD:inside(mx,my,b.x(),b.y(),b.width(),b.height())?CREAM:l.enabled?0x80EFE6CF:0x40EFE6CF;
            g.outline(b.x()-2,b.y()-2,b.width()+4,b.height()+4,color);
            int ty=b.y()>12?b.y()-12:b.y()+b.height()+3;
            String tag=e.title().getString()+(l.enabled?"":" · "+Component.translatable("options.off").getString());int tx=Math.max(2,Math.min(b.x(),width-2-font.width(tag)));g.text(font,tag,tx,ty,e==selected?GOLD:MUTED);
            if(!warning.isEmpty())g.text(font,"! "+warning,b.x(),ty+(ty<b.y()?-10:10),CLAY);
        }
        g.fill(panelX,panelY,panelX+panelW,panelY+panelH,0xF02E3033);g.outline(panelX,panelY,panelW,panelH,LINE);
        g.text(font,title,panelX+8,panelY+7,CREAM);
        text(g,Component.translatable("stow.ui.arrange.hint").getString(),panelX+8,panelY+18,panelW-16,MUTED);
        var l=cur();
        for(var child:children())if(child instanceof PlannerButton pb&&pb.getMessage().getString().equals("−")){text(g,Component.translatable("stow.ui.arrange.size",selected.title()).getString(),panelX+8,pb.getY()+5,panelW-106,MUTED);g.centeredText(font,l.scale+" %",pb.getX()+38,pb.getY()+5,CREAM);}
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice){
        if(super.mouseClicked(e,twice))return true;
        if(inside(e.x(),e.y(),panelX,panelY,panelW,panelH))return true;
        // topmost first: later elements draw over earlier ones
        var order=new ArrayList<>(List.of(Element.values()));Collections.reverse(order);
        for(var el:order){var b=bounds(el);if(inside(e.x(),e.y(),b.x()-2,b.y()-2,b.width()+4,b.height()+4)){
            boolean changed=el!=selected;selected=el;
            if(e.button()==InputConstants.MOUSE_BUTTON_LEFT){dragging=true;grabX=e.x()-b.x();grabY=e.y()-b.y();}
            if(changed)rebuildWidgets();return true;}}
        return false;
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){
        if(!dragging)return super.mouseDragged(e,dx,dy);
        var l=cur();var b=bounds(selected);
        int x=Math.clamp((int)(e.x()-grabX),0,Math.max(0,width-b.width())),y=Math.clamp((int)(e.y()-grabY),0,Math.max(0,height-b.height()));
        guideX=-1;guideY=-1;guide="";
        if(!minecraft.hasAltDown()){
            int center=x+b.width()/2;if(Math.abs(center-width/2)<=4){x=width/2-b.width()/2;guideX=width/2;guide=Component.translatable("stow.ui.arrange.guide-center").getString();}
            if(Math.abs(x-8)<=3){x=8;guideX=8;guide=Component.translatable("stow.ui.arrange.guide-margin").getString();}
            if(Math.abs(width-x-b.width()-8)<=3){x=width-b.width()-8;guideX=width-8;guide=Component.translatable("stow.ui.arrange.guide-margin").getString();}
            if(Math.abs(y-8)<=3){y=8;guideY=8;guide=Component.translatable("stow.ui.arrange.guide-margin").getString();}
            if(Math.abs(height-y-b.height()-8)<=3){y=height-b.height()-8;guideY=height-8;guide=Component.translatable("stow.ui.arrange.guide-margin").getString();}
        }
        boolean right=l.corner==DockCorner.BOTTOM_RIGHT||l.corner==DockCorner.TOP_RIGHT,bottom=l.corner==DockCorner.BOTTOM_RIGHT||l.corner==DockCorner.BOTTOM_LEFT;
        if(l.follow){l.follow=false;rebuildWidgets();}
        l.x=Math.clamp(right?width-x-b.width():x,0,1000);l.y=Math.clamp(bottom?height-y-b.height():y,0,1000);
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e){if(dragging){dragging=false;guideX=guideY=-1;rebuildWidgets();return true;}return super.mouseReleased(e);}
    @Override public boolean keyPressed(KeyEvent e){
        if(e.isEscape()){onClose();return true;}
        if(e.hasControlDown()&&e.key()==InputConstants.KEY_S){save();return true;}
        int step=e.hasShiftDown()?10:1,dx=0,dy=0;
        switch(e.key()){case InputConstants.KEY_LEFT->dx=-step;case InputConstants.KEY_RIGHT->dx=step;case InputConstants.KEY_UP->dy=-step;case InputConstants.KEY_DOWN->dy=step;
            case InputConstants.KEY_TAB->{if(getFocused()==null||e.hasControlDown()){selected=Element.values()[(selected.ordinal()+(e.hasShiftDown()?Element.values().length-1:1))%Element.values().length];rebuildWidgets();return true;}}
            default->{}}
        if(dx!=0||dy!=0){
            var l=cur();l.follow=false;
            boolean right=l.corner==DockCorner.BOTTOM_RIGHT||l.corner==DockCorner.TOP_RIGHT,bottom=l.corner==DockCorner.BOTTOM_RIGHT||l.corner==DockCorner.BOTTOM_LEFT;
            l.x=Math.clamp(l.x+(right?-dx:dx),0,1000);l.y=Math.clamp(l.y+(bottom?-dy:dy),0,1000);return true;
        }
        return super.keyPressed(e);
    }
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
