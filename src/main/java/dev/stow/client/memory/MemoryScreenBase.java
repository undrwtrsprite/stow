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
    protected MemoryScreenBase(Component title,Screen parent,ChestMemoryStore store){super(title);this.parent=parent;this.navigationStore=store;}
    protected int sidebarWidth(){return width>=640?104:0;}
    protected int contentWidth(){return Math.min(720,width-sidebarWidth()-24);}
    protected int contentLeft(){return sidebarWidth()+(width-sidebarWidth()-contentWidth())/2;}
    protected void layoutDialog(int ignoredMaximumHeight){
        dialogHeight=height;dialogTop=0;
        if(sidebarWidth()>0&&navigationStore!=null){
            for(int section=0;section<3;section++){
                final int target=section;String key=switch(section){case 0->"stow.menu.materials";case 1->"stow.menu.storage";default->"stow.menu.projects";};
                addRenderableWidget(new PlannerButton(Component.translatable(key),b->navigate(target),8,42+section*28,sidebarWidth()-18,22,false)
                    .navigation().selected(()->currentSection()==target));
            }
        }
    }
    private int currentSection(){return this instanceof ProjectsScreen?2:this instanceof ChestMemoryScreen||this instanceof ChestRenameScreen?1:0;}
    private void navigate(int section){
        if(section==currentSection()&&(this instanceof MaterialsScreen||this instanceof ChestMemoryScreen||this instanceof ProjectsScreen))return;
        Screen root=parent;while(root instanceof MemoryScreenBase utility)root=utility.parent;
        minecraft.gui.setScreen(switch(section){case 0->new MaterialsScreen(root,navigationStore);case 1->new ChestMemoryScreen(root,navigationStore,"",false,null);default->new ProjectsScreen(root,navigationStore);});
    }
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
    @Override public boolean keyPressed(KeyEvent event){if(event.isEscape()){onClose();return true;}return super.keyPressed(event);}
    @Override public void onClose(){
        setFocused(null);Screen back=parent;
        if(back==null||back instanceof AbstractContainerScreen<?> container&&minecraft.player!=null&&minecraft.player.containerMenu!=container.getMenu())back=minecraft.player==null?null:new InventoryScreen(minecraft.player);
        minecraft.gui.setScreen(back);
    }
    @Override public boolean isPauseScreen(){return false;}
}
