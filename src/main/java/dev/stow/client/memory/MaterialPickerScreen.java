package dev.stow.client.memory;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

/** Explicit choose -> quantity -> add flow; clicking a search result never saves by accident. */
public final class MaterialPickerScreen extends MemoryScreenBase {
    private final ChestMemoryStore store;
    private final List<MemoryItems.Entry> catalogue;
    private final String initialQuery;
    private final int initialAmount;
    private EditBox search,amount;
    private MemoryItems.Entry selected;
    private List<MemoryItems.Entry> results=List.of();
    private final List<PlannerButton> rows=new ArrayList<>();
    private int page,pageSize,left,bodyWidth,gridTop,gridBottom;
    private PlannerButton previous,next,confirm;
    private String error="";
    public MaterialPickerScreen(Screen parent,ChestMemoryStore store,String query,int amount) {
        super(Component.translatable("stow.need.picker"),parent,store);this.store=store;this.initialQuery=query;this.initialAmount=amount;
        catalogue=MemoryItems.catalogue();if(!query.isBlank())selected=MemoryItems.resolve(catalogue,query);
    }
    @Override protected void init() {
        layoutDialog(420);
        String query=search==null?initialQuery:search.getValue(),value=amount==null?Integer.toString(initialAmount):amount.getValue();
        bodyWidth=contentWidth();left=contentLeft();gridTop=76;gridBottom=dialogHeight-100;
        pageSize=Math.max(1,(gridBottom-gridTop)/28)*2;
        search=addRenderableWidget(new EditBox(font,left+8,44,bodyWidth-16,22,Component.translatable("stow.need.search")));
        search.setMaxLength(80);search.setHint(Component.translatable("stow.need.search"));search.setValue(query);
        int footer=dialogHeight-90;
        amount=addRenderableWidget(new EditBox(font,left+88,footer+28,80,20,Component.translatable("stow.need.amount")));
        amount.setMaxLength(7);amount.setValue(value);
        confirm=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.confirm-add"),b -> {
            if(selected==null || target()==0)return;
            try{store.setGoal(selected.id(),target());ChestMemory.saveLater(store);onClose();}
            catch(IllegalArgumentException e){error=Component.translatable("stow.need.limit").getString();}
        },left+bodyWidth-104,footer+28,96,20,true));
        search.setResponder(s -> {page=0;refresh();});amount.setResponder(s -> {error="";syncConfirm();});
        addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b -> onClose(),left+8,dialogHeight-28,66,20,false));
        previous=addRenderableWidget(new PlannerButton(Component.literal("<"),b -> {page--;refresh();},left+bodyWidth-68,dialogHeight-28,26,20,false));
        next=addRenderableWidget(new PlannerButton(Component.literal(">"),b -> {page++;refresh();},left+bodyWidth-36,dialogHeight-28,26,20,false));
        setFocused(selected==null?search:amount);if(selected!=null){amount.moveCursorToEnd(false);amount.setHighlightPos(0);}refresh();
    }
    private int target(){try{int count=Integer.parseInt(amount.getValue());return count>0 && count<=1_000_000?count:0;}catch(NumberFormatException e){return 0;}}
    private void syncConfirm() {
        confirm.active=selected!=null && target()>0 && store!=null;
        boolean existing=selected!=null && store!=null && store.goals().stream().anyMatch(g -> g.itemId().equals(selected.id()));
        confirm.setMessage(Component.translatable(existing?"stow.need.confirm-save":"stow.need.confirm-add"));
        amount.setTextColor(target()>0?0xFFE9F0F3:0xFFFFAA91);
        amount.setTooltip(target()>0?null:Tooltip.create(Component.translatable("stow.need.amount-range")));
    }
    private void refresh() {
        results=MemoryItems.search(catalogue,search.getValue());
        page=Math.max(0,Math.min(page,Math.max(0,(results.size()-1)/pageSize)));
        rows.forEach(this::removeWidget);rows.clear();
        int cellWidth=(bodyWidth-20)/2;
        for(int i=page*pageSize;i<Math.min(results.size(),(page+1)*pageSize);i++) {
            var entry=results.get(i);int n=i-page*pageSize,x=left+8+(n%2)*(cellWidth+4),y=gridTop+(n/2)*28;
            PlannerButton button=new PlannerButton(Component.literal(entry.name()),b -> {selected=entry;error="";syncConfirm();setFocused(amount);amount.moveCursorToEnd(false);amount.setHighlightPos(0);},x,y,cellWidth,24,false)
                .item(entry.id()).selected(() -> selected!=null && selected.id().equals(entry.id()));
            boolean ambiguous=catalogue.stream().filter(e -> e.name().equals(entry.name())).limit(2).count()>1;
            if(ambiguous || font.width(entry.name())>cellWidth-36)button.setTooltip(Tooltip.create(Component.literal(ambiguous?entry.name()+" · "+entry.id():entry.name())));
            rows.add(addRenderableWidget(button));
        }
        previous.active=page>0;next.active=(page+1)*pageSize<results.size();syncConfirm();
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(vertical!=0 && mx>=left && mx<left+bodyWidth && my-dialogTop>=gridTop && my-dialogTop<gridBottom){page+=vertical<0?1:-1;refresh();return true;}
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if((event.key()==InputConstants.KEY_RETURN || event.key()==InputConstants.KEY_NUMPADENTER) && amount.isFocused() && confirm.active){confirm.onPress(event);return true;}
        return super.keyPressed(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mx,int my,float delta) {
        extractMenuBackdrop(graphics,delta);graphics.pose().pushMatrix();graphics.pose().translate(0,dialogTop);
        
        
        graphics.text(font,Component.translatable("stow.need.picker-step"),left+8,30,0xFFAAAAAA);
        int footer=dialogHeight-90;
        graphics.horizontalLine(left+8,left+bodyWidth-8,footer-7,0xFF3B4B55);
        if(selected==null)graphics.text(font,Component.translatable("stow.need.pick-first"),left+8,footer+2,0xFFAAAAAA);
        else {
            graphics.item(MemoryItems.icon(selected.id()),left+8,footer-1);
            graphics.text(font,font.plainSubstrByWidth(selected.name(),bodyWidth-50),left+30,footer+3,0xFF9BDFCB);
        }
        String label=Component.translatable("stow.need.amount").getString();
        graphics.text(font,label,left+8,footer+34,0xFFAAAAAA);
        if(results.isEmpty())graphics.text(font,font.plainSubstrByWidth(Component.translatable("stow.need.no-items").getString(),bodyWidth-16),left+8,gridTop+4,0xFFAAAAAA);
        if(!error.isEmpty())graphics.text(font,font.plainSubstrByWidth(error,bodyWidth-96),left+82,dialogHeight-22,0xFFFFAA91);
        else graphics.text(font,(page+1)+" / "+Math.max(1,(results.size()+pageSize-1)/pageSize),left+84,dialogHeight-22,0xFFAAAAAA);
        graphics.pose().popMatrix();super.extractRenderState(graphics,mx,my,delta);
    }
}
