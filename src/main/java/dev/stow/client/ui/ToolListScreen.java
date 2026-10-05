package dev.stow.client.ui;

import dev.stow.client.memory.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Native full-page chrome and a common searchable, downward-paginated list. */
public abstract class ToolListScreen extends MemoryScreenBase {
    protected int left,bodyWidth,page,pageSize,rowHeight,total;
    protected EditBox search;
    protected PlannerButton previous,next;
    protected final List<AbstractWidget> rowWidgets=new ArrayList<>();
    protected String initialQuery="";
    private Component searchHint;
    protected ToolListScreen(Component title,Screen parent){super(title,parent,null);}
    @Override protected int sidebarWidth(){return 0;}
    protected void begin(int rowHeight,Component hint){
        layoutDialog(height);this.rowHeight=rowHeight;searchHint=hint;left=contentLeft();bodyWidth=contentWidth();pageSize=Math.max(1,(height-126)/rowHeight);
        String query=search==null?initialQuery:search.getValue();rowWidgets.clear();
        search=addRenderableWidget(new EditBox(font,left+8,44,bodyWidth-16,22,hint));search.setHint(hint);search.setMaxLength(80);search.setValue(query);
        addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b->onClose(),left+8,height-30,66,22,false));
        previous=addRenderableWidget(new PlannerButton(Component.literal("<"),b->{page--;refresh();},left+bodyWidth-68,height-30,26,22,false));
        next=addRenderableWidget(new PlannerButton(Component.literal(">"),b->{page++;refresh();},left+bodyWidth-36,height-30,26,22,false));
        search.setResponder(s->{page=0;refresh();});
    }
    protected abstract void refresh();
    protected void resetRows(int total){
        rowWidgets.forEach(this::removeWidget);rowWidgets.clear();this.total=total;page=Math.clamp(page,0,Math.max(0,(total-1)/pageSize));
        previous.active=page>0;next.active=(page+1)*pageSize<total;
    }
    protected <T extends AbstractWidget> T row(T widget){rowWidgets.add(addRenderableWidget(widget));return widget;}
    protected int rowY(int local){return 84+local*rowHeight;}
    protected void chrome(GuiGraphicsExtractor g,float delta){
        extractMenuBackdrop(g,delta);
        g.text(font,(page+1)+" / "+Math.max(1,(total+pageSize-1)/pageSize),left+84,height-23,0xFFAAAAAA);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        super.extractRenderState(g,x,y,delta);
        if(search!=null&&search.isFocused()&&search.getValue().isEmpty())g.text(font,font.plainSubstrByWidth(searchHint.getString(),search.getWidth()-16),search.getX()+10,search.getY()+7,0xFF777777);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(vertical!=0&&x>=left&&x<left+bodyWidth&&y>=80&&y<height-38){page+=vertical<0?1:-1;refresh();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
}
