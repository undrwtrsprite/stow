package dev.stow.client.memory;

import java.util.function.Consumer;
import me.shedaniel.clothconfig2.gui.entries.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Keep Cloth's native controls and validation, with wrapped labels above narrow rows. */
final class ResponsiveConfigEntries {
    private ResponsiveConfigEntries(){}
    private static boolean stacked(Component label,int width){return width<Minecraft.getInstance().font.width(label)+158;}
    private static int labelHeight(Component label,int width){return Minecraft.getInstance().font.split(label,Math.max(40,width)).size()*9+6;}
    static final class Slider extends IntegerSliderEntry {
        private boolean hideLabel;
        Slider(Component label,int value,int min,int max,Component reset,int defaults,Consumer<Integer> save){super(label,min,max,value,reset,()->defaults,save);}
        @Override public Component getDisplayedFieldName(){return hideLabel?Component.empty():super.getDisplayedFieldName();}
        @Override public int getItemHeight(){int width=getParent()==null?Integer.MAX_VALUE:getParent().getItemWidth();return stacked(super.getDisplayedFieldName(),width)?labelHeight(super.getDisplayedFieldName(),width)+24:24;}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int index,int y,int x,int width,int height,int mx,int my,boolean hovered,float delta){
            Component label=super.getDisplayedFieldName();
            if(!stacked(label,width)){super.extractRenderState(g,index,y,x,width,height,mx,my,hovered,delta);return;}
            int offset=labelHeight(label,width);g.textWithWordWrap(Minecraft.getInstance().font,label,x,y+2,width,getPreferredTextColor());hideLabel=true;
            try{super.extractRenderState(g,index,y+offset,x,width,height-offset,mx,my,hovered,delta);}finally{hideLabel=false;}
        }
    }

    static final class Toggle extends BooleanListEntry {
        private boolean hideLabel;
        Toggle(Component label,boolean value,Component reset,boolean defaults,Consumer<Boolean> save){super(label,value,reset,()->defaults,save);}
        @Override public Component getDisplayedFieldName(){return hideLabel?Component.empty():super.getDisplayedFieldName();}
        @Override public int getItemHeight(){int width=getParent()==null?Integer.MAX_VALUE:getParent().getItemWidth();return stacked(super.getDisplayedFieldName(),width)?labelHeight(super.getDisplayedFieldName(),width)+24:24;}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int index,int y,int x,int width,int height,int mx,int my,boolean hovered,float delta){
            Component label=super.getDisplayedFieldName();
            if(!stacked(label,width)){super.extractRenderState(g,index,y,x,width,height,mx,my,hovered,delta);return;}
            int offset=labelHeight(label,width);g.textWithWordWrap(Minecraft.getInstance().font,label,x,y+2,width,getPreferredTextColor());hideLabel=true;
            try{super.extractRenderState(g,index,y+offset,x,width,height-offset,mx,my,hovered,delta);}finally{hideLabel=false;}
        }
    }

    static final class Number extends IntegerListEntry {
        private boolean hideLabel;
        Number(Component label,int value,int min,int max,Component reset,int defaults,Consumer<Integer> save){super(label,value,reset,()->defaults,save);setMinimum(min);setMaximum(max);}
        @Override public Component getDisplayedFieldName(){return hideLabel?Component.empty():super.getDisplayedFieldName();}
        @Override public int getItemHeight(){int width=getParent()==null?Integer.MAX_VALUE:getParent().getItemWidth();return stacked(super.getDisplayedFieldName(),width)?labelHeight(super.getDisplayedFieldName(),width)+24:24;}
        @Override public void extractRenderState(GuiGraphicsExtractor g,int index,int y,int x,int width,int height,int mx,int my,boolean hovered,float delta){
            Component label=super.getDisplayedFieldName();
            if(!stacked(label,width)){super.extractRenderState(g,index,y,x,width,height,mx,my,hovered,delta);return;}
            int offset=labelHeight(label,width);g.textWithWordWrap(Minecraft.getInstance().font,label,x,y+2,width,getPreferredTextColor());hideLabel=true;
            try{super.extractRenderState(g,index,y+offset,x,width,height-offset,mx,my,hovered,delta);}finally{hideLabel=false;}
        }
    }
}
