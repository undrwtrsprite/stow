package dev.stow.client.ui;

import dev.stow.client.hud.EquipmentWatch;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EquipmentScreen extends ToolListScreen {
    private List<EquipmentWatch.Entry> items=List.of();
    public EquipmentScreen(Screen parent){super(Component.translatable("stow.watch.title"),parent);}
    @Override protected void init(){begin(46,Component.translatable("stow.watch.search"));refresh();}
    @Override protected void refresh(){
        String query=search.getValue().strip().toLowerCase(Locale.ROOT);
        items=EquipmentWatch.entries(minecraft.player==null?null:minecraft.player.getInventory()).stream().filter(e->e.item().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)).toList();resetRows(items.size());
    }
    @Override public void tick(){refresh();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        chrome(g,delta);g.text(font,Component.translatable("stow.watch.hint"),left+8,72,0xFFAAAAAA);
        for(int i=page*pageSize;i<Math.min(items.size(),(page+1)*pageSize);i++){
            var item=items.get(i);int top=rowY(i-page*pageSize);g.item(item.item(),left+8,top);
            String count=item.percent()+"% · "+item.remaining()+" / "+item.maximum();int countWidth=font.width(count);
            g.text(font,font.plainSubstrByWidth(item.item().getHoverName().getString(),Math.max(30,bodyWidth-countWidth-52)),left+32,top+4,0xFFFFFFFF);
            g.text(font,count,left+bodyWidth-countWidth-8,top+4,item.color());
            g.fill(left+32,top+22,left+bodyWidth-8,top+25,0xFF383F43);
            g.fill(left+32,top+22,left+32+(bodyWidth-40)*item.percent()/100,top+25,item.color());
        }
        if(items.isEmpty())g.text(font,Component.translatable("stow.watch.empty"),left+8,92,0xFFAAAAAA);
        super.extractRenderState(g,x,y,delta);
    }
}
