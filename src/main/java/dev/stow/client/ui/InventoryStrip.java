package dev.stow.client.ui;

import dev.stow.Stow;
import dev.stow.StowConfig.SortOrder;
import dev.stow.client.input.StowShortcuts;
import dev.stow.client.input.StowShortcuts.Action;
import dev.stow.client.inventory.InventorySorting;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import static dev.stow.client.stow.StowTheme.*;

/** Redesign v2 §3.5 helpers for the container strip: the Sort control and the key legend. */
public final class InventoryStrip {
    private InventoryStrip(){}
    /** Sort the open container, or the player's main inventory when there is no container (or inventory=true). */
    public static boolean sort(AbstractContainerScreen<?> screen,boolean inventory){
        Slot target=null;boolean player=inventory||screen instanceof InventoryScreen;
        for(Slot slot:screen.getMenu().slots){
            boolean own=slot.container instanceof Inventory;
            if(player?own&&slot.getContainerSlot()>=9&&slot.getContainerSlot()<36:!own){target=slot;break;}
        }
        if(target==null)return false;
        boolean started=InventorySorting.start(screen,target);
        if(!started)UiNotifications.show("stow.strip.sort-unavailable");
        return started;
    }
    /** Scroll on the Sort control: change the plain-click order. */
    public static void cycleOrder(double direction){
        var values=SortOrder.values();int i=Stow.config.sortOrder.ordinal();
        Stow.config.sortOrder=values[Math.floorMod(i+(direction<0?1:-1),values.length)];Stow.config.save();
        UiNotifications.show(Component.translatable("stow.strip.sort-order",Component.translatable("stow.sort."+Stow.config.sortOrder.name().toLowerCase(Locale.ROOT))));
    }
    public static Component sortTooltip(){
        return Component.translatable("stow.strip.sort.tip",Component.translatable("stow.sort."+Stow.config.sortOrder.name().toLowerCase(Locale.ROOT)));
    }
    private record Entry(String key,String action) {}
    /** The legend shows the player's actual bindings. */
    private static List<Entry> legend(){
        var out=new ArrayList<Entry>();
        for(var action:List.of(Action.PIN,Action.TRACK,Action.FIND_HOVERED,Action.HUD_GOAL,Action.SORT,Action.SEARCH,Action.PALETTE)){
            var bound=StowShortcuts.binding(action);if(bound.code()<0&&action!=Action.FIND_HOVERED)continue;
            out.add(new Entry(bound.code()<0?Component.translatable("stow.strip.legend.unbound").getString():StowShortcuts.label(bound),Component.translatable("stow.strip.legend."+action.name().toLowerCase(Locale.ROOT)).getString()));
        }
        return out;
    }
    public static int legendWidth(){var font=Minecraft.getInstance().font;int w=0;for(var e:legend())w=Math.max(w,font.width(e.key())+10+font.width(e.action()));return Math.max(w,font.width(Component.translatable("stow.strip.legend.title")))+12;}
    public static int legendHeight(){return 16+legend().size()*10+4;}
    public static void drawLegend(GuiGraphicsExtractor g,int x,int y){
        var font=Minecraft.getInstance().font;var entries=legend();int w=legendWidth(),h=legendHeight();
        g.nextStratum();
        g.fill(x,y,x+w,y+h,0xF02E3033);g.outline(x,y,w,h,LINE);
        section(g,Component.translatable("stow.strip.legend.title").getString(),x+6,y+5);
        int keyW=0;for(var e:entries)keyW=Math.max(keyW,font.width(e.key()));
        int ty=y+15;for(var e:entries){g.text(font,e.key(),x+6,ty,GOLD);g.text(font,e.action(),x+6+keyW+8,ty,CREAM);ty+=10;}
    }
}
