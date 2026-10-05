package dev.stow.client.memory;

import java.util.*;
import java.util.function.*;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Cloth's native selector/reset controls; narrow rows put their label on a separate line. */
final class ResponsiveEnumEntry<T extends Enum<?>> extends EnumListEntry<T> {
    private boolean hideLabel;
    ResponsiveEnumEntry(Component label,Class<T> type,T value,Component reset,T defaults,Consumer<T> save,Function<Enum,Component> names,Component tooltip){
        super(label,type,value,reset,()->defaults,save,names,()->tooltip==null?Optional.empty():Optional.of(new Component[]{tooltip}));
    }
    private boolean stacked(int rowWidth){return rowWidth<Minecraft.getInstance().font.width(super.getDisplayedFieldName())+158;}
    @Override public int getItemHeight(){return getParent()!=null&&stacked(getParent().getItemWidth())?44:24;}
    @Override public Component getDisplayedFieldName(){return hideLabel?Component.empty():super.getDisplayedFieldName();}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int index,int y,int x,int rowWidth,int rowHeight,int mx,int my,boolean hovered,float delta){
        if(!stacked(rowWidth)){super.extractRenderState(g,index,y,x,rowWidth,rowHeight,mx,my,hovered,delta);return;}
        g.text(Minecraft.getInstance().font,super.getDisplayedFieldName(),x,y+2,getPreferredTextColor());
        hideLabel=true;
        try{super.extractRenderState(g,index,y+16,x,rowWidth,rowHeight-16,mx,my,hovered,delta);}finally{hideLabel=false;}
    }
}
