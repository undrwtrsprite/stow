package dev.stow.client.memory;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import static dev.stow.client.memory.ChestMemoryStore.*;

/** Icon + combined count, deliberately without a background, border, or button sprite. */
public final class FloatingMaterialTracker extends AbstractWidget {
    private int maxHeight;
    private final Consumer<String> open;
    private ChestMemoryStore store;
    private List<MaterialGoal> goals=List.of();
    private int offset,rows;
    private final dev.stow.client.ui.ListMotion motion=new dev.stow.client.ui.ListMotion();
    private float renderOffset;
    private String hoveredGoal;
    public boolean toggleHoveredHud(net.minecraft.client.input.KeyEvent event){if(!active||hoveredGoal==null||!dev.stow.client.input.StowShortcuts.matches(dev.stow.client.input.StowShortcuts.Action.HUD_GOAL,dev.stow.client.input.StowShortcuts.chord(event)))return false;MaterialPlanner.toggleHud(store,hoveredGoal);return true;}
    private String projectId;
    public FloatingMaterialTracker(int x,int y,int width,int maxHeight,Consumer<String> open) {
        super(x,y,width,0,Component.translatable("stow.need.floating"));this.maxHeight=maxHeight;this.open=open;
        visible=false;
    }
    public void place(int x,int y,int width,int maxHeight){setX(x);setY(y);setWidth(width);this.maxHeight=maxHeight;}
    public void update(ChestMemoryStore store,boolean canOpen) {
        if(this.store!=store || !Objects.equals(projectId,store==null?null:store.activeProjectId())){offset=dev.stow.Stow.config.rememberMaterialPage&&store!=null?store.materialPage():0;motion.reset(offset);}hoveredGoal=null;
        projectId=store==null?null:store.activeProjectId();
        this.store=store;goals=store==null?List.of():store.visibleGoals();
        int available=maxHeight;
        rows=Math.max(1,(available-(goals.size()*22>available?12:0))/22);
        offset=Math.max(0,Math.min(offset,Math.max(0,goals.size()-rows)));
        setHeight(Math.min(rows,goals.size())*22+(goals.size()>rows?12:0));
        visible=!goals.isEmpty() && width>=38 && maxHeight>=22;active=canOpen;
    }
    public List<MaterialGoal> displayedGoals(){return goals.subList(offset,Math.min(goals.size(),offset+rows));}
    public String countLabel(MaterialGoal goal){var p=MaterialPlanner.progress(store,goal);return p.label();}
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,int mx,int my,float delta) {
        var font=Minecraft.getInstance().font;
        List<MaterialGoal> shown=displayedGoals();hoveredGoal=null;
        renderOffset=motion.offset();
        graphics.enableScissor(getX(),getY(),getRight(),getY()+shown.size()*22);
        graphics.pose().pushMatrix();graphics.pose().translate(0,renderOffset);
        for(int i=0;i<shown.size();i++) {
            MaterialGoal goal=shown.get(i);var progress=MaterialPlanner.progress(store,goal);int y=getY()+i*22;
            graphics.item(MemoryItems.icon(goal.itemId()),getX(),y+2);
            if(store.isHudGoal(goal.itemId(),dev.stow.Stow.config.dockRows))dev.stow.client.inventory.SlotMarkers.star(graphics,getX()+12,y+1,0xFFFFD486);
            String count=progress.label();
            int color=progress.color();
            float scale=Math.min(1,(width-23)/(float)Math.max(1,font.width(count)));
            if(scale<0.8f){
                // Preserve exact amounts and readable text in a narrow right margin.
                drawCount(graphics,Long.toString(progress.counted()),y+2,color);
                drawCount(graphics,"/ "+goal.target(),y+12,color);
            }else{
                graphics.pose().pushMatrix();graphics.pose().translate(getX()+22,y+(20-9*scale)/2);graphics.pose().scale(scale,scale);
                graphics.text(font,count,0,0,color);graphics.pose().popMatrix();
            }
            if(mx>=getX() && mx<getRight() && my>=getY() && my<getY()+shown.size()*22 && my-renderOffset>=y && my-renderOffset<y+22) {
                hoveredGoal=goal.itemId();
                long oldest=store.oldestSourceTime(goal.itemId());
                List<Component> lines=new ArrayList<>();lines.add(Component.literal(MemoryItems.name(goal.itemId())).withStyle(net.minecraft.ChatFormatting.BOLD));
                lines.add(Component.translatable("stow.need.hover-counts",progress.inventory(),progress.chests()));
                if(oldest>0&&System.currentTimeMillis()-oldest>=300000)lines.add(Component.translatable("stow.need.oldest",ChestMemoryStore.age(oldest)));
                var binding=dev.stow.client.input.StowShortcuts.binding(dev.stow.client.input.StowShortcuts.Action.HUD_GOAL);
                boolean glowing=ChestMemory.isMaterialGlowing(goal.itemId());
                lines.add(binding.code()<0?Component.translatable(glowing?"stow.need.floating-glow-off-hint":"stow.need.floating-hint"):Component.translatable(glowing?"stow.need.floating-glow-off-controls":"stow.need.floating-controls",dev.stow.client.input.StowShortcuts.label(binding)));
                PlannerTooltips.show(graphics,mx,my,lines.toArray(Component[]::new));
            }
        }
        graphics.pose().popMatrix();graphics.disableScissor();
        if(goals.size()>rows)graphics.text(font,(offset+1)+"–"+Math.min(goals.size(),offset+rows)+" / "+goals.size(),getX(),getY()+shown.size()*22+2,0xFFBDC6CC);
    }
    private void drawCount(GuiGraphicsExtractor graphics,String text,int y,int color){
        var font=Minecraft.getInstance().font;float scale=Math.min(1,(width-23)/(float)Math.max(1,font.width(text)));
        graphics.pose().pushMatrix();graphics.pose().translate(getX()+22,y);graphics.pose().scale(scale,scale);graphics.text(font,text,0,0,color);graphics.pose().popMatrix();
    }
    @Override protected boolean isValidClickButton(net.minecraft.client.input.MouseButtonInfo info){int button=info.button();return dev.stow.client.input.StowShortcuts.matches(dev.stow.client.input.StowShortcuts.Action.HUD_GOAL,dev.stow.client.input.StowShortcuts.chord(info))||button==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT || button==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;}
    @Override public void onClick(MouseButtonEvent event,boolean doubleClick) {
        if(event.y()-getY()-renderOffset<0)return;
        int index=(int)(event.y()-getY()-renderOffset)/22+offset;
        if(index>=offset && index<Math.min(goals.size(),offset+rows)){String id=goals.get(index).itemId();if(dev.stow.client.input.StowShortcuts.matches(dev.stow.client.input.StowShortcuts.Action.HUD_GOAL,dev.stow.client.input.StowShortcuts.chord(event.buttonInfo()))){MaterialPlanner.toggleHud(store,id);return;}if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT)ChestMemory.toggleMaterialGlow(store,id);else open.accept(id);}
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(!isMouseOver(mx,my) || !visible || vertical==0)return false;
        offset=Math.max(0,Math.min(Math.max(0,goals.size()-rows),offset+motion.wheelSteps(-vertical)));motion.moveTo(offset,22,1);if(store!=null)store.setMaterialPage(offset);return true;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
}
