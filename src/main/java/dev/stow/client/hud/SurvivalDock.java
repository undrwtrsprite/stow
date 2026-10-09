package dev.stow.client.hud;

import dev.stow.Stow;
import dev.stow.StowConfig.DockCorner;
import dev.stow.StowConfig.HudAlignment;
import dev.stow.client.memory.*;
import java.util.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Background-free project/equipment dock. No world markers, camera transforms or mouse capture. */
public final class SurvivalDock {
    public record Row(ItemStack icon,String label,int color) {}
    public record Bounds(int x,int y,int width,int height,float scale) {}
    private SurvivalDock(){}
    public static void register(){HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("stow","survival_dock"),(g,delta)->{
        var mc=Minecraft.getInstance();if(mc.player==null||mc.gui.screen()!=null||mc.gui.hud.isHidden())return;
        draw(g,ChestMemory.currentStore());
    });}
    public static void notice(String key,Object... values){dev.stow.client.ui.UiNotifications.show(key,values);}
    public static void clear(){dev.stow.client.ui.UiNotifications.clear();}
    public static List<Row> equipmentRows(){
        var mc=Minecraft.getInstance();var rows=new ArrayList<Row>();
        if(Stow.config.equipmentWatch&&mc.player!=null){var entries=EquipmentWatch.hudEntries(mc.player.getInventory()).stream().filter(entry->Stow.config.equipmentAll||entry.low()).toList();
            for(var item:entries)rows.add(new Row(item.item(),(Stow.config.equipmentPercent?item.percent()+"% · ":"")+item.remaining(),item.color()));
        }return List.copyOf(rows);
    }
    public static List<Row> projectRows(ChestMemoryStore store){
        var rows=new ArrayList<Row>();if(Stow.config.dockMaterials&&store!=null){var goals=store.hudGoals(Stow.config.dockRows);
            for(var goal:goals.stream().limit(Stow.config.dockRows).toList()){
                var progress=MaterialPlanner.progress(store,goal);
                rows.add(new Row(MemoryItems.icon(goal.itemId()),progress.label(),progress.color()));
            }
            int additional=store.additionalHudGoals(Stow.config.dockRows);
            if(additional>0)rows.add(new Row(ItemStack.EMPTY,Component.translatable("stow.hud.more-materials",additional).getString(),0xFFBDC9CC));
        }return List.copyOf(rows);
    }
    public static Bounds bounds(int width,int height,int contentWidth,int contentHeight,DockCorner corner,int offsetX,int offsetY,int scalePercent){
        float guiScale=(float)Minecraft.getInstance().getWindow().getGuiScale();
        float scale=HudStyle.scale(guiScale,scalePercent);
        scale=Math.min(scale,Math.min((width-8)/(float)Math.max(1,contentWidth),(height-8)/(float)Math.max(1,contentHeight)));
        int w=(int)Math.ceil(contentWidth*scale),h=(int)Math.ceil(contentHeight*scale);
        boolean right=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.TOP_RIGHT,bottom=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.BOTTOM_LEFT;
        int x=Math.clamp(right?width-offsetX-w:offsetX,0,Math.max(0,width-w));
        int y=Math.clamp(bottom?height-offsetY-h:offsetY,0,Math.max(0,height-h));
        return new Bounds(x,y,w,h,scale);
    }
    public static void draw(GuiGraphicsExtractor g,ChestMemoryStore store){
        var equipment=equipmentRows();
        if(Stow.config.dockEnabled)drawRows(g,equipment,null,Stow.config.dockCorner,Stow.config.dockOffsetX,Stow.config.dockOffsetY,Stow.config.dockScale,Stow.config.dockAlignment);
        var project=projectRows(store);if(!project.isEmpty())drawRows(g,project,Component.literal(store.projectName()),Stow.config.projectCorner,Stow.config.projectOffsetX,Stow.config.projectOffsetY,Stow.config.projectScale,Stow.config.projectAlignment);
    }
    public static int contentWidth(List<Row> rows,Component heading){
        var font=Minecraft.getInstance().font;int width=1;
        for(Row row:rows)width=Math.max(width,(row.icon().isEmpty()?0:22)+font.width(row.label()));
        return heading==null?width:Math.max(width,Math.min(180,font.width(heading)));
    }
    public static int contentHeight(List<Row> rows,Component heading){return rows.size()*20+(heading==null?0:14);}
    public static void drawRows(GuiGraphicsExtractor g,List<Row> rows,Component heading,DockCorner corner,int offsetX,int offsetY,int scale,boolean rightAligned){
        drawRows(g,rows,heading,corner,offsetX,offsetY,scale,rightAligned?HudAlignment.RIGHT:HudAlignment.LEFT);
    }
    public static void drawRows(GuiGraphicsExtractor g,List<Row> rows,Component heading,DockCorner corner,int offsetX,int offsetY,int scale,HudAlignment alignment){
        if(rows.isEmpty()&&heading==null)return;int contentWidth=contentWidth(rows,heading);
        int contentHeight=rows.size()*20+(heading!=null?14:0);Bounds box=bounds(g.guiWidth(),g.guiHeight(),contentWidth,contentHeight,corner,offsetX,offsetY,scale);
        drawAt(g,rows,heading,box,alignment);
    }
    public static void drawAt(GuiGraphicsExtractor g,List<Row> rows,Component heading,Bounds box,boolean rightAligned){
        drawAt(g,rows,heading,box,rightAligned?HudAlignment.RIGHT:HudAlignment.LEFT);
    }
    public static void drawAt(GuiGraphicsExtractor g,List<Row> rows,Component heading,Bounds box,HudAlignment alignment){
        var font=Minecraft.getInstance().font;int contentWidth=contentWidth(rows,heading);
        var pose=g.pose();pose.pushMatrix();pose.translate(box.x(),box.y());pose.scale(box.scale(),box.scale());int y=0;
        if(heading!=null){String title=font.plainSubstrByWidth(heading.getString(),contentWidth);g.text(font,title,alignedX(contentWidth,font.width(title),alignment),y,0xFFBDC9CC);y+=14;}
        for(Row row:rows){
            int rowWidth=font.width(row.label())+(row.icon().isEmpty()?0:22),x=alignedX(contentWidth,rowWidth,alignment);
            if(alignment==HudAlignment.RIGHT){if(!row.icon().isEmpty())g.item(row.icon(),contentWidth-16,y);g.text(font,row.label(),x,y+4,row.color());}
            else {if(!row.icon().isEmpty())g.item(row.icon(),x,y);g.text(font,row.label(),x+(row.icon().isEmpty()?0:22),y+4,row.color());}y+=20;
        }pose.popMatrix();
    }
    public static int alignedX(int width,int rowWidth,HudAlignment alignment){return switch(alignment){case LEFT->0;case CENTER->(width-rowWidth)/2;case RIGHT->width-rowWidth;};}
}
