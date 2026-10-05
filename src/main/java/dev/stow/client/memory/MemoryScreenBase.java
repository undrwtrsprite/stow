package dev.stow.client.memory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Shared Cloth-style chrome; keep the server container open during local navigation. */
public abstract class MemoryScreenBase extends Screen {
    protected final Screen parent;
    protected final ChestMemoryStore navigationStore;
    protected int dialogHeight,dialogTop;
    protected final dev.stow.client.ui.ListMotion listMotion=new dev.stow.client.ui.ListMotion();
    private final java.util.Map<net.minecraft.client.gui.components.AbstractWidget,Integer> listWidgets=new java.util.IdentityHashMap<>();
    private int listTop,listBottom;
    private float frameOffset;
    protected int scrollIndex(double wheel,int index,int total,int visible,int columns){
        int steps=listMotion.wheelSteps(-wheel);
        return Math.clamp(index+steps*columns,0,Math.max(0,((total-visible+columns-1)/columns)*columns));
    }
    protected void trackList(java.util.Collection<? extends net.minecraft.client.gui.components.AbstractWidget> widgets,int top,int bottom,int index,int rowHeight,int columns){
        listMotion.moveTo(index,rowHeight,columns);listTop=top;listBottom=bottom;
        var original=new java.util.IdentityHashMap<>(listWidgets);listWidgets.clear();
        for(var widget:widgets)listWidgets.put(widget,original.getOrDefault(widget,widget.getY()));
    }
    protected void beginList(GuiGraphicsExtractor g){
        frameOffset=listMotion.offset();
        for(var entry:listWidgets.entrySet())entry.getKey().setY(entry.getValue()+Math.round(frameOffset));
        g.enableScissor(contentLeft(),listTop,contentLeft()+contentWidth(),listBottom);
        g.pose().pushMatrix();g.pose().translate(0,frameOffset);
    }
    protected void endList(GuiGraphicsExtractor g){g.pose().popMatrix();g.disableScissor();}
    protected String listRange(int first,int visible,int total){return total==0?"0 / 0":(first+1)+"–"+Math.min(total,first+visible)+" / "+total;}
    protected void listRangeLabel(GuiGraphicsExtractor g,int first,int visible,int total){
        String label=listRange(first,visible,total);int x=contentLeft()+142,available=contentWidth()-216;
        g.text(font,label,x+Math.max(0,(available-font.width(label))/2),height-23,0xFFAAAAAA);
    }
    protected void listScrollbar(GuiGraphicsExtractor g,int first,int visible,int total,int top,int bottom){
        if(total<=visible)return;int track=bottom-top,thumb=Math.max(12,track*visible/total);
        int y=top+(track-thumb)*first/Math.max(1,total-visible),x=contentLeft()+contentWidth()-3;
        g.fill(x,top,x+2,bottom,0xFF383D3F);g.fill(x,y,x+2,Math.min(bottom,y+thumb),0xFFA2AAAF);
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){
        var hidden=new java.util.ArrayList<net.minecraft.client.gui.components.AbstractWidget>();
        if(event.y()<listTop||event.y()>=listBottom)for(var widget:listWidgets.keySet())if(widget.visible){hidden.add(widget);widget.visible=false;}
        try{return super.mouseClicked(event,twice);}finally{for(var widget:hidden)widget.visible=true;}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        // Keep hitboxes at the interpolated positions, and clip only scrolling rows.
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget){
            if(listWidgets.containsKey(widget)){
                widget.setY(listWidgets.get(widget)+Math.round(frameOffset));
                g.enableScissor(contentLeft(),listTop,contentLeft()+contentWidth(),listBottom);
                widget.extractRenderState(g,mx,my>=listTop&&my<listBottom?my:Integer.MIN_VALUE,delta);g.disableScissor();
            }else widget.extractRenderState(g,mx,my,delta);
        }
    }
    protected MemoryScreenBase(Component title,Screen parent,ChestMemoryStore store){super(title);this.parent=parent;this.navigationStore=store;}
    protected int sidebarWidth(){return width>=640?104:0;}
    protected int contentWidth(){return Math.min(720,width-sidebarWidth()-24);}
    protected int contentLeft(){return sidebarWidth()+(width-sidebarWidth()-contentWidth())/2;}
    protected int workspaceTop(){return sidebarWidth()>0?42:62;}
    protected void layoutDialog(int ignoredMaximumHeight){
        dialogHeight=height;dialogTop=0;
        if(sidebarWidth()>0&&navigationStore!=null){
            for(int section=0;section<3;section++){
                final int target=section;String key=switch(section){case 0->"stow.menu.materials";case 1->"stow.menu.storage";default->"stow.menu.projects";};
                addRenderableWidget(new PlannerButton(Component.translatable(key),b->navigate(target),8,42+section*28,sidebarWidth()-18,22,false)
                    .navigation().selected(()->currentSection()==target));
            }
        }
        if(sidebarWidth()==0&&navigationStore!=null&&(this instanceof MaterialsScreen||this instanceof ChestMemoryScreen||this instanceof ProjectsScreen)){
            int w=(contentWidth()-24)/3;
            for(int section=0;section<3;section++){
                final int target=section;String key=switch(section){case 0->"stow.menu.materials";case 1->"stow.menu.storage";default->"stow.menu.projects";};
                addRenderableWidget(new PlannerButton(Component.translatable(key),b->navigate(target),contentLeft()+8+section*(w+4),34,w,22,false).selected(()->currentSection()==target));
            }
        }
    }
    protected void rowBackdrop(GuiGraphicsExtractor g,int x,int y,int w,int h,int accent){
        g.fill(x,y,x+w,y+h,0x18000000);g.fill(x,y,x+2,y+h,accent);g.horizontalLine(x+2,x+w-1,y+h-1,0xFF404747);
    }
    protected void pageLabel(GuiGraphicsExtractor g,int page,int total){String label=(page+1)+" / "+Math.max(1,total);int x=contentLeft()+142,available=contentWidth()-216;g.text(font,label,x+Math.max(0,(available-font.width(label))/2),height-23,0xFFAAAAAA);}
    private int currentSection(){return this instanceof ProjectsScreen?2:this instanceof ChestMemoryScreen||this instanceof ChestRenameScreen?1:0;}
    private void navigate(int section){
        if(section==currentSection()&&(this instanceof MaterialsScreen||this instanceof ChestMemoryScreen chest&&!chest.isSelectionMode()||this instanceof ProjectsScreen))return;
        Screen root=rootScreen();
        minecraft.gui.setScreen(switch(section){case 0->new MaterialsScreen(root,navigationStore);case 1->new ChestMemoryScreen(root,navigationStore,"",false,null);default->new ProjectsScreen(root,navigationStore);});
    }
    public Screen rootScreen(){Screen root=parent;while(root instanceof MemoryScreenBase utility)root=utility.parent;return root;}
    protected void extractMenuBackdrop(GuiGraphicsExtractor g,float delta){
        if(minecraft.level==null)extractPanorama(g,delta);
        extractBlurredBackground(g);extractMenuBackground(g);
        int side=sidebarWidth();
        g.blit(RenderPipelines.GUI_TEXTURED,Identifier.withDefaultNamespace("textures/gui/menu_list_background.png"),side,30,0,0,width-side,height-62,width-side,height-62,32,32,0xFF444444);
        g.horizontalLine(side,width-1,29,0xFF929292);g.horizontalLine(side,width-1,30,0xFF151515);
        g.horizontalLine(side,width-1,height-32,0xFF929292);g.horizontalLine(side,width-1,height-31,0xFF151515);
        if(side>0){g.fill(0,0,side-2,height,0xC0000000);g.verticalLine(side-2,0,height,0xFF555555);g.text(font,"stow",8,12,0xFFFFFFFF);}
        g.centeredText(font,font.plainSubstrByWidth(title.getString(),width-side-28),side+(width-side)/2,12,0xFFFFFFFF);
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){}
    @Override public boolean keyPressed(KeyEvent event){if(event.isEscape()){onClose();return true;}
        if(!(this instanceof dev.stow.client.ui.CommandPaletteScreen)&&dev.stow.client.input.StowShortcuts.matches(dev.stow.client.input.StowShortcuts.Action.PALETTE,dev.stow.client.input.StowShortcuts.chord(event)))return dev.stow.client.input.StowShortcuts.dispatch(dev.stow.client.input.StowShortcuts.Action.PALETTE,this,null);
        return super.keyPressed(event);}
    @Override public void onClose(){
        setFocused(null);Screen back=parent;
        if(back==null||back instanceof AbstractContainerScreen<?> container&&minecraft.player!=null&&minecraft.player.containerMenu!=container.getMenu())back=minecraft.player==null?null:new InventoryScreen(minecraft.player);
        minecraft.gui.setScreen(back);
    }
    @Override public boolean isPauseScreen(){return false;}
}
