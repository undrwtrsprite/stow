package dev.stow.client.memory;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import static dev.stow.client.memory.ChestMemoryStore.MaterialGoal;

/** Paste, review and commit. Drafts never mutate the saved project. */
public final class MaterialImportScreen extends MemoryScreenBase {
    static final class Draft {
        final String original;String query,amount;MaterialListImporter.Item item;boolean included,skipped;
        Draft(MaterialListImporter.Row row){original=row.original();query=row.query();amount=row.amount();item=row.resolved();included=item!=null&&quantity()>0;skipped=amount.isEmpty();}
        int quantity(){return MaterialListImporter.quantity(amount,item==null?0:item.stackSize());}
        boolean valid(){return item!=null&&quantity()>0;}
    }
    private final ChestMemoryStore store;
    private final String projectId;
    private final MaterialListImporter importer=new MaterialListImporter(MemoryItems.importCatalogue());
    private final List<Draft> drafts=new ArrayList<>();
    private final List<AbstractWidget> rowWidgets=new ArrayList<>();
    private MultiLineEditBox input;
    private String text="",error="";
    private boolean reviewing,add;
    private int page,pageSize,left,bodyWidth,start;
    private PlannerButton confirm;
    private List<MaterialGoal> result=List.of();
    public MaterialImportScreen(Screen parent,ChestMemoryStore store){super(Component.translatable("stow.import.title"),parent,store);this.store=store;projectId=store.activeProjectId();}
    @Override protected void init(){
        layoutDialog(480);left=contentLeft();bodyWidth=contentWidth();start=workspaceTop()+54;
        rowWidgets.clear();
        addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b->{if(reviewing){reviewing=false;error="";rebuildWidgets();}else onClose();},left+8,height-30,66,22,false));
        if(!reviewing){
            trackList(List.of(),start,height-64,0,54,1);
            input=addRenderableWidget(MultiLineEditBox.builder().setX(left+8).setY(start).setPlaceholder(Component.translatable("stow.import.placeholder")).build(font,bodyWidth-16,Math.max(32,height-start-68),Component.translatable("stow.import.input")));
            input.setCharacterLimit(MaterialListImporter.MAX_TEXT);input.setValue(text);input.setValueListener(value->text=value);
            addRenderableWidget(new PlannerButton(Component.translatable("stow.import.paste"),b->{String pasted=minecraft.keyboardHandler.getClipboard();if(pasted.length()>MaterialListImporter.MAX_TEXT){error=Component.translatable("stow.import.text-limit").getString();return;}input.setValue(pasted);error="";},left+8,workspaceTop(),92,22,false));
            confirm=addRenderableWidget(new PlannerButton(Component.translatable("stow.import.review"),b->{
                try{drafts.clear();for(var row:importer.parse(text))drafts.add(new Draft(row));page=0;reviewing=true;error="";rebuildWidgets();}
                catch(IllegalArgumentException e){error=Component.translatable("stow.import.text-limit").getString();}
            },left+bodyWidth-112,height-30,104,22,true));
            setFocused(input);
        }else{
            pageSize=Math.max(1,(height-start-64)/54);page=Math.clamp(page,0,Math.max(0,drafts.size()-pageSize));
            var mode=addRenderableWidget(new PlannerButton(Component.translatable(add?"stow.import.mode-add":"stow.import.mode-replace"),b->{add=!add;rebuildWidgets();},left+8,workspaceTop(),bodyWidth-16,22,false));
            mode.setTooltip(Tooltip.create(Component.translatable("stow.import.mode-hint")));
            for(int i=page;i<Math.min(drafts.size(),page+pageSize);i++){
                Draft row=drafts.get(i);int y=start+(i-page)*54;
                var edit=addRenderableWidget(new PlannerButton(Component.translatable("stow.import.edit"),b->minecraft.gui.setScreen(new MaterialImportRowScreen(this,store,importer,row)),left+bodyWidth-88,y+18,48,22,false));
                String source=row.original.length()>140?row.original.substring(0,140)+"…":row.original;
                edit.setTooltip(Tooltip.create(Component.literal(source)));
                var toggle=addRenderableWidget(new PlannerButton(Component.translatable(row.included?"stow.import.skip":"stow.import.use-row"),b->{if(row.included){row.included=false;row.skipped=true;}else if(row.valid()){row.included=true;row.skipped=false;}else{minecraft.gui.setScreen(new MaterialImportRowScreen(this,store,importer,row));return;}rebuildWidgets();},left+bodyWidth-36,y+18,28,22,false).icon(row.included?dev.stow.client.ui.UIIcons.Kind.CHECK:dev.stow.client.ui.UIIcons.Kind.MINUS));
                toggle.setTooltip(Tooltip.create(Component.translatable(row.included?"stow.import.skip":row.valid()?"stow.import.use-row":"stow.import.choose-item")));
                rowWidgets.add(edit);rowWidgets.add(toggle);
            }
            trackList(rowWidgets,start-3,height-64,page,54,1);
            addRenderableWidget(new PlannerButton(Component.literal("<"),b->{page=Math.max(0,page-pageSize);rebuildWidgets();},left+80,height-30,24,22,false)).active=page>0;
            addRenderableWidget(new PlannerButton(Component.literal(">"),b->{page+=pageSize;rebuildWidgets();},left+108,height-30,24,22,false)).active=page+pageSize<drafts.size();
            confirm=addRenderableWidget(new PlannerButton(Component.translatable("stow.import.confirm"),b->commit(),left+bodyWidth-120,height-30,112,22,true));
            validate();
        }
    }
    private List<MaterialGoal> incoming(){return drafts.stream().filter(row->row.included&&row.valid()).map(row->new MaterialGoal(row.item.id(),row.quantity())).toList();}
    private void validate(){
        error="";var incoming=incoming();confirm.active=!incoming.isEmpty()&&projectId.equals(store.activeProjectId());
        if(!projectId.equals(store.activeProjectId())){error=Component.translatable("stow.import.project-changed").getString();return;}
        try{result=store.previewImport(incoming,add);}catch(IllegalArgumentException e){confirm.active=false;result=List.of();error=Component.translatable("stow.import.batch-limit").getString();}
    }
    private void commit(){
        validate();if(!confirm.active)return;
        var incoming=incoming();store.importGoals(incoming,add);ChestMemory.saveLater(store);
        dev.stow.client.ui.UiNotifications.show("stow.feedback.imported",incoming.stream().map(MaterialGoal::itemId).distinct().count());
        minecraft.gui.setScreen(new MaterialsScreen(rootScreen(),store,incoming.getFirst().itemId(),(int)incoming.stream().map(MaterialGoal::itemId).distinct().count()));
    }
    @Override public void tick(){if(reviewing)validate();else confirm.active=!text.isBlank();}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(reviewing&&vertical!=0&&x>=left&&x<left+bodyWidth&&y>=start&&y<height-64){page=scrollIndex(vertical,page,drafts.size(),pageSize,1);rebuildWidgets();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        extractMenuBackdrop(g,delta);
        if(!reviewing)g.textWithWordWrap(font,Component.translatable("stow.import.help"),left+8,workspaceTop()+27,bodyWidth-16,0xFFBDC9CC);
        else{
            long included=drafts.stream().filter(row->row.included&&row.valid()).count(),pending=drafts.stream().filter(row->!row.included&&!row.skipped).count();
            g.text(font,Component.translatable("stow.import.summary",included,pending),left+8,workspaceTop()+31,0xFFBDC9CC);
            beginList(g);
            for(int i=page;i<Math.min(drafts.size(),page+pageSize);i++){
                var row=drafts.get(i);int y=start+(i-page)*54;rowBackdrop(g,left+4,y-3,bodyWidth-8,51,row.included?0xFF8DE8B2:0xFF808080);
                g.text(font,font.plainSubstrByWidth(row.original,bodyWidth-24),left+12,y+2,0xFFAAAAAA);
                if(row.valid()){
                    int target=result.stream().filter(goal->goal.itemId().equals(row.item.id())).mapToInt(MaterialGoal::target).findFirst().orElse(row.quantity());
                    g.item(MemoryItems.icon(row.item.id()),left+12,y+18);
                    g.text(font,font.plainSubstrByWidth(row.item.name(),bodyWidth-138),left+34,y+18,row.included?0xFFFFFFFF:0xFFAAAAAA);
                    g.text(font,Component.translatable(row.included?"stow.import.row-amount":"stow.import.row-skip-amount",row.quantity(),target),left+34,y+33,row.included?0xFF8DE8B2:0xFFAAAAAA);
                }else g.textWithWordWrap(font,Component.translatable(row.skipped?"stow.import.ignored":"stow.import.needs-review"),left+12,y+20,bodyWidth-116,0xFFFFD486);
            }
            endList(g);listScrollbar(g,page,pageSize,drafts.size(),start-3,height-64);
            if(error.isEmpty())g.text(font,listRange(page,pageSize,drafts.size()),left+8,height-48,0xFFAAAAAA);
        }
        if(!error.isEmpty())g.text(font,font.plainSubstrByWidth(error,bodyWidth-16),left+8,height-48,0xFFFFAA91);
        super.extractRenderState(g,mx,my,delta);
    }
}
