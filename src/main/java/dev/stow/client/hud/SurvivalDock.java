package dev.stow.client.hud;

import dev.stow.Stow;
import dev.stow.StowConfig.DockCorner;
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
    private static Component notification;private static long notificationUntil;
    private SurvivalDock(){}
    public static void register(){HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("stow","survival_dock"),(g,delta)->{
        var mc=Minecraft.getInstance();if(mc.player==null||mc.gui.screen()!=null||mc.gui.hud.isHidden())return;
        draw(g,ChestMemory.currentStore());
    });}
    public static void notice(String key,Object... values){notification=Component.translatable(key,values);notificationUntil=System.currentTimeMillis()+3500;}
    public static void clear(){notification=null;notificationUntil=0;}
    public static List<Row> rows(ChestMemoryStore store){
        var mc=Minecraft.getInstance();var rows=new ArrayList<Row>();
        if(Stow.config.equipmentWatch&&mc.player!=null){var warnings=EquipmentWatch.warnings(mc.player.getInventory());
            for(var item:warnings.stream().limit(2).toList())rows.add(new Row(item.item(),item.percent()+"% · "+item.remaining(),item.color()));
            if(warnings.size()>2)rows.add(new Row(ItemStack.EMPTY,Component.translatable("stow.watch.more",warnings.size()-2).getString(),0xFFFFD486));
        }
        if(Stow.config.dockMaterials&&store!=null){var goals=store.visibleGoals();
            for(var goal:goals.stream().limit(Stow.config.dockRows).toList()){
                var progress=MaterialPlanner.progress(store,goal);
                rows.add(new Row(MemoryItems.icon(goal.itemId()),progress.total()+" / "+goal.target(),progress.enough()?0xFF8DE8B2:0xFFFFFFFF));
            }
            if(goals.size()>Stow.config.dockRows)rows.add(new Row(ItemStack.EMPTY,Component.translatable("stow.dock.more",goals.size()-Stow.config.dockRows).getString(),0xFFBCC7CA));
        }
        return List.copyOf(rows);
    }
    public static Bounds bounds(int width,int height,int contentWidth,int contentHeight,DockCorner corner,int offsetX,int offsetY,int scalePercent){
        float scale=Math.clamp(scalePercent,60,150)/100f;
        scale=Math.min(scale,Math.min((width-8)/(float)Math.max(1,contentWidth),(height-8)/(float)Math.max(1,contentHeight)));
        int w=(int)Math.ceil(contentWidth*scale),h=(int)Math.ceil(contentHeight*scale);
        boolean right=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.TOP_RIGHT,bottom=corner==DockCorner.BOTTOM_RIGHT||corner==DockCorner.BOTTOM_LEFT;
        int x=Math.clamp(right?width-offsetX-w:offsetX,4,Math.max(4,width-w-4));
        int y=Math.clamp(bottom?height-offsetY-h:offsetY,4,Math.max(4,height-h-4));
        return new Bounds(x,y,w,h,scale);
    }
    public static void draw(GuiGraphicsExtractor g,ChestMemoryStore store){
        if(!Stow.config.dockEnabled)return;
        var mc=Minecraft.getInstance();var font=mc.font;List<Row> rows=rows(store);
        boolean notice=notification!=null&&System.currentTimeMillis()<notificationUntil;
        boolean project=Stow.config.dockMaterials&&store!=null&&!store.visibleGoals().isEmpty();
        if(rows.isEmpty()&&!notice)return;
        int contentWidth=80;for(Row row:rows)contentWidth=Math.max(contentWidth,22+font.width(row.label()));
        if(project)contentWidth=Math.max(contentWidth,Math.min(180,font.width(store.projectName())));
        if(notice)contentWidth=Math.max(contentWidth,Math.min(180,font.width(notification)));
        int contentHeight=rows.size()*20+(project?14:0)+(notice?14:0);
        Bounds box=bounds(g.guiWidth(),g.guiHeight(),contentWidth,contentHeight,Stow.config.dockCorner,Stow.config.dockOffsetX,Stow.config.dockOffsetY,Stow.config.dockScale);
        var pose=g.pose();pose.pushMatrix();pose.translate(box.x(),box.y());pose.scale(box.scale(),box.scale());int y=0;
        if(notice){g.text(font,font.plainSubstrByWidth(notification.getString(),contentWidth),0,y,0xFFB5FFEE);y+=14;}
        if(project){g.text(font,font.plainSubstrByWidth(store.projectName(),contentWidth),0,y,0xFFBDC9CC);y+=14;}
        for(Row row:rows){if(!row.icon().isEmpty())g.item(row.icon(),0,y);g.text(font,row.label(),row.icon().isEmpty()?0:22,y+4,row.color());y+=20;}
        pose.popMatrix();
    }
}
