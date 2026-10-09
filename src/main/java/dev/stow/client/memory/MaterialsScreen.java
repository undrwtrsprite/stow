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
    private static final int ROW_HEIGHT=42;
    private final ChestMemoryStore store;
    private final String focusItem;
    private boolean appliedFocus;
    private List<MaterialGoal> goals=List.of();
    private final List<AbstractWidget> widgets=new ArrayList<>();
    private final List<GoalRow> rows=new ArrayList<>();
    private final Map<String,String> drafts=new HashMap<>();
    private record GoalRow(MaterialGoal goal,EditBox amount,PlannerButton save,PlannerButton visibility,InventoryIconButton hud) {}
    private int page,pageSize,left,bodyWidth,startY,toolbarY;
    private EditBox search;
    private List<MaterialGoal> allGoals=List.of();
    private PlannerButton previous,next,chests,project,undo,copy;
    private String projectId;
    private long revision=-1;
    private Component importNotice;
    private String importNoticeProject;
    private int importedTypes;
    private String resizeFocus;
    @Override protected void repositionElements(){
        var focus=rows.stream().filter(r->r.amount.isFocused()).findFirst().orElse(null);resizeFocus=focus==null?null:focus.goal.itemId();
        boolean searching=search!=null&&search.isFocused();int cursor=focus!=null?focus.amount.getCursorPosition():searching?search.getCursorPosition():0;
        super.repositionElements();
        if(resizeFocus!=null){for(var row:rows)if(row.goal.itemId().equals(resizeFocus)){setFocused(row.amount);row.amount.moveCursorTo(Math.min(cursor,row.amount.getValue().length()),false);break;}}
        else if(searching){setFocused(search);search.moveCursorTo(Math.min(cursor,search.getValue().length()),false);}
        resizeFocus=null;
    }
    public MaterialsScreen(Screen parent,ChestMemoryStore store){this(parent,store,null);}
    public MaterialsScreen(Screen parent,ChestMemoryStore store,String focusItem){super(Component.translatable("stow.need.title"),parent,store);this.store=store;this.focusItem=focusItem;}
    public MaterialsScreen(Screen parent,ChestMemoryStore store,String focusItem,int importedTypes){this(parent,store,focusItem);this.importedTypes=importedTypes;importNotice=Component.translatable("stow.import.saved",importedTypes);importNoticeProject=store.activeProjectId();}
    @Override protected void init() {
        layoutDialog(480);
        bodyWidth=contentWidth();left=contentLeft();toolbarY=workspaceTop();startY=toolbarY+48;pageSize=Math.max(1,(dialogHeight-startY-38)/ROW_HEIGHT);
        project=addRenderableWidget(new PlannerButton(Component.empty(),b -> minecraft.gui.setScreen(new ProjectsScreen(this,store)),left+8,toolbarY,bodyWidth-166,22,false));
        copy=addRenderableWidget(new PlannerButton(Component.translatable("stow.export.copy"),b->{
            minecraft.keyboardHandler.setClipboard(MaterialListExporter.export(store.projectName(),store.goals()));
            dev.stow.client.ui.UiNotifications.show("stow.export.copied",store.goals().size());
        },left+bodyWidth-152,toolbarY,28,22,false).icon(dev.stow.client.ui.UIIcons.Kind.COPY));
        copy.setTooltip(Tooltip.create(Component.translatable("stow.export.hint")));
        undo=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.undo"),b -> {store.undo();dev.stow.client.ui.UiNotifications.show("stow.feedback.undo");importNotice=null;ChestMemory.saveLater(store);refresh();},left+80,dialogHeight-30,56,22,false));
        var add=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.add"),b -> {
            minecraft.gui.setScreen(new MaterialPickerScreen(this,store,"",1000));
        },left+bodyWidth-120,toolbarY,28,22,true).icon(dev.stow.client.ui.UIIcons.Kind.PLUS));add.active=store!=null;add.setTooltip(Tooltip.create(Component.translatable("stow.need.add")));
        addRenderableWidget(new PlannerButton(Component.translatable("stow.import.open"),b->minecraft.gui.setScreen(new MaterialImportScreen(this,store)),left+bodyWidth-88,toolbarY,80,22,false)).active=store!=null;
        chests=addRenderableWidget(new PlannerButton(Component.empty(),b -> minecraft.gui.setScreen(new ChestMemoryScreen(this,store,"",true,null)),left+bodyWidth-120,toolbarY+24,112,22,false));
        String query=search==null?"":search.getValue();search=addRenderableWidget(new EditBox(font,left+8,toolbarY+24,bodyWidth-134,22,Component.translatable("stow.need.search")));search.setMaxLength(80);search.setHint(Component.translatable("stow.need.search"));search.setValue(query);search.setResponder(v->{page=0;refresh();});
        addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b -> onClose(),left+8,dialogHeight-30,66,22,false));
        previous=addRenderableWidget(new PlannerButton(Component.literal("<"),b -> {page-=pageSize;refresh();},left+bodyWidth-68,dialogHeight-30,26,22,false));
        next=addRenderableWidget(new PlannerButton(Component.literal(">"),b -> {page+=pageSize;refresh();},left+bodyWidth-36,dialogHeight-30,26,22,false));
        refresh();
    }
    private static int parseAmount(String value){try{int n=Integer.parseInt(value);return n>0 && n<=1_000_000?n:0;}catch(NumberFormatException e){return 0;}}
    private void refresh() {
        GoalRow focused=rows.stream().filter(r->r.amount.isFocused()).findFirst().orElse(null);
        int cursor=focused==null?0:focused.amount.getCursorPosition();
        if(store!=null && !Objects.equals(projectId,store.activeProjectId())){page=0;drafts.clear();focused=null;projectId=store.activeProjectId();}
        allGoals=store==null?List.of():store.goals();
        drafts.keySet().removeIf(id->allGoals.stream().noneMatch(g->g.itemId().equals(id)));
        goals=allGoals.stream().filter(g->search.getValue().isBlank()||ChestMemoryStore.matches(new MemoryItem(g.itemId(),MemoryItems.name(g.itemId()),1),search.getValue())).toList();
        if(resizeFocus!=null)for(int i=0;i<goals.size();i++)if(goals.get(i).itemId().equals(resizeFocus)){page=(i/pageSize)*pageSize;break;}
        if(!appliedFocus && focusItem!=null){for(int i=0;i<goals.size();i++)if(goals.get(i).itemId().equals(focusItem))page=(i/pageSize)*pageSize;appliedFocus=true;}
        page=Math.max(0,Math.min(page,Math.max(0,goals.size()-pageSize)));
        widgets.forEach(this::removeWidget);widgets.clear();rows.clear();
        for(int i=page;i<Math.min(goals.size(),(page+pageSize));i++) {
            MaterialGoal goal=goals.get(i);int y=startY+(i-page)*ROW_HEIGHT,x=left+8;
            EditBox amount=new EditBox(font,x+38,y+18,56,20,Component.translatable("stow.need.amount"));amount.setMaxLength(7);
            amount.setValue(drafts.getOrDefault(goal.itemId(),Integer.toString(goal.target())));
            PlannerButton save=new PlannerButton(Component.translatable("stow.need.confirm-save"),b -> saveTarget(goal,amount),x+98,y+18,22,20,false).icon(dev.stow.client.ui.UIIcons.Kind.CHECK);
            amount.setResponder(value -> {drafts.put(goal.itemId(),value);save.active=parseAmount(value)>0 && parseAmount(value)!=goal.target();amount.setTextColor(parseAmount(value)>0?0xFFE9F0F3:0xFFFFAA91);amount.setTooltip(parseAmount(value)>0?null:Tooltip.create(Component.translatable("stow.need.amount-range")));});
            amount.setTextColor(parseAmount(amount.getValue())>0?0xFFE9F0F3:0xFFFFAA91);
            save.setTooltip(Tooltip.create(Component.translatable("stow.need.save-target")));
            save.active=parseAmount(amount.getValue())>0 && parseAmount(amount.getValue())!=goal.target();
            PlannerButton sources=new PlannerButton(Component.translatable("stow.need.sources"),b -> minecraft.gui.setScreen(new ChestMemoryScreen(this,store,"",true,goal.itemId())),left+bodyWidth-100,y+18,20,20,false).icon(dev.stow.client.ui.UIIcons.Kind.STORAGE);
            sources.setTooltip(Tooltip.create(Component.translatable("stow.need.sources-hint")));
            PlannerButton show=new PlannerButton(Component.empty(),b -> {store.toggleGoalVisible(goal.itemId());dev.stow.client.ui.UiNotifications.show(store.isGoalVisible(goal.itemId())?"stow.feedback.material-shown":"stow.feedback.material-hidden");ChestMemory.saveLater(store);syncButtons();},left+bodyWidth-76,y+18,20,20,false);
            var hud=new InventoryIconButton(InventoryIconButton.Kind.HUD,Component.translatable("stow.hud.toggle"),b->{MaterialPlanner.toggleHud(store,goal.itemId());syncButtons();},left+bodyWidth-52,y+18);
            hud.setTooltip(Tooltip.create(Component.translatable("stow.hud.toggle")));
            PlannerButton remove=new PlannerButton(Component.literal("×"),b -> {store.removeGoal(goal.itemId());dev.stow.client.ui.UiNotifications.show("stow.feedback.material-removed");ChestMemory.saveLater(store);refresh();},left+bodyWidth-28,y+18,20,20,false).icon(dev.stow.client.ui.UIIcons.Kind.CLOSE);
            remove.setTooltip(Tooltip.create(Component.translatable("stow.need.remove")));
            for(AbstractWidget widget:List.of(amount,save,sources,show,remove,hud))widgets.add(addRenderableWidget(widget));
            rows.add(new GoalRow(goal,amount,save,show,hud));
            if(focused!=null&&focused.goal.itemId().equals(goal.itemId())){setFocused(amount);amount.moveCursorTo(Math.min(cursor,amount.getValue().length()),false);}
        }
        trackList(widgets,startY,dialogHeight-38,page,ROW_HEIGHT,1);
        previous.active=page>0;next.active=(page+pageSize)<goals.size();syncButtons();
        revision=store==null?0:store.revision();
    }
    private void syncButtons() {
        copy.active=store!=null&&!allGoals.isEmpty();
        project.active=store!=null;project.setMessage(Component.translatable("stow.need.project-current",store==null?"":store.projectName()));
        boolean imported=importNotice!=null&&store!=null&&Objects.equals(importNoticeProject,store.activeProjectId());
        project.setTooltip(Tooltip.create(imported?Component.translatable("stow.import.done",importedTypes):Component.translatable("stow.materials.summary",allGoals.size(),store==null?0:store.starredGoals(Integer.MAX_VALUE).size())));
        undo.active=store!=null && store.canUndo();undo.setTooltip(PlannerTooltips.undo(store));
        chests.active=store!=null;
        chests.setMessage(Component.translatable("stow.need.storage-count",store==null?0:store.includedChestCount()));
        for(GoalRow row:rows){row.visibility.setMessage(Component.translatable(store.isGoalVisible(row.goal.itemId())?"stow.need.hide-item":"stow.need.show-item"));row.visibility.icon(store.isGoalVisible(row.goal.itemId())?dev.stow.client.ui.UIIcons.Kind.EYE:dev.stow.client.ui.UIIcons.Kind.EYE_OFF);row.visibility.setTooltip(Tooltip.create(Component.translatable(store.isGoalVisible(row.goal.itemId())?"stow.need.hide-tooltip":"stow.need.show-tooltip")));boolean selected=store.isHudGoal(row.goal.itemId(),dev.stow.Stow.config.dockRows);row.hud.setHudSelected(selected);row.hud.active=selected||store.starredGoals(Integer.MAX_VALUE).size()<dev.stow.Stow.config.dockRows;row.hud.setTooltip(Tooltip.create(Component.translatable(row.hud.active?(selected?"stow.hud.remove":"stow.hud.toggle"):"stow.hud.full",dev.stow.Stow.config.dockRows)));}
    }
    private void saveTarget(MaterialGoal goal,EditBox amount) {
        int target=parseAmount(amount.getValue());if(target==0)return;
        drafts.remove(goal.itemId());store.updateTarget(goal.itemId(),target);dev.stow.client.ui.UiNotifications.show("stow.feedback.material-saved",target,MemoryItems.name(goal.itemId()));ChestMemory.saveLater(store);refresh();
    }
    @Override public void tick(){if(store!=null && (store.revision()!=revision || !store.goals().equals(allGoals) || !Objects.equals(projectId,store.activeProjectId())))refresh();else syncButtons();}
    @Override public boolean keyPressed(KeyEvent event) {
        if(event.key()==InputConstants.KEY_RETURN || event.key()==InputConstants.KEY_NUMPADENTER)for(GoalRow row:rows)if(row.amount.isFocused()){saveTarget(row.goal,row.amount);return true;}
        return super.keyPressed(event);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(vertical!=0 && mx>=left && mx<left+bodyWidth && my-dialogTop>=startY && my-dialogTop<dialogHeight-38){page=scrollIndex(vertical,page,goals.size(),pageSize,1);refresh();return true;}
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mx,int my,float delta) {
        extractMenuBackdrop(graphics,delta);graphics.pose().pushMatrix();graphics.pose().translate(0,dialogTop);
        
        
        beginList(graphics);int listMouseY=my-Math.round(listMotion.offset());
        for(int i=0;i<rows.size();i++) {
            var row=rows.get(i);var goal=row.goal;int y=startY+i*ROW_HEIGHT;
            var progress=MaterialPlanner.progress(store,goal);boolean visible=store.isGoalVisible(goal.itemId());
            rowBackdrop(graphics,left+4,y,bodyWidth-8,ROW_HEIGHT-2,visible?0xFFFFFFFF:0xFF808080);
            graphics.item(MemoryItems.icon(goal.itemId()),left+12,y);
            String count=progress.label();int countWidth=font.width(count),nameWidth=Math.max(20,bodyWidth-countWidth-54);
            graphics.text(font,font.plainSubstrByWidth(MemoryItems.name(goal.itemId()),nameWidth),left+34,y+4,0xFFFFFFFF);
            graphics.text(font,count,left+bodyWidth-countWidth-12,y+4,progress.color());
            graphics.text(font,Component.translatable("stow.need.target-label"),left+12,y+24,0xFFAAAAAA);
            String missing=Component.translatable(progress.enough()?"stow.need.ready":"stow.need.missing",progress.missing()).getString();
            int detailLeft=left+140,detailRight=left+bodyWidth-110;
            if(font.width(missing)<=detailRight-detailLeft) {
                graphics.text(font,missing,detailRight-font.width(missing),y+24,progress.enough()?0xFF8DE8B2:0xFFEDBF8D);
                var parts=Component.translatable("stow.need.count-parts",progress.inventory(),progress.chests());
                int space=detailRight-detailLeft-font.width(missing)-10;
                if(font.width(parts)<=space)graphics.text(font,parts,detailLeft,y+24,0xFFAAAAAA);
            }
            graphics.fill(left+12,y+39,left+bodyWidth-12,y+40,0xFF444A4A);
            int filled=(int)Math.min(bodyWidth-24,(bodyWidth-24)*progress.counted()/(double)goal.target());
            if(filled>0)graphics.fill(left+12,y+39,left+12+filled,y+40,progress.color());
            if(mx>=left+12&&mx<left+34+nameWidth&&listMouseY>=y&&listMouseY<y+16&&font.width(MemoryItems.name(goal.itemId()))>nameWidth)
                PlannerTooltips.show(graphics,mx,my,Component.literal(MemoryItems.name(goal.itemId())));
            if(mx>=left+bodyWidth-countWidth-12&&mx<left+bodyWidth-8&&listMouseY>=y&&listMouseY<y+16) {
                var details=new ArrayList<Component>();
                details.add(Component.translatable("stow.need.count-parts",progress.inventory(),progress.chests()));
                details.add(Component.literal(missing));
                long oldest=store.oldestSourceTime(goal.itemId());
                if(oldest>0)details.add(Component.translatable("stow.need.oldest",ChestMemoryStore.age(oldest)));
                PlannerTooltips.show(graphics,mx,my,details.toArray(Component[]::new));
            }
        }
        if(store==null)graphics.textWithWordWrap(font,Component.translatable("stow.memory.no-world"),left+12,startY+6,bodyWidth-24,0xFFAAAAAA);
        else if(goals.isEmpty()&&!search.getValue().isBlank()){
            graphics.text(font,Component.translatable("stow.materials.no-match"),left+12,startY+6,0xFFFFFFFF);
            graphics.textWithWordWrap(font,Component.translatable("stow.search.empty-help"),left+12,startY+26,bodyWidth-24,0xFFAAAAAA);
        }
        else if(goals.isEmpty()) {
            graphics.text(font,Component.translatable("stow.need.empty-title"),left+12,startY+6,0xFFFFFFFF);
            graphics.textWithWordWrap(font,Component.translatable("stow.need.empty-help"),left+12,startY+26,bodyWidth-24,0xFFAAAAAA);
        }
        endList(graphics);
        listScrollbar(graphics,page,pageSize,goals.size(),startY,dialogHeight-38);
        listRangeLabel(graphics,page,pageSize,goals.size());
        graphics.pose().popMatrix();super.extractRenderState(graphics,mx,my,delta);
        if(search.isFocused()&&search.getValue().isEmpty())graphics.text(font,font.plainSubstrByWidth(Component.translatable("stow.need.search").getString(),search.getWidth()-16),search.getX()+8,search.getY()+7,0xFF777777);
    }
}
