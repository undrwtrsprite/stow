package dev.stow.client.memory;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import static dev.stow.client.memory.ChestMemoryStore.*;

/** Compact planner: choose materials, edit quantities in place, choose storage separately. */
public final class MaterialsScreen extends MemoryScreenBase {
    private final ChestMemoryStore store;
    private final String focusItem;
    private boolean appliedFocus;
    private List<MaterialGoal> goals=List.of();
    private final List<AbstractWidget> widgets=new ArrayList<>();
    private final List<GoalRow> rows=new ArrayList<>();
    private record GoalRow(MaterialGoal goal,EditBox amount,PlannerButton save,PlannerButton visibility) {}
    private int page,pageSize,left,bodyWidth;
    private PlannerButton previous,next,chests,project,undo;
    private String projectId;
    public MaterialsScreen(Screen parent,ChestMemoryStore store){this(parent,store,null);}
    public MaterialsScreen(Screen parent,ChestMemoryStore store,String focusItem){super(Component.translatable("stow.need.title"),parent,store);this.store=store;this.focusItem=focusItem;}
    @Override protected void init() {
        layoutDialog(480);
        bodyWidth=contentWidth();left=contentLeft();pageSize=Math.max(1,(dialogHeight-142)/56);
        projectId=store==null?null:store.activeProjectId();
        project=addRenderableWidget(new PlannerButton(Component.empty(),b -> minecraft.gui.setScreen(new ProjectsScreen(this,store)),left+8,46,bodyWidth-90,22,false));
        undo=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.undo"),b -> {store.undo();ChestMemory.saveLater(store);refresh();},left+bodyWidth-76,46,68,22,false));
        var add=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.add"),b -> {
            minecraft.gui.setScreen(new MaterialPickerScreen(this,store,"",1000));
        },left+8,74,112,22,true));add.active=store!=null;
        chests=addRenderableWidget(new PlannerButton(Component.empty(),b -> minecraft.gui.setScreen(new ChestMemoryScreen(this,store,"",true,null)),left+126,74,bodyWidth-134,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b -> onClose(),left+8,dialogHeight-30,66,22,false));
        previous=addRenderableWidget(new PlannerButton(Component.literal("<"),b -> {page--;refresh();},left+bodyWidth-68,dialogHeight-30,26,22,false));
        next=addRenderableWidget(new PlannerButton(Component.literal(">"),b -> {page++;refresh();},left+bodyWidth-36,dialogHeight-30,26,22,false));
        refresh();
    }
    private static int parseAmount(String value){try{int n=Integer.parseInt(value);return n>0 && n<=1_000_000?n:0;}catch(NumberFormatException e){return 0;}}
    private void refresh() {
        Map<String,GoalRow> oldRows=new HashMap<>();for(GoalRow row:rows)oldRows.put(row.goal.itemId(),row);
        if(store!=null && !Objects.equals(projectId,store.activeProjectId())){page=0;oldRows.clear();projectId=store.activeProjectId();}
        goals=store==null?List.of():store.goals();
        if(!appliedFocus && focusItem!=null){for(int i=0;i<goals.size();i++)if(goals.get(i).itemId().equals(focusItem))page=i/pageSize;appliedFocus=true;}
        page=Math.max(0,Math.min(page,Math.max(0,(goals.size()-1)/pageSize)));
        widgets.forEach(this::removeWidget);widgets.clear();rows.clear();
        for(int i=page*pageSize;i<Math.min(goals.size(),(page+1)*pageSize);i++) {
            MaterialGoal goal=goals.get(i);int y=108+(i-page*pageSize)*56,x=left+8;
            EditBox amount=new EditBox(font,x+38,y+32,56,18,Component.translatable("stow.need.amount"));amount.setMaxLength(7);
            GoalRow old=oldRows.get(goal.itemId());amount.setValue(old!=null && old.goal.target()==goal.target()?old.amount.getValue():Integer.toString(goal.target()));
            PlannerButton save=new PlannerButton(Component.literal("OK"),b -> saveTarget(goal,amount),x+98,y+32,22,18,false);
            save.setTooltip(Tooltip.create(Component.translatable("stow.need.save-target")));
            amount.setResponder(value -> {save.active=parseAmount(value)>0 && parseAmount(value)!=goal.target();amount.setTextColor(parseAmount(value)>0?0xFFE9F0F3:0xFFFFAA91);amount.setTooltip(parseAmount(value)>0?null:Tooltip.create(Component.translatable("stow.need.amount-range")));});
            save.active=parseAmount(amount.getValue())>0 && parseAmount(amount.getValue())!=goal.target();
            PlannerButton sources=new PlannerButton(Component.translatable("stow.need.sources"),b -> minecraft.gui.setScreen(new ChestMemoryScreen(this,store,"",true,goal.itemId())),x+126,y+32,58,18,false);
            sources.setTooltip(Tooltip.create(Component.translatable("stow.need.sources-hint")));
            PlannerButton show=new PlannerButton(Component.empty(),b -> {store.toggleGoalVisible(goal.itemId());ChestMemory.saveLater(store);syncButtons();},x+190,y+32,58,18,false);
            show.setTooltip(Tooltip.create(Component.translatable("stow.need.visibility-hint")));
            PlannerButton remove=new PlannerButton(Component.literal("×"),b -> {store.removeGoal(goal.itemId());ChestMemory.saveLater(store);refresh();},left+bodyWidth-28,y+32,20,18,false);
            remove.setTooltip(Tooltip.create(Component.translatable("stow.need.remove")));
            for(AbstractWidget widget:List.of(amount,save,sources,show,remove))widgets.add(addRenderableWidget(widget));
            rows.add(new GoalRow(goal,amount,save,show));
        }
        previous.active=page>0;next.active=(page+1)*pageSize<goals.size();syncButtons();
    }
    private void syncButtons() {
        project.active=store!=null;project.setMessage(Component.translatable("stow.need.project-current",store==null?"":store.projectName()));
        undo.active=store!=null && store.canUndo();undo.setTooltip(PlannerTooltips.undo(store));
        chests.active=store!=null;
        chests.setMessage(Component.translatable("stow.need.storage-count",store==null?0:store.includedChestCount()));
        for(GoalRow row:rows)row.visibility.setMessage(Component.translatable(store.isGoalVisible(row.goal.itemId())?"stow.need.hide-item":"stow.need.show-item"));
    }
    private void saveTarget(MaterialGoal goal,EditBox amount) {
        int target=parseAmount(amount.getValue());if(target==0)return;
        store.updateTarget(goal.itemId(),target);ChestMemory.saveLater(store);refresh();
    }
    @Override public void tick(){if(store!=null && (!store.goals().equals(goals) || !Objects.equals(projectId,store.activeProjectId())))refresh();else syncButtons();}
    @Override public boolean keyPressed(KeyEvent event) {
        if(event.key()==InputConstants.KEY_RETURN || event.key()==InputConstants.KEY_NUMPADENTER)for(GoalRow row:rows)if(row.amount.isFocused()){saveTarget(row.goal,row.amount);return true;}
        return super.keyPressed(event);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(vertical!=0 && mx>=left && mx<left+bodyWidth && my-dialogTop>=108 && my-dialogTop<dialogHeight-38){page+=vertical<0?1:-1;refresh();return true;}
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mx,int my,float delta) {
        extractMenuBackdrop(graphics,delta);graphics.pose().pushMatrix();graphics.pose().translate(0,dialogTop);
        
        
        graphics.text(font,Component.translatable("stow.need.menu-subtitle"),left+8,32,0xFFAAAAAA);
        String shown=Component.translatable("stow.need.visible-count",store==null?0:store.visibleGoals().size()).getString();
        if(font.width(title)+font.width(shown)+28<bodyWidth)graphics.text(font,shown,left+bodyWidth-font.width(shown)-8,16,0xFFCCCCCC);
        for(int i=0;i<rows.size();i++) {
            var row=rows.get(i);var goal=row.goal;int y=108+i*56;
            var progress=MaterialPlanner.progress(store,goal);boolean visible=store.isGoalVisible(goal.itemId());
            graphics.fill(left+4,y-3,left+bodyWidth-4,y+53,0x18000000);
            graphics.fill(left+4,y-3,left+6,y+53,visible?0xFFFFFFFF:0xFF808080);
            graphics.item(MemoryItems.icon(goal.itemId()),left+12,y+2);
            String count=progress.total()+" / "+goal.target();int countWidth=font.width(count);
            graphics.text(font,font.plainSubstrByWidth(MemoryItems.name(goal.itemId()),Math.max(20,bodyWidth-countWidth-54)),left+34,y+2,0xFFFFFFFF);
            graphics.text(font,count,left+bodyWidth-countWidth-10,y+2,progress.enough()?0xFF8DE8B2:0xFFFFFFFF);
            String missing=Component.translatable(progress.enough()?"stow.need.ready":"stow.need.missing",progress.missing()).getString();
            graphics.text(font,missing,left+bodyWidth-font.width(missing)-10,y+17,progress.enough()?0xFF8DE8B2:0xFFEDBF8D);
            String parts=Component.translatable("stow.need.count-parts",progress.inventory(),progress.chests()).getString();
            graphics.text(font,font.plainSubstrByWidth(parts,Math.max(20,bodyWidth-font.width(missing)-28)),left+12,y+17,0xFFAAAAAA);
            graphics.text(font,Component.translatable("stow.need.target-label"),left+12,y+37,0xFFAAAAAA);
            int nameWidth=Math.max(20,bodyWidth-countWidth-54);
            if(mx>=left+12 && mx<left+34+nameWidth && my-dialogTop>=y && my-dialogTop<y+12 && font.width(MemoryItems.name(goal.itemId()))>nameWidth)
                PlannerTooltips.show(graphics,mx,my,Component.literal(MemoryItems.name(goal.itemId())));
            if(mx>=left+bodyWidth-countWidth-10 && mx<left+bodyWidth-8 && my-dialogTop>=y && my-dialogTop<y+12){long oldest=store.oldestSourceTime(goal.itemId());if(oldest>0)PlannerTooltips.show(graphics,mx,my,Component.translatable("stow.need.oldest",ChestMemoryStore.age(oldest)));}
        }
        if(goals.isEmpty()) {
            graphics.text(font,Component.translatable("stow.need.empty-title"),left+12,120,0xFFFFFFFF);
            graphics.textWithWordWrap(font,Component.translatable("stow.need.empty-help"),left+12,140,bodyWidth-24,0xFFAAAAAA);
        }
        String pages=Component.translatable("stow.need.page",Math.min(goals.size(),page*pageSize+1),Math.min(goals.size(),(page+1)*pageSize),goals.size()).getString();
        if(goals.isEmpty())pages="";
        graphics.text(font,pages,left+82,dialogHeight-23,0xFFAAAAAA);
        graphics.pose().popMatrix();super.extractRenderState(graphics,mx,my,delta);
    }
}
