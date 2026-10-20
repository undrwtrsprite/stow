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

/**
 * Release highlights in the shared Stow menu chrome: title header and menu-list panel from MemoryScreenBase,
 * one storage-card style row (rowBackdrop) per feature, the thin list scrollbar and PlannerButtons along the bottom.
 * Closing from the automatic prompt marks the version as seen.
 */
public final class WhatsNewScreen extends MemoryScreenBase {
    public static final String RELEASES="https://github.com/undrwtrsprite/stow/releases";
    private static final int TOP=44,ACCENT=0xFFFFD486,BODY=0xFFB8C6CC,HINT=0xFFAAAAAA,STEP=0xFFFFFFFF;
    private record Line(FormattedCharSequence text,int x,int y,int color){}
    private record Card(int y,int height){}
    private final List<WhatsNew.Entry> entries;
    private final boolean automatic;
    private final List<Line> lines=new ArrayList<>();
    private final List<Card> cards=new ArrayList<>();
    private int left,bodyWidth,bottom,contentHeight;
    private double scroll;
    public WhatsNewScreen(Screen parent,List<WhatsNew.Entry> entries,boolean automatic){
        super(Component.translatable("stow.whatsnew.title"),parent,null);this.entries=entries;this.automatic=automatic;
    }
    @Override protected void init(){
        layoutDialog(height);left=contentLeft();bodyWidth=contentWidth();bottom=height-38;
        int changelogW=Math.min(180,bodyWidth/2-12),gotItW=Math.min(110,bodyWidth/2-12);
        addRenderableWidget(new PlannerButton(Component.translatable("stow.whatsnew.changelog"),
            b->ConfirmLinkScreen.confirmLinkNow(this,URI.create(RELEASES)),left+8,height-30,changelogW,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("stow.whatsnew.got-it"),b->onClose(),left+bodyWidth-8-gotItW,height-30,gotItW,22,true));
        layoutText();
    }
    private void layoutText(){
        lines.clear();cards.clear();int y=0,textX=12,textWidth=bodyWidth-30;
        if(entries.isEmpty()){for(var part:font.split(Component.translatable("stow.whatsnew.empty"),textWidth)){lines.add(new Line(part,8,y,HINT));y+=10;}}
        for(var entry:entries){
            lines.add(new Line(Component.translatable("stow.whatsnew.version",entry.version()).getVisualOrderText(),8,y,ACCENT));y+=14;
            for(var section:entry.sections()){
                int cardTop=y;y+=6;
                for(var part:font.split(Component.translatable(section.title()),textWidth)){lines.add(new Line(part,textX,y,0xFFFFFFFF));y+=11;}
                for(var part:font.split(Component.translatable(section.text()),textWidth)){lines.add(new Line(part,textX,y,BODY));y+=10;}
                if(!section.how().isEmpty()){
                    y+=3;lines.add(new Line(Component.translatable("stow.whatsnew.how").getVisualOrderText(),textX,y,ACCENT));y+=11;
                    for(var step:section.how()){
                        boolean firstLine=true;
                        for(var part:font.split(Component.translatable(step),textWidth-10)){
                            if(firstLine)lines.add(new Line(Component.literal("•").getVisualOrderText(),textX,y,HINT));
                            lines.add(new Line(part,textX+10,y,STEP));y+=10;firstLine=false;
                        }
                    }
                }
                y+=5;cards.add(new Card(cardTop,y-cardTop));y+=4;
            }
            y+=8;
        }
        contentHeight=y;clampScroll();
    }
    private int viewHeight(){return bottom-TOP;}
    private int maxScroll(){return Math.max(0,contentHeight-viewHeight());}
    private void clampScroll(){scroll=Math.clamp(scroll,0,maxScroll());}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(vertical!=0){scroll+=listMotion.wheelSteps(-vertical)*20;clampScroll();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(event.isUp()||event.isDown()){scroll+=event.isDown()?20:-20;clampScroll();return true;}
        return super.keyPressed(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        extractMenuBackdrop(g,delta);
        int offset=(int)Math.round(scroll);
        g.enableScissor(left,TOP,left+bodyWidth,bottom);
        for(var card:cards){int y=TOP+card.y()-offset;if(y+card.height()<TOP||y>bottom)continue;rowBackdrop(g,left+4,y,bodyWidth-14,card.height(),ACCENT);}
        for(var line:lines){int y=TOP+line.y()-offset;if(y<TOP-12||y>bottom)continue;g.text(font,line.text(),left+line.x(),y,line.color());}
        g.disableScissor();
        if(maxScroll()>0)listScrollbar(g,offset,viewHeight(),contentHeight,TOP,bottom);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public void onClose(){
        if(automatic)WhatsNew.markSeen();
        setFocused(null);minecraft.gui.setScreen(parent);
    }
}
