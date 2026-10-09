package dev.stow.client.memory;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import static dev.stow.client.memory.ChestMemoryStore.*;

/** Search shared chest knowledge, or choose the active project's storage scope. */
public final class ChestMemoryScreen extends MemoryScreenBase {
    private final ChestMemoryStore store;
    private final String initialQuery;
    private final boolean selectionMode;
    private final String targetId;
    private EditBox search;
    private List<SearchResult> results=List.of();
    private final List<Button> rows=new ArrayList<>();
    private int page,startY,pageSize,left,bodyWidth,toolbarY;
    private long revision=-1;
    private final Map<String,Integer> selectionOrder=new LinkedHashMap<>();
    private int columns=1,cellWidth;
    private PlannerButton previous,next,stopGlow,undo,all,none;
    public ChestMemoryScreen(Screen parent,ChestMemoryStore store,String query){this(parent,store,query,false,null);}
    public ChestMemoryScreen(Screen parent,ChestMemoryStore store,String query,boolean selectionMode,String targetId){
        super(Component.translatable(selectionMode?"stow.need.select-chests":"stow.memory.title"),parent,store);
        this.store=store;this.initialQuery=query;this.selectionMode=selectionMode;this.targetId=targetId;
    }
    @Override protected void init(){
        layoutDialog(480);
        String query=search==null?initialQuery:search.getValue();bodyWidth=contentWidth();left=contentLeft();
        toolbarY=workspaceTop();startY=toolbarY+44;columns=bodyWidth>=(selectionMode?280:480)?2:1;cellWidth=(bodyWidth-12)/columns;pageSize=Math.max(1,(dialogHeight-startY-36)/(selectionMode?62:78))*columns;
        search=addRenderableWidget(new EditBox(font,left+8,toolbarY,bodyWidth-112,22,Component.translatable("stow.memory.search")));
        search.setMaxLength(80);search.setHint(Component.translatable("stow.memory.search"));search.setValue(query);search.setResponder(value -> refresh(true));
        if(selectionMode){
            all=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.all"),b->{store.selectAllChests(true);dev.stow.client.ui.UiNotifications.show("stow.feedback.chests-all",store.projectName());save();},left+bodyWidth-98,toolbarY,42,22,false).selected(()->store!=null&&store.usesAllChests()));
            none=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.none"),b->{store.selectAllChests(false);dev.stow.client.ui.UiNotifications.show("stow.feedback.chests-none");save();},left+bodyWidth-50,toolbarY,42,22,false).selected(()->store!=null&&!store.usesAllChests()&&store.includedChestCount()==0));
            all.active=none.active=store!=null;
        }else{
            var materials=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.title-short"),b->minecraft.gui.setScreen(new MaterialsScreen(this,store)),left+bodyWidth-98,toolbarY,90,22,false));materials.active=store!=null;
        }
        addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b -> onClose(),left+8,dialogHeight-30,66,22,false));
        stopGlow=addRenderableWidget(new PlannerButton(Component.translatable("stow.memory.stop-glow"),b -> {ChestMemory.stopGlow();refresh(false);},left+80,dialogHeight-30,76,22,false));
        undo=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.undo"),b -> {store.undo();dev.stow.client.ui.UiNotifications.show("stow.feedback.undo");save();},left+162,dialogHeight-30,56,22,false));
        previous=addRenderableWidget(new PlannerButton(Component.literal("<"),b -> {page-=pageSize;refresh(false);},left+bodyWidth-68,dialogHeight-30,26,22,false));
        next=addRenderableWidget(new PlannerButton(Component.literal(">"),b -> {page+=pageSize;refresh(false);},left+bodyWidth-36,dialogHeight-30,26,22,false));
        setFocused(search);refresh(false);
    }
    public boolean isSelectionMode(){return selectionMode;}
    private void save(){ChestMemory.saveLater(store);refresh(false);}
    private void refresh(boolean resetPage){
        if(resetPage){page=0;selectionOrder.clear();}
        String dimension=minecraft.level==null?"":minecraft.level.dimension().identifier().toString();
        results=store==null?List.of():store.search(search.getValue(),dimension,minecraft.player==null?null:minecraft.player.blockPosition());
        if(targetId!=null)results=results.stream().filter(r -> store.countIn(r.chest().location(),targetId)>0).map(r -> new SearchResult(r.chest(),(int)store.countIn(r.chest().location(),targetId),r.chest().items().stream().filter(i -> i.id().equals(targetId)).toList())).toList();
        if(selectionMode&&store!=null){
            if(selectionOrder.isEmpty())results=results.stream().sorted(java.util.Comparator.comparingInt(r->store.isIncluded(r.chest().location())?0:1)).toList();
            for(var result:results)selectionOrder.putIfAbsent(result.chest().location().key(),selectionOrder.size());
            results=results.stream().sorted(java.util.Comparator.comparingInt(r->selectionOrder.get(r.chest().location().key()))).toList();
        }
        revision=store==null?0:store.revision();page=Math.clamp(page,0,Math.max(0,((results.size()-pageSize+columns-1)/columns)*columns));rows.forEach(this::removeWidget);rows.clear();
        for(int i=page;i<Math.min(results.size(),(page+pageSize));i++){
            var result=results.get(i);
            if(selectionMode){
                int n=i-page,x=left+6+n%columns*cellWidth,y=startY+n/columns*62;
                var select=new PlannerButton(Component.literal(store.isIncluded(result.chest().location())?"x":"+"),b->{store.toggleIncluded(result.chest().location());dev.stow.client.ui.UiNotifications.show(store.isIncluded(result.chest().location())?"stow.feedback.chest-included":"stow.feedback.chest-excluded",store.displayName(result.chest()),store.projectName());save();},x+2,y+2,20,20,false).selected(()->store.isIncluded(result.chest().location())).icon(store.isIncluded(result.chest().location())?dev.stow.client.ui.UIIcons.Kind.CHECK:dev.stow.client.ui.UIIcons.Kind.PLUS);
                select.setTooltip(Tooltip.create(Component.translatable(store.isIncluded(result.chest().location())?"stow.need.exclude-chest":"stow.need.include-chest")));rows.add(addRenderableWidget(select));
                var forget=new PlannerButton(Component.translatable("stow.memory.forget"),b->{ChestMemory.forget(store,result.chest().location());save();},x+cellWidth-50,y+2,20,20,false).icon(dev.stow.client.ui.UIIcons.Kind.CLOSE);
                forget.setTooltip(Tooltip.create(Component.translatable("stow.memory.forget-hint")));rows.add(addRenderableWidget(forget));
                var glow=new InventoryIconButton(InventoryIconButton.Kind.GLOW,Component.translatable("stow.memory.glow"),b->{ChestMemory.select(result.chest().location());refresh(false);},x+cellWidth-26,y+2);glow.setTooltip(Tooltip.create(Component.translatable("stow.memory.glow")));rows.add(addRenderableWidget(glow));
                rows.add(addRenderableWidget(new PlannerButton(Component.translatable("stow.memory.rename"),b->minecraft.gui.setScreen(new ChestRenameScreen(this,store,result.chest())),x+cellWidth-52,y+34,46,20,false)));
                continue;
            }
            int n=i-page,x=left+6+n%columns*cellWidth,y=startY+n/columns*78;boolean glowing=dev.stow.Stow.config.chestGlow&&Objects.equals(ChestMemory.selected(),result.chest().location());
            var glow=new PlannerButton(Component.translatable(glowing?"stow.memory.hide":"stow.memory.glow"),b -> {ChestMemory.select(result.chest().location());refresh(false);},x+cellWidth-48,y+2,42,22,false);
            rows.add(addRenderableWidget(glow));
            rows.add(addRenderableWidget(new PlannerButton(Component.translatable("stow.memory.rename"),b -> minecraft.gui.setScreen(new ChestRenameScreen(this,store,result.chest())),x+cellWidth-108,y+44,48,22,false)));
            var forget=new PlannerButton(Component.translatable("stow.memory.forget"),b -> {ChestMemory.forget(store,result.chest().location());save();},x+cellWidth-54,y+44,48,22,false);
            forget.setTooltip(Tooltip.create(Component.translatable("stow.memory.forget-hint")));rows.add(addRenderableWidget(forget));

        }
        trackList(rows,startY,dialogHeight-36,page,selectionMode?62:78,columns);
        previous.active=page>0;next.active=(page+pageSize)<results.size();stopGlow.active=ChestMemory.selected()!=null;
        undo.active=store!=null && store.canUndo();undo.setTooltip(PlannerTooltips.undo(store));
    }
    @Override public void tick(){if(store!=null && store.revision()!=revision)refresh(false);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(vertical!=0 && mx>=left && mx<left+bodyWidth && my-dialogTop>=startY && my-dialogTop<dialogHeight-36){page=scrollIndex(vertical,page,results.size(),pageSize,columns);refresh(false);return true;}return super.mouseScrolled(mx,my,horizontal,vertical);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        extractMenuBackdrop(g,delta);g.pose().pushMatrix();g.pose().translate(0,dialogTop);
        
        String context=selectionMode?Component.translatable("stow.storage.scope",store==null?"":store.projectName(),store==null?0:store.includedChestCount()).getString():Component.translatable("stow.storage.summary",results.size()).getString();
        g.text(font,font.plainSubstrByWidth(context,bodyWidth-84),left+8,toolbarY+29,0xFFAAAAAA);
        String pageText=listRange(page,pageSize,results.size());g.text(font,pageText,left+bodyWidth-8-font.width(pageText),toolbarY+29,0xFFAAAAAA);
        beginList(g);int listMouseY=my-Math.round(listMotion.offset());
        for(int i=page;i<Math.min(results.size(),(page+pageSize));i++){
            var result=results.get(i);var chest=result.chest();var location=chest.location();
            if(selectionMode){
                int n=i-page,x=left+6+n%columns*cellWidth,y=startY+n/columns*62;
                rowBackdrop(g,x,y,cellWidth-4,60,store.isIncluded(location)?0xFF8DE8B2:0xFF808080);
                String name=store.displayName(chest),position=location.x()+", "+location.y()+", "+location.z();
                g.text(font,font.plainSubstrByWidth(name,cellWidth-80),x+26,y+5,0xFFFFFFFF);g.text(font,font.plainSubstrByWidth(position,cellWidth-80),x+26,y+17,0xFFAAAAAA);
                MemoryItems.drawPreview(g,chest.items(),x+6,y+35,Math.max(1,Math.min(4,(cellWidth-104)/MemoryItems.PREVIEW_CELL)),mx,my,dialogTop+Math.round(listMotion.offset()));
                if(mx>=x+24&&mx<x+cellWidth-52&&listMouseY-dialogTop>=y&&listMouseY-dialogTop<y+28)PlannerTooltips.show(g,mx,my,Component.literal(name),Component.literal(position+" · "+dimensionName(location.dimension())));
                continue;
            }
            int n=i-page,x=left+6+n%columns*cellWidth,y=startY+n/columns*78;
            rowBackdrop(g,x,y,cellWidth-4,76,Objects.equals(ChestMemory.selected(),location)&&dev.stow.Stow.config.chestGlow?0xFFFFD486:0xFF808080);
            String name=store.displayName(chest),position=location.x()+", "+location.y()+", "+location.z();
            g.text(font,font.plainSubstrByWidth(name,cellWidth-56),x+8,y+6,0xFFFFFFFF);
            g.text(font,font.plainSubstrByWidth(position+" · "+dimensionName(location.dimension()),cellWidth-16),x+8,y+28,0xFFAAAAAA);
            MemoryItems.drawPreview(g,chest.items(),x+8,y+46,Math.max(1,Math.min(5,(cellWidth-140)/MemoryItems.PREVIEW_CELL)),mx,my,dialogTop+Math.round(listMotion.offset()));
            if(mx>=x+4&&mx<x+cellWidth-48&&listMouseY-dialogTop>=y&&listMouseY-dialogTop<y+34)PlannerTooltips.show(g,mx,my,Component.literal(name),Component.literal(position+" · "+dimensionName(location.dimension())),Component.translatable("stow.memory.last-seen",ChestMemoryStore.age(chest.lastSeen())));

        }
        if(results.isEmpty()){
            if(store==null)g.textWithWordWrap(font,Component.translatable("stow.memory.no-world"),left+8,startY+6,bodyWidth-16,0xFFAAAAAA);
            else{
                String title=!search.getValue().isBlank()?"stow.storage.no-match":targetId!=null?"stow.storage.no-source":"stow.memory.empty";
                String help=!search.getValue().isBlank()?"stow.search.empty-help":targetId!=null?"stow.storage.no-source-help":"stow.memory.empty-help";
                g.text(font,Component.translatable(title),left+8,startY+6,0xFFFFFFFF);
                g.textWithWordWrap(font,Component.translatable(help,targetId==null?"":MemoryItems.name(targetId)),left+8,startY+26,bodyWidth-16,0xFFAAAAAA);
            }
        }
        endList(g);
        listScrollbar(g,page,pageSize,results.size(),startY,dialogHeight-36);
        g.pose().popMatrix();super.extractRenderState(g,mx,my,delta);
        if(search.isFocused()&&search.getValue().isEmpty())g.text(font,font.plainSubstrByWidth(Component.translatable("stow.memory.search").getString(),search.getWidth()-16),search.getX()+8,search.getY()+7,0xFF777777);
    }
    private static String dimensionName(String dimension){return switch(dimension){case "minecraft:overworld" -> Component.translatable("stow.dimension.overworld").getString();case "minecraft:the_nether" -> Component.translatable("stow.dimension.nether").getString();case "minecraft:the_end" -> Component.translatable("stow.dimension.end").getString();default -> dimension;};}
}
