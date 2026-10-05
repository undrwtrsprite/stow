package dev.stow.client.ui;

import dev.stow.client.hud.EquipmentWatch;
import dev.stow.client.memory.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Existing projects plus carried-vs-owned supplies, with quick access to equipment and storage. */
public final class WorkspaceScreen extends ToolListScreen {
    private ChestMemoryStore store;private List<ChestMemoryStore.MaterialGoal> goals=List.of();
    public WorkspaceScreen(Screen parent){super(Component.translatable("stow.workspace.title"),parent);}
    @Override protected void init(){begin(48,Component.translatable("stow.need.search"));store=ChestMemory.currentStore();refresh();}
    @Override protected void refresh(){goals=store==null?List.of():store.goals().stream().filter(g->MemoryItems.name(g.itemId()).toLowerCase(Locale.ROOT).contains(search.getValue().toLowerCase(Locale.ROOT))).toList();resetRows(goals.size());}
    @Override public void tick(){refresh();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        chrome(g,delta);
        int free=0;if(minecraft.player!=null)for(var item:minecraft.player.getInventory().getNonEquipmentItems())if(item.isEmpty())free++;
        int warnings=EquipmentWatch.warnings(minecraft.player==null?null:minecraft.player.getInventory()).size();
        String summary=Component.translatable("stow.workspace.summary",free,warnings).getString();g.text(font,font.plainSubstrByWidth(summary,bodyWidth-16),left+8,72,0xFFAAAAAA);
        for(int i=page*pageSize;i<Math.min(goals.size(),(page+1)*pageSize);i++){
            var goal=goals.get(i);var p=MaterialPlanner.progress(store,goal);int top=rowY(i-page*pageSize);
            g.item(MemoryItems.icon(goal.itemId()),left+8,top+2);
            String total=p.total()+" / "+goal.target();g.text(font,font.plainSubstrByWidth(MemoryItems.name(goal.itemId()),Math.max(40,bodyWidth-font.width(total)-50)),left+32,top+4,0xFFFFFFFF);
            g.text(font,total,left+bodyWidth-font.width(total)-8,top+4,p.enough()?0xFF8DE8B2:0xFFFFFFFF);
            String note=Component.translatable(p.inventory()>=goal.target()?"stow.workspace.packed":p.enough()?"stow.workspace.collect":"stow.workspace.gather",p.inventory(),p.enough()?Math.max(0,goal.target()-p.inventory()):p.missing()).getString();
            g.text(font,font.plainSubstrByWidth(note,bodyWidth-40),left+32,top+21,0xFFBDC9CC);
        }
        if(goals.isEmpty())g.textWithWordWrap(font,Component.translatable("stow.workspace.empty"),left+8,92,bodyWidth-16,0xFFAAAAAA);
        super.extractRenderState(g,x,y,delta);
    }
}
