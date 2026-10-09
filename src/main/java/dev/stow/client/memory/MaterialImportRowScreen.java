package dev.stow.client.memory;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Select an exact match, fix a quantity, or explicitly skip a pasted line. */
final class MaterialImportRowScreen extends MemoryScreenBase {
    private final MaterialListImporter importer;
    private final MaterialImportScreen.Draft draft;
    private MaterialListImporter.Item selected;
    private EditBox search,amount;
    private final List<AbstractWidget> results=new ArrayList<>();
    private PlannerButton confirm;
    private int page,pageSize,left,bodyWidth,start;
    MaterialImportRowScreen(Screen parent,ChestMemoryStore store,MaterialListImporter importer,MaterialImportScreen.Draft draft){
        super(Component.translatable("stow.import.edit-title"),parent,store);this.importer=importer;this.draft=draft;selected=draft.item;
    }
    @Override protected void init(){
        layoutDialog(480);left=contentLeft();bodyWidth=contentWidth();start=workspaceTop()+58;pageSize=Math.max(1,(height-start-94)/26);
        String query=search==null?draft.query:search.getValue(),quantity=amount==null?draft.amount:amount.getValue();
        search=addRenderableWidget(new EditBox(font,left+8,workspaceTop()+24,bodyWidth-16,22,Component.translatable("stow.need.search")));search.setMaxLength(256);search.setValue(query);search.setHint(Component.translatable("stow.need.search"));
        amount=addRenderableWidget(new EditBox(font,left+8,height-82,120,22,Component.translatable("stow.need.amount")));amount.setMaxLength(64);amount.setValue(quantity);amount.setHint(Component.literal("64 / 2 stacks + 16"));
        amount.setTooltip(Tooltip.create(Component.translatable("stow.import.quantity-hint")));
        confirm=addRenderableWidget(new PlannerButton(Component.translatable("stow.import.use-row"),b->{if(!valid())return;draft.query=selected.name();draft.item=selected;draft.amount=amount.getValue();draft.included=true;draft.skipped=false;onClose();},left+bodyWidth-112,height-30,104,22,true));
        addRenderableWidget(new PlannerButton(Component.translatable("gui.cancel"),b->onClose(),left+8,height-30,66,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("stow.import.skip"),b->{draft.included=false;draft.skipped=true;onClose();},left+80,height-30,64,22,false));
        search.setResponder(value->{var matches=importer.resolve(value);selected=matches.size()==1?matches.getFirst():null;page=0;refresh();});amount.setResponder(value->confirm.active=valid());
        refresh();setFocused(search);
    }
    private boolean valid(){return selected!=null&&MaterialListImporter.quantity(amount.getValue(),selected.stackSize())>0;}
    private void refresh(){
        results.forEach(this::removeWidget);results.clear();var matches=importer.search(search.getValue());page=Math.clamp(page,0,Math.max(0,matches.size()-pageSize));
        for(int i=page;i<Math.min(matches.size(),page+pageSize);i++){
            var item=matches.get(i);var button=new PlannerButton(Component.literal(item.name()),b->{selected=item;setFocused(amount);confirm.active=valid();},left+8,start+(i-page)*26,bodyWidth-16,22,false).selected(()->selected!=null&&selected.id().equals(item.id()));
            button.setTooltip(Tooltip.create(Component.literal(item.id())));results.add(addRenderableWidget(button));
        }
        trackList(results,start,height-94,page,26,1);
        confirm.active=valid();
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(vertical!=0&&y>=start&&y<height-94){page=scrollIndex(vertical,page,importer.search(search.getValue()).size(),pageSize,1);refresh();return true;}return super.mouseScrolled(x,y,horizontal,vertical);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        extractMenuBackdrop(g,delta);g.text(font,font.plainSubstrByWidth(draft.original,bodyWidth-16),left+8,workspaceTop()+4,0xFFBDC9CC);
        String status=selected==null?Component.translatable("stow.import.choose-item").getString():Component.translatable("stow.import.quantity-preview",MaterialListImporter.quantity(amount.getValue(),selected.stackSize())).getString();
        g.text(font,font.plainSubstrByWidth(status,bodyWidth-152),left+140,height-75,valid()?0xFF8DE8B2:0xFFFFAA91);
        listScrollbar(g,page,pageSize,importer.search(search.getValue()).size(),start,height-94);
        beginList(g);endList(g);
        super.extractRenderState(g,mx,my,delta);
    }
}
