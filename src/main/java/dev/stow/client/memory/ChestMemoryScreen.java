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
    private int page,startY,pageSize,left,bodyWidth;
    private long revision=-1;
    private PlannerButton previous,next,stopGlow,undo,all,none;
    public ChestMemoryScreen(Screen parent,ChestMemoryStore store,String query){this(parent,store,query,false,null);}
    public ChestMemoryScreen(Screen parent,ChestMemoryStore store,String query,boolean selectionMode,String targetId){
        super(Component.translatable(selectionMode?"stow.need.select-chests":"stow.memory.title"),parent,store);
        this.store=store;this.initialQuery=query;this.selectionMode=selectionMode;this.targetId=targetId;
    }
    @Override protected void init(){
        layoutDialog(480);
        String query=search==null?initialQuery:search.getValue();bodyWidth=contentWidth();left=contentLeft();
        startY=selectionMode?108:82;pageSize=Math.max(1,(dialogHeight-startY-36)/72);
        search=addRenderableWidget(new EditBox(font,left+8,36,bodyWidth-112,20,Component.translatable("stow.memory.search")));
        search.setMaxLength(80);search.setHint(Component.translatable("stow.memory.search"));search.setValue(query);search.setResponder(value -> refresh(true));
        var materials=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.title-short"),b -> minecraft.gui.setScreen(new MaterialsScreen(this,store)),left+bodyWidth-98,36,90,20,false));materials.active=store!=null;
        if(selectionMode){
            all=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.all"),b -> {store.selectAllChests(true);save();},left+8,62,62,20,false).selected(() -> store!=null && store.usesAllChests()));
            none=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.none"),b -> {store.selectAllChests(false);save();},left+76,62,62,20,false).selected(() -> store!=null && !store.usesAllChests() && store.includedChestCount()==0));
            all.active=none.active=store!=null;all.setTooltip(Tooltip.create(Component.translatable("stow.need.scope")));
        }
        addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b -> onClose(),left+8,dialogHeight-28,56,20,false));
        stopGlow=addRenderableWidget(new PlannerButton(Component.translatable("stow.memory.stop-glow"),b -> {if(ChestMemory.selected()!=null)ChestMemory.select(ChestMemory.selected());refresh(false);},left+70,dialogHeight-28,86,20,false));
        undo=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.undo"),b -> {store.undo();save();},left+162,dialogHeight-28,56,20,false));
        previous=addRenderableWidget(new PlannerButton(Component.literal("<"),b -> {page--;refresh(false);},left+bodyWidth-62,dialogHeight-28,24,20,false));
        next=addRenderableWidget(new PlannerButton(Component.literal(">"),b -> {page++;refresh(false);},left+bodyWidth-32,dialogHeight-28,24,20,false));
        setFocused(search);refresh(false);
    }
    private void save(){ChestMemory.saveLater(store);refresh(false);}
    private void refresh(boolean resetPage){
        if(resetPage)page=0;
        String dimension=minecraft.level==null?"":minecraft.level.dimension().identifier().toString();
        results=store==null?List.of():store.search(search.getValue(),dimension,minecraft.player==null?null:minecraft.player.blockPosition());
        if(targetId!=null)results=results.stream().filter(r -> store.countIn(r.chest().location(),targetId)>0).map(r -> new SearchResult(r.chest(),(int)store.countIn(r.chest().location(),targetId),r.chest().items().stream().filter(i -> i.id().equals(targetId)).toList())).toList();
        revision=store==null?0:store.revision();page=Math.max(0,Math.min(page,Math.max(0,(results.size()-1)/pageSize)));rows.forEach(this::removeWidget);rows.clear();
        for(int i=page*pageSize;i<Math.min(results.size(),(page+1)*pageSize);i++){
            var result=results.get(i);int y=startY+(i-page*pageSize)*72;boolean glowing=Objects.equals(ChestMemory.selected(),result.chest().location());
            var glow=new PlannerButton(Component.translatable(glowing?"stow.memory.hide":"stow.memory.glow"),b -> {ChestMemory.select(result.chest().location());refresh(false);},left+bodyWidth-64,y,56,20,false);
            glow.setTooltip(Tooltip.create(Component.translatable("stow.memory.glow.description")));
            rows.add(addRenderableWidget(glow));
            rows.add(addRenderableWidget(new PlannerButton(Component.translatable("stow.memory.rename"),b -> minecraft.gui.setScreen(new ChestRenameScreen(this,store,result.chest())),left+bodyWidth-130,y+30,58,20,false)));
            rows.add(addRenderableWidget(new PlannerButton(Component.translatable("stow.memory.forget"),b -> {store.forget(result.chest().location());save();},left+bodyWidth-66,y+30,58,20,false)));
            if(selectionMode){var select=new PlannerButton(Component.literal(store.isIncluded(result.chest().location())?"x":"+"),b -> {store.toggleIncluded(result.chest().location());save();},left+8,y,22,20,false).selected(() -> store.isIncluded(result.chest().location()));select.setTooltip(Tooltip.create(Component.translatable(store.isIncluded(result.chest().location())?"stow.need.exclude-chest":"stow.need.include-chest")));rows.add(addRenderableWidget(select));}
        }
        previous.active=page>0;next.active=(page+1)*pageSize<results.size();stopGlow.active=ChestMemory.selected()!=null;
        undo.active=store!=null && store.canUndo();undo.setTooltip(PlannerTooltips.undo(store));
    }
    @Override public void tick(){if(store!=null && store.revision()!=revision)refresh(false);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(vertical!=0 && mx>=left && mx<left+bodyWidth && my-dialogTop>=startY && my-dialogTop<dialogHeight-36){page+=vertical<0?1:-1;refresh(false);return true;}return super.mouseScrolled(mx,my,horizontal,vertical);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        extractMenuBackdrop(g,delta);g.pose().pushMatrix();g.pose().translate(0,dialogTop);
        
        if(selectionMode){g.text(font,font.plainSubstrByWidth(Component.translatable("stow.need.selected",store==null?0:store.includedChestCount()).getString(),bodyWidth-154),left+148,68,0xFFCCCCCC);
            g.text(font,font.plainSubstrByWidth(Component.translatable("stow.need.project-storage-notice").getString(),bodyWidth-16),left+8,90,0xFFAAAAAA);
        }else g.text(font,font.plainSubstrByWidth(Component.translatable("stow.memory.notice").getString(),bodyWidth-16),left+8,64,0xFFAAAAAA);
        for(int i=page*pageSize;i<Math.min(results.size(),(page+1)*pageSize);i++){
            var result=results.get(i);var chest=result.chest();var location=chest.location();int y=startY+(i-page*pageSize)*72;
            g.fill(left+4,y-3,left+bodyWidth-4,y+66,0x18000000);g.fill(left+4,y-3,left+6,y+66,selectionMode && store.isIncluded(location)?0xFFFFFFFF:0xFF808080);
            int labelX=selectionMode?left+36:left+8;
            g.text(font,font.plainSubstrByWidth(store.displayName(chest),left+bodyWidth-labelX-74),labelX,y+1,0xFFFFFFFF);
            String position=location.x()+", "+location.y()+", "+location.z()+" · "+dimensionName(location.dimension());
            g.text(font,font.plainSubstrByWidth(position,left+bodyWidth-labelX-74),labelX,y+15,0xFFCCCCCC);
            int slots=Math.max(1,Math.min(5,(bodyWidth-154)/22));MemoryItems.drawPreview(g,chest.items(),left+8,y+31,slots,mx,my,dialogTop);
            String age=ChestMemoryStore.age(chest.lastSeen());g.text(font,Component.translatable("stow.memory.last-seen",age),left+8,y+54,0xFFAAAAAA);
            String count=result.count()+" "+Component.translatable("stow.memory.items").getString();g.text(font,count,left+bodyWidth-font.width(count)-8,y+54,0xFFAAAAAA);
            int labelWidth=left+bodyWidth-labelX-74;
            if(mx>=labelX && mx<left+bodyWidth-74 && my-dialogTop>=y && my-dialogTop<y+12 && font.width(store.displayName(chest))>labelWidth)PlannerTooltips.show(g,mx,my,Component.literal(store.displayName(chest)));
            if(mx>=labelX && mx<left+bodyWidth-74 && my-dialogTop>=y+12 && my-dialogTop<y+26 && font.width(position)>labelWidth)PlannerTooltips.show(g,mx,my,Component.literal(position));
        }
        if(results.isEmpty())g.textWithWordWrap(font,Component.translatable(store==null?"stow.memory.no-world":"stow.memory.empty"),left+8,startY+6,bodyWidth-16,0xFFAAAAAA);
        g.pose().popMatrix();super.extractRenderState(g,mx,my,delta);
    }
    private static String dimensionName(String dimension){return switch(dimension){case "minecraft:overworld" -> "Overworld";case "minecraft:the_nether" -> "Nether";case "minecraft:the_end" -> "The End";default -> dimension;};}
}
