package dev.stow.client.ui;

import dev.stow.client.memory.MemoryScreenBase;
import dev.stow.client.memory.PlannerButton;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Scrollable release highlights with short how-to steps. Closing from the automatic prompt marks the version as seen. */
public final class WhatsNewScreen extends MemoryScreenBase {
    public static final String RELEASES="https://github.com/undrwtrsprite/stow/releases";
    private record Line(FormattedCharSequence text,int x,int y,int color){}
    private final List<WhatsNew.Entry> entries;
    private final boolean automatic;
    private final List<Line> lines=new ArrayList<>();
    private int top,bottom,textLeft,textWidth,contentHeight;
    private double scroll;
    public WhatsNewScreen(Screen parent,List<WhatsNew.Entry> entries,boolean automatic){
        super(Component.translatable("stow.whatsnew.title"),parent,null);this.entries=entries;this.automatic=automatic;
    }
    @Override protected void init(){
        layoutDialog(height);
        top=40;bottom=height-40;textWidth=Math.min(contentWidth()-32,440);textLeft=(width-textWidth)/2;
        int buttonW=Math.min(150,(textWidth-8)/2);
        addRenderableWidget(new PlannerButton(Component.translatable("stow.whatsnew.changelog"),
            b->ConfirmLinkScreen.confirmLinkNow(this,URI.create(RELEASES)),width/2-buttonW-4,height-30,buttonW,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("stow.whatsnew.got-it"),b->onClose(),width/2+4,height-30,buttonW,22,true));
        layoutText();
    }
    private void layoutText(){
        lines.clear();int y=4;
        if(entries.isEmpty()){for(var part:font.split(Component.translatable("stow.whatsnew.empty"),textWidth)){lines.add(new Line(part,0,y,0xFFB8C6CC));y+=11;}}
        for(var entry:entries){
            lines.add(new Line(Component.translatable("stow.whatsnew.version",entry.version()).getVisualOrderText(),0,y,0xFFFFD486));y+=16;
            for(var section:entry.sections()){
                for(var part:font.split(Component.translatable(section.title()),textWidth)){lines.add(new Line(part,0,y,0xFFFFFFFF));y+=11;}
                y+=1;
                for(var part:font.split(Component.translatable(section.text()),textWidth)){lines.add(new Line(part,0,y,0xFFC8D0D8));y+=10;}
                if(!section.how().isEmpty()){
                    y+=3;lines.add(new Line(Component.translatable("stow.whatsnew.how").getVisualOrderText(),0,y,0xFF7CFF8A));y+=11;
                    for(var step:section.how()){
                        boolean firstLine=true;
                        for(var part:font.split(Component.translatable(step),textWidth-12)){
                            if(firstLine)lines.add(new Line(Component.literal("•").getVisualOrderText(),2,y,0xFF7CFF8A));
                            lines.add(new Line(part,12,y,0xFFE6EAEE));y+=10;firstLine=false;
                        }
                        y+=1;
                    }
                }
                y+=9;
            }
            y+=6;
        }
        contentHeight=y;clampScroll();
    }
    private int maxScroll(){return Math.max(0,contentHeight-(bottom-top));}
    private void clampScroll(){scroll=Math.clamp(scroll,0,maxScroll());}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(vertical!=0){scroll-=vertical*22;clampScroll();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(event.isUp()||event.isDown()){scroll+=event.isDown()?22:-22;clampScroll();return true;}
        return super.keyPressed(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        extractMenuBackdrop(g,delta);
        g.enableScissor(textLeft-8,top,textLeft+textWidth+12,bottom);
        int offset=(int)Math.round(scroll);
        for(var line:lines){int y=top+line.y()-offset;if(y<top-12||y>bottom)continue;g.text(font,line.text(),textLeft+line.x(),y,line.color());}
        g.disableScissor();
        int max=maxScroll();
        if(max>0){int track=bottom-top,thumb=Math.max(16,track*track/Math.max(1,contentHeight)),x=textLeft+textWidth+8,y=top+(int)((track-thumb)*(scroll/max));
            g.fill(x,top,x+3,bottom,0x40FFFFFF);g.fill(x,y,x+3,y+thumb,0xC0FFFFFF);}
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose(){
        if(automatic)WhatsNew.markSeen();
        setFocused(null);minecraft.gui.setScreen(parent);
    }
}
