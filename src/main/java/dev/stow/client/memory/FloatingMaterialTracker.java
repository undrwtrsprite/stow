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
import dev.stow.client.hud.EquipmentWatch;
import dev.stow.client.ui.EquipmentScreen;

/** Icon + combined count, deliberately without a background, border, or button sprite. */
public final class FloatingMaterialTracker extends AbstractWidget {
    private final int maxHeight;
    private final Consumer<String> open;
    private ChestMemoryStore store;
    private List<MaterialGoal> goals=List.of();
    private List<EquipmentWatch.Entry> equipment=List.of();
    private int offset,rows;
    private String projectId;
    public FloatingMaterialTracker(int x,int y,int width,int maxHeight,Consumer<String> open) {
        super(x,y,width,0,Component.translatable("stow.need.floating"));this.maxHeight=maxHeight;this.open=open;
        visible=false;
    }
    public void update(ChestMemoryStore store,boolean canOpen) {
        if(this.store!=store || !Objects.equals(projectId,store==null?null:store.activeProjectId()))offset=0;
        projectId=store==null?null:store.activeProjectId();
        this.store=store;goals=store==null?List.of():store.visibleGoals();
        var mc=Minecraft.getInstance();var warnings=EquipmentWatch.warnings(mc.player==null?null:mc.player.getInventory());
        int equipmentRows=Math.min(2,Math.max(0,(maxHeight-(goals.isEmpty()?0:34))/22));equipment=warnings.stream().limit(equipmentRows).toList();
        int available=maxHeight-equipment.size()*22;
        rows=Math.max(1,(available-(goals.size()*22>available?12:0))/22);
        offset=Math.max(0,Math.min(offset,Math.max(0,goals.size()-rows)));
        setHeight(Math.min(rows,goals.size())*22+(goals.size()>rows?12:0)+equipment.size()*22);
        visible=(!goals.isEmpty()||!equipment.isEmpty()) && width>=38 && maxHeight>=22;active=canOpen;
    }
    public List<MaterialGoal> displayedGoals(){return goals.subList(offset,Math.min(goals.size(),offset+rows));}
    public String countLabel(MaterialGoal goal){var p=MaterialPlanner.progress(store,goal);return p.total()+" / "+goal.target();}
    @Override protected void extractWidgetRenderState(GuiGraphicsExtractor graphics,int mx,int my,float delta) {
        var font=Minecraft.getInstance().font;
        List<MaterialGoal> shown=displayedGoals();
        for(int i=0;i<shown.size();i++) {
            MaterialGoal goal=shown.get(i);var progress=MaterialPlanner.progress(store,goal);int y=getY()+i*22;
            graphics.item(MemoryItems.icon(goal.itemId()),getX(),y+2);
            String count=progress.total()+" / "+goal.target();
            int color=progress.enough()?0xFF8DE8B2:0xFFFFFFFF;
            float scale=Math.min(1,(width-23)/(float)Math.max(1,font.width(count)));
            if(scale<0.8f){
                // Preserve exact amounts and readable text in a narrow right margin.
                drawCount(graphics,Long.toString(progress.total()),y+2,color);
                drawCount(graphics,"/ "+goal.target(),y+12,color);
            }else{
                graphics.pose().pushMatrix();graphics.pose().translate(getX()+22,y+(20-9*scale)/2);graphics.pose().scale(scale,scale);
                graphics.text(font,count,0,0,color);graphics.pose().popMatrix();
            }
            if(mx>=getX() && mx<getRight() && my>=y && my<y+22) {
                long oldest=store.oldestSourceTime(goal.itemId());
                List<Component> lines=new ArrayList<>();lines.add(Component.literal(MemoryItems.name(goal.itemId())));
                lines.add(Component.translatable("stow.need.hover-counts",progress.inventory(),progress.chests()));
                if(oldest>0)lines.add(Component.translatable("stow.need.oldest",ChestMemoryStore.age(oldest)));
                lines.add(Component.translatable("stow.need.floating-hint"));
                PlannerTooltips.show(graphics,mx,my,lines.toArray(Component[]::new));
            }
        }
        if(goals.size()>rows)graphics.text(font,(offset+1)+"–"+Math.min(goals.size(),offset+rows)+" / "+goals.size(),getX(),getY()+shown.size()*22+2,0xFFBDC6CC);
        int equipmentTop=getY()+shown.size()*22+(goals.size()>rows?12:0);
        for(int i=0;i<equipment.size();i++){
            var item=equipment.get(i);int y=equipmentTop+i*22;graphics.item(item.item(),getX(),y+2);
            drawCount(graphics,item.percent()+"% · "+item.remaining(),y+6,item.color());
            if(mx>=getX()&&mx<getRight()&&my>=y&&my<y+22)PlannerTooltips.show(graphics,mx,my,item.item().getHoverName(),Component.literal(item.remaining()+" / "+item.maximum()));
        }
    }
    private void drawCount(GuiGraphicsExtractor graphics,String text,int y,int color){
        var font=Minecraft.getInstance().font;float scale=Math.min(1,(width-23)/(float)Math.max(1,font.width(text)));
        graphics.pose().pushMatrix();graphics.pose().translate(getX()+22,y);graphics.pose().scale(scale,scale);graphics.text(font,text,0,0,color);graphics.pose().popMatrix();
    }
    @Override protected boolean isValidClickButton(net.minecraft.client.input.MouseButtonInfo info){int button=info.button();return button==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT || button==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;}
    @Override public void onClick(MouseButtonEvent event,boolean doubleClick) {
        int equipmentTop=getY()+displayedGoals().size()*22+(goals.size()>rows?12:0);
        if(!equipment.isEmpty()&&event.y()>=equipmentTop){var mc=Minecraft.getInstance();mc.gui.setScreen(new EquipmentScreen(mc.gui.screen()));return;}
        int index=(int)(event.y()-getY())/22+offset;
        if(index>=offset && index<Math.min(goals.size(),offset+rows)){String id=goals.get(index).itemId();if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT)ChestMemory.findNearest(store,id);else open.accept(id);}
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(!isMouseOver(mx,my) || !visible || vertical==0)return false;
        offset=Math.max(0,Math.min(Math.max(0,goals.size()-rows),offset+(vertical<0?1:-1)));return true;
    }
    @Override protected void updateWidgetNarration(NarrationElementOutput output){defaultButtonNarrationText(output);}
}
