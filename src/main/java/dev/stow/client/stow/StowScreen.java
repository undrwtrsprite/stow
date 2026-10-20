package dev.stow.client.stow;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stow.Stow;
import dev.stow.StowConfig;
import dev.stow.client.input.StowShortcuts;
import dev.stow.client.input.StowShortcuts.Action;
import dev.stow.client.memory.*;
import dev.stow.client.stow.StowSession.*;
import dev.stow.client.ui.StowEditBox;
import dev.stow.client.ui.UIIcons;
import dev.stow.client.ui.UiNotifications;
import dev.stow.client.util.TextQuery;
import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.*;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import static dev.stow.client.memory.ChestMemoryStore.*;
import static dev.stow.client.stow.StowTheme.*;

/**
 * Redesign v2 Stow screen: one query bar over Find / Project / Containers, with master/detail
 * (wide, content width at least 600 GUI px) or a pushed detail view (compact). Views only:
 * all data changes go through ChestMemoryStore and the existing Companion requests.
 */
public final class StowScreen extends Screen {
    private static final Pattern NEED=Pattern.compile("(?i)^(?:need\\s+)(\\d{1,7})\\s*[x×]?\\s+(.+)$");
    private static final int ROW=22,HEADER=14;
    enum Kind { HEADER,ITEM,CATALOGUE,CONTAINER,ACTION,PROJECT,GOAL,NEED,MORE }
    record Row(Kind kind,String key,String title,String sub,int subColor,String right,int rightColor,ItemStack icon,Object data) {
        boolean selectable(){return kind!=Kind.HEADER;}
        int height(){return kind==Kind.HEADER?HEADER:ROW;}
    }
    enum Popup { NONE,PROJECTS,SCOPE,HELP }

    private final Screen parent;
    private final ChestMemoryStore store;
    private final StowSession session;
    private final List<MemoryItems.Entry> catalogue;
    private EditBox query;
    private int left,w,listX,listW,detailX,detailW,panelTop,panelBottom,listTop;
    private boolean wide,detailOpen,keyboard,addMode;
    private List<Row> rows=List.of();
    private int selected=-1,scroll;
    private long seenRevision=-1;
    private final Set<String> expanded=new HashSet<>();
    private final List<AbstractWidget> dynamic=new ArrayList<>(),popupWidgets=new ArrayList<>();
    private Popup popup=Popup.NONE;
    private int popupX,popupY,popupW,popupH;
    private EditBox inline,projectName;
    private PlannerButton createButton;
    private String inlineFor="",message="",error="";
    private int historyIndex=-1;
    private long deleteArmed;

    private StowScreen(Screen parent,ChestMemoryStore store){
        super(Component.translatable("stow.ui.title"));this.parent=parent;this.store=store;
        session=StowSession.get(ChestMemory.currentWorldKey());catalogue=MemoryItems.catalogue();
    }
    /** Ctrl+K, the strip's Stow button, Mod Menu (via settings) and the per-segment shortcuts all land here. */
    public static StowScreen open(Screen parent,ItemStack hovered,Segment segment){
        while(parent instanceof StowScreen stow)parent=stow.parent;
        var screen=new StowScreen(parent,ChestMemory.currentStore());var s=screen.session;
        if(segment!=null)s.segment=segment;
        if(hovered!=null&&!hovered.isEmpty()){s.segment=Segment.FIND;s.query.put(Segment.FIND,hovered.getHoverName().getString());s.selection.remove(Segment.FIND);}
        var active=parent instanceof AbstractContainerScreen<?>?ChestMemory.activeLocation():null;
        if(active!=null){s.selection.put(Segment.CONTAINERS,"container:"+active.key());s.query.put(Segment.CONTAINERS,"");s.containerFilter=ContainerFilter.ALL;}
        return screen;
    }
    /** The strip's Stow button: an open remembered container goes straight to its Containers detail. */
    public static StowScreen openForContainer(Screen parent){
        var active=ChestMemory.activeLocation();
        return open(parent,ItemStack.EMPTY,active!=null?Segment.CONTAINERS:null);
    }
    /** The container project strip: open Project with this goal selected. */
    public static StowScreen openGoal(Screen parent,String itemId){
        var session=StowSession.get(ChestMemory.currentWorldKey());session.selection.put(Segment.PROJECT,"goal:"+itemId);session.query.put(Segment.PROJECT,"");
        if(session.projectFilter==ProjectFilter.DONE)session.projectFilter=ProjectFilter.ALL;
        return open(parent,ItemStack.EMPTY,Segment.PROJECT);
    }
    public ChestMemoryStore store(){return store;}
    public Screen origin(){return parent;}
    private Segment segment(){return session.segment;}

    // ---------------------------------------------------------------- layout
    @Override protected void init(){
        w=Math.min(760,width-16);left=(width-w)/2;
        var mode=Stow.config.stowLayout;wide=mode==StowConfig.LayoutMode.WIDE?w>=420:mode==StowConfig.LayoutMode.AUTO&&w>=600;
        panelTop=68;panelBottom=height-30;
        listX=left;listW=wide?w*55/100:w;detailX=wide?listX+listW+6:left;detailW=wide?w-listW-6:w;
        query=new StowEditBox(font,left,24,w,20,Component.translatable("stow.ui.query.narration"));
        query.setMaxLength(120);query.setHint(queryHint(segment()));
        query.setValue(session.query.getOrDefault(segment(),""));
        query.setResponder(this::queryChanged);
        addRenderableWidget(query);setInitialFocus(query);
        int bw=18;
        var gear=addRenderableWidget(new PlannerButton(Component.empty(),b->minecraft.gui.setScreen(SettingsScreen.open(this)),left+w-bw,3,bw,18,false).icon(UIIcons.Kind.SETTINGS));
        gear.setTooltip(Tooltip.create(Component.translatable("stow.settings.title")));
        var help=addRenderableWidget(new PlannerButton(Component.literal("?"),b->togglePopup(Popup.HELP),left+w-bw*2-4,3,bw,18,false));
        help.setTooltip(Tooltip.create(Component.translatable("stow.ui.help.title")));
        refresh();
    }
    private void queryChanged(String value){session.query.put(segment(),value);historyIndex=-1;scroll=0;selected=-1;session.selection.remove(segment());refresh();}
    private static Component queryHint(Segment segment){return Component.translatable(segment==Segment.FIND?"stow.ui.query.hint":segment==Segment.PROJECT?"stow.ui.query.hint-project":"stow.ui.query.hint-containers");}
    private int contentBottom(){return panelBottom;}
    /** Rebuild the rows and every widget that depends on them; the query box is kept. */
    public void refresh(){
        if(minecraft==null||query==null)return;
        seenRevision=store==null?-1:store.revision();
        rows=buildRows();
        String want=session.selection.get(segment());
        selected=-1;
        if(want!=null)for(int i=0;i<rows.size();i++)if(rows.get(i).selectable()&&rows.get(i).key().equals(want)){selected=i;break;}
        if(selected<0&&(!query.getValue().isBlank()||segment()!=Segment.FIND))selected=firstSelectable(0,1);
        if(selected>=0)session.selection.put(segment(),rows.get(selected).key());
        if(!wide&&selected<0)detailOpen=false;
        listTop=panelTop+headerHeight();
        clampScroll();ensureVisible();rebuildDynamic();
    }
    private int headerHeight(){return switch(segment()){case FIND->0;case PROJECT->addMode?18:58;case CONTAINERS->24;};}
    private int firstSelectable(int from,int step){for(int i=from;i>=0&&i<rows.size();i+=step)if(rows.get(i).selectable())return i;return -1;}
    private Row current(){return selected>=0&&selected<rows.size()?rows.get(selected):null;}
    private void select(int index,boolean byKeyboard){
        if(index<0||index>=rows.size()||!rows.get(index).selectable())return;
        boolean changed=index!=selected;selected=index;keyboard=byKeyboard;session.selection.put(segment(),rows.get(index).key());
        ensureVisible();if(changed){inlineFor="";error="";narrate(rows.get(index));rebuildDynamic();}
    }
    private void narrate(Row row){
        var narrator=minecraft.getNarrator();if(!narrator.isActive())return;
        narrator.saySystemNow(Component.literal(row.title()+(row.sub().isEmpty()?"":", "+row.sub())+(row.right().isEmpty()?"":", "+row.right())));
    }
    private int rowY(int index){int y=0;for(int i=0;i<index;i++)y+=rows.get(i).height();return y;}
    private int totalHeight(){return rowY(rows.size());}
    private int listHeight(){return contentBottom()-listTop-2;}
    private void clampScroll(){scroll=Math.clamp(scroll,0,Math.max(0,totalHeight()-listHeight()));}
    private void ensureVisible(){if(selected<0)return;int y=rowY(selected),h=rows.get(selected).height();
        if(selected>0&&rows.get(selected-1).kind()==Kind.HEADER){y-=HEADER;h+=HEADER;}
        if(y<scroll)scroll=y;if(y+h>scroll+listHeight())scroll=y+h-listHeight();clampScroll();}
    private boolean showingDetail(){return wide||detailOpen;}
    private boolean showingList(){return wide||!detailOpen;}

    // ---------------------------------------------------------------- rows
    private List<Row> buildRows(){
        if(store==null)return List.of();
        return switch(segment()){case FIND->findRows();case PROJECT->addMode?addRows():projectRows();case CONTAINERS->containerRows();};
    }
    private List<Row> needRows(String text){
        var matcher=NEED.matcher(text.strip());if(!matcher.matches())return List.of();
        int amount;try{amount=Integer.parseInt(matcher.group(1));}catch(NumberFormatException e){return List.of();}
        if(amount<=0||amount>1_000_000)return List.of();
        var out=new ArrayList<Row>();var exact=MemoryItems.resolve(catalogue,matcher.group(2));
        var candidates=exact!=null?List.of(exact):MemoryItems.search(catalogue,matcher.group(2)).stream().limit(6).toList();
        if(candidates.isEmpty())return List.of();
        out.add(header(Component.translatable("stow.ui.find.group.add",store.projectName()).getString()));
        for(var entry:candidates)out.add(new Row(Kind.NEED,"need:"+entry.id(),Component.translatable("stow.ui.find.need",number(amount),entry.name()).getString(),
            exact!=null?Component.translatable("stow.ui.find.need-enter").getString():Component.translatable("stow.ui.find.need-pick").getString(),MUTED,"",MUTED,MemoryItems.icon(entry.id()),new Object[]{entry.id(),amount}));
        return out;
    }
    private static Row header(String text){return new Row(Kind.HEADER,"h:"+text,text,"",MUTED,"",MUTED,ItemStack.EMPTY,null);}
    private Row more(String group,int total){return new Row(Kind.MORE,"more:"+group,Component.translatable("stow.ui.find.show-all",total).getString(),"",MUTED,"",MUTED,ItemStack.EMPTY,group);}
    private record Hit(String id,String name,int rank,long known,StowIndex.ItemStat stat) {}
    private List<Row> findRows(){
        String raw=query.getValue().strip();if(raw.isEmpty())return List.of();
        var out=new ArrayList<Row>();var index=StowIndex.of(store);
        boolean actionsOnly=raw.startsWith(">"),projectsOnly=raw.startsWith("#");
        String text=actionsOnly||projectsOnly?raw.substring(1).strip():raw;
        if(!actionsOnly&&!projectsOnly){
            out.addAll(needRows(raw));
            var parsed=TextQuery.of(text);var lower=text.toLowerCase(Locale.ROOT);
            var hits=new ArrayList<Hit>();
            for(var stat:index.items()){String name=MemoryItems.name(stat.id());boolean match=parsed.matches(name,stat.id(),true);
                if(!match)for(String alias:stat.names())if(parsed.matches(alias,stat.id(),true)){match=true;break;}
                if(match)hits.add(new Hit(stat.id(),name,rank(name,stat.id(),lower),stat.stored()+StowIndex.carried(stat.id()),stat));}
            if(text.length()>=2)for(var entry:MemoryItems.search(catalogue,text)){if(index.item(entry.id())!=null)continue;hits.add(new Hit(entry.id(),entry.name(),rank(entry.name(),entry.id(),lower)+10,StowIndex.carried(entry.id()),null));if(hits.size()>400)break;}
            hits.sort(Comparator.comparingInt(Hit::rank).thenComparing(Comparator.comparingLong(Hit::known).reversed()).thenComparing(Hit::name));
            if(!hits.isEmpty()){
                out.add(header(Component.translatable("stow.ui.find.group.items").getString()));
                int cap=expanded.contains("items")?60:5;
                for(var hit:hits.stream().limit(cap).toList())out.add(itemRow(hit.id(),hit.name(),hit.stat(),index));
                if(hits.size()>cap&&cap<60)out.add(more("items",hits.size()));
            }
            var containers=store.chests().stream().filter(c->parsed.matches(store.displayName(c),"stow:container",false)).sorted(StowIndex.nearestFirst()).toList();
            if(!containers.isEmpty()){
                out.add(header(Component.translatable("stow.ui.find.group.containers").getString()));
                int cap=expanded.contains("containers")?40:3;
                for(var chest:containers.stream().limit(cap).toList())out.add(containerRow(chest));
                if(containers.size()>cap&&cap<40)out.add(more("containers",containers.size()));
            }
        }
        if(!projectsOnly){
            var actions=StowActions.all(this).stream().filter(a->StowActions.matches(a,text)).toList();
            if(!actions.isEmpty()&&(actionsOnly||!text.isEmpty())){
                out.add(header(Component.translatable("stow.ui.find.group.actions").getString()));
                int cap=actionsOnly||expanded.contains("actions")?40:3;
                for(var action:actions.stream().limit(cap).toList())out.add(new Row(Kind.ACTION,"action:"+action.id(),action.title().getString(),action.enabled()?"":action.reason()==null?"":action.reason().getString(),OCHRE,action.shortcutLabel(),DIM,action.icon(),action));
                if(actions.size()>cap&&cap<40)out.add(more("actions",actions.size()));
            }
        }
        if(!actionsOnly){
            var projects=store.projects().stream().filter(p->text.isEmpty()||p.name().toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT))).toList();
            if(!projects.isEmpty()&&(projectsOnly||!text.isEmpty())){
                out.add(header(Component.translatable("stow.ui.find.group.projects").getString()));
                for(var project:projects.stream().limit(projectsOnly?16:2).toList())out.add(new Row(Kind.PROJECT,"project:"+project.id(),project.name(),project.id().equals(store.activeProjectId())?Component.translatable("stow.ui.project.active").getString():Component.translatable("stow.ui.find.switch-hint").getString(),MUTED,"",MUTED,MemoryItems.icon("minecraft:map"),project));
            }
        }
        return out;
    }
    private static int rank(String name,String id,String query){
        String n=name.toLowerCase(Locale.ROOT),path=id.substring(id.indexOf(':')+1);
        if(n.equals(query)||id.equals(query)||path.equals(query.replace(' ','_')))return 0;
        if(n.startsWith(query))return 1;
        for(String word:n.split("\\s+"))if(word.startsWith(query))return 2;
        return 3;
    }
    private Row itemRow(String id,String name,StowIndex.ItemStat stat,StowIndex index){
        long carried=StowIndex.carried(id),stored=stat==null?0:stat.stored();
        String sub;int color=MUTED;
        if(stat==null)sub=Component.translatable("stow.ui.find.not-stored",number(carried)).getString();
        else{
            var nearest=index.nearest(id);double d=nearest==null?-1:StowIndex.distance(nearest);
            sub=Component.translatable("stow.ui.find.item-sub",number(stored+carried),stat.sources().size(),distance(d)).getString();
        }
        var goal=store.goal(id);
        if(goal!=null){var progress=MaterialPlanner.progress(store,goal);if(!progress.enough()){sub=Component.translatable("stow.ui.find.missing-for",store.projectName(),number(progress.missing())).getString();color=OCHRE;}}
        String right=stat==null?"":ChestMemoryStore.age(stat.newest());int rightColor=stat!=null&&System.currentTimeMillis()-stat.newest()>=StowIndex.STALE_MS?OCHRE:DIM;
        return new Row(stat==null?Kind.CATALOGUE:Kind.ITEM,"item:"+id,name,sub,color,right,rightColor,MemoryItems.icon(id),id);
    }
    private Row containerRow(SavedChest chest){
        var loc=chest.location();double d=StowIndex.distance(chest);
        String where=loc.x()+" "+loc.y()+" "+loc.z()+(d>=0?" · "+distance(d):" · "+shortDimension(loc.dimension()));
        boolean stale=StowIndex.stale(chest);
        return new Row(Kind.CONTAINER,"container:"+loc.key(),store.displayName(chest),where,MUTED,ChestMemoryStore.age(chest.lastSeen()),stale?OCHRE:DIM,MemoryItems.storageIcon(chest),chest);
    }
    private static String shortDimension(String id){return id.substring(id.indexOf(':')+1).replace('_',' ');}
    private List<Row> projectRows(){
        var out=new ArrayList<Row>();String text=query.getValue().strip();
        out.addAll(needRows(text));
        var parsed=TextQuery.of(NEED.matcher(text).matches()?"":text);
        for(var goal:store.goals()){
            String name=MemoryItems.name(goal.itemId());if(!parsed.matches(name,goal.itemId(),true))continue;
            var progress=MaterialPlanner.progress(store,goal);
            boolean done=progress.enough();
            if(session.projectFilter==ProjectFilter.MISSING&&done||session.projectFilter==ProjectFilter.DONE&&!done)continue;
            out.add(new Row(Kind.GOAL,"goal:"+goal.itemId(),name,"",MUTED,"",MUTED,MemoryItems.icon(goal.itemId()),new Object[]{goal,progress}));
        }
        return out;
    }
    private List<Row> addRows(){
        String text=query.getValue().strip();if(text.isEmpty())return List.of();
        var out=new ArrayList<Row>();
        for(var entry:MemoryItems.search(catalogue,text).stream().limit(80).toList()){
            var goal=store.goal(entry.id());
            out.add(new Row(Kind.CATALOGUE,"add:"+entry.id(),entry.name(),goal==null?entry.id():Component.translatable("stow.ui.project.already",number(goal.target())).getString(),goal==null?DIM:SAGE,"",MUTED,MemoryItems.icon(entry.id()),entry.id()));
        }
        return out;
    }
    private List<Row> containerRows(){
        var parsed=TextQuery.of(query.getValue().strip());var out=new ArrayList<Row>();
        var list=new ArrayList<SavedChest>();
        for(var chest:store.chests()){
            boolean match=parsed.matches(store.displayName(chest),"stow:container",false)||chest.items().stream().anyMatch(i->parsed.matches(i.name(),i.id(),true));
            if(!match)continue;
            switch(session.containerFilter){
                case IN_PROJECT->{if(!store.isIncluded(chest.location()))continue;}
                case NAMED->{if(store.customName(chest.location()).isEmpty())continue;}
                case STALE->{if(!StowIndex.stale(chest))continue;}
                default->{}
            }
            if(!session.ownerFilter.isEmpty()&&!StorageOwners.matches(chest,session.ownerFilter))continue;
            list.add(chest);
        }
        Comparator<SavedChest> order=switch(session.containerSort){
            case NAME->Comparator.comparing((SavedChest c)->store.displayName(c),String.CASE_INSENSITIVE_ORDER);
            case LAST_SEEN->Comparator.comparingLong(SavedChest::lastSeen).reversed();
            default->StowIndex.nearestFirst();
        };
        list.sort(order);
        for(var chest:list)out.add(containerRow(chest));
        return out;
    }

    // ---------------------------------------------------------------- widgets
    private <T extends AbstractWidget> T dyn(T widget){dynamic.add(addRenderableWidget(widget));return widget;}
    private PlannerButton button(Component label,Button.OnPress press,int x,int y,int width,boolean primary){return dyn(new PlannerButton(label,press,x,y,width,20,primary));}
    private int fitWidth(Component label,int min){return Math.max(min,font.width(label)+14);}
    private void rebuildDynamic(){
        var focused=getFocused();boolean inlineFocused=inline!=null&&focused==inline;String inlineValue=inline==null?"":inline.getValue();String oldInlineFor=inlineFor;
        boolean nameFocused=projectName!=null&&focused==projectName;String nameValue=projectName==null?"":projectName.getValue();
        dynamic.forEach(this::removeWidget);dynamic.clear();inline=null;
        popupWidgets.forEach(this::removeWidget);popupWidgets.clear();projectName=null;createButton=null;
        segmentButtons();footer();
        switch(segment()){case PROJECT->projectHeader();case CONTAINERS->containerHeader();default->{}}
        if(segment()==Segment.FIND&&query.getValue().isBlank()&&store!=null)nowWidgets();
        else if(showingDetail()&&store!=null)detailWidgets();
        if(inline!=null&&inlineFocused&&oldInlineFor.equals(inlineFor))inline.setValue(inlineValue);
        if(popup!=Popup.NONE)popupContents();
        if(projectName!=null){projectName.setValue(nameValue);if(nameFocused||popup==Popup.PROJECTS)setFocused(projectName);}
        else if(inline!=null&&inlineFocused&&oldInlineFor.equals(inlineFor))setFocused(inline);
        else if(focused==null||!children().contains(focused))setFocused(query);
    }
    private void segmentButtons(){
        int x=left,y=48;
        for(var seg:Segment.values()){
            Component label=segmentLabel(seg);int bw=fitWidth(label,60);
            var b=dyn(new PlannerButton(label,btn->switchSegment(seg),x,y,bw,18,false).selected(()->segment()==seg));
            b.setTooltip(Tooltip.create(Component.translatable("stow.ui.segment.tip",Component.literal("Ctrl+"+(seg.ordinal()+1)))));
            x+=bw+4;
        }
        message=store==null?"":store.projectName()+" · "+Component.translatable("stow.ui.context.chests",store.includedChestCount()).getString();
    }
    private Component segmentLabel(Segment seg){
        if(store==null)return Component.translatable("stow.ui.segment."+seg.name().toLowerCase(Locale.ROOT));
        return switch(seg){
            case FIND->Component.translatable("stow.ui.segment.find");
            case PROJECT->{long missing=store.goals().stream().filter(g->!MaterialPlanner.progress(store,g).enough()).count();
                yield missing>0?Component.translatable(wide?"stow.ui.segment.project-missing":"stow.ui.segment.project-short",number(missing)):Component.translatable("stow.ui.segment.project");}
            case CONTAINERS->Component.translatable(wide?"stow.ui.segment.containers-count":"stow.ui.segment.containers-short",number(store.size()));
        };
    }
    private void footer(){
        int y=height-25,x=left;
        var done=button(Component.translatable("gui.done"),b->onClose(),x,y,fitWidth(Component.translatable("gui.done"),50),false);x+=done.getWidth()+4;
        if(store!=null&&store.canUndo()){
            Component label=Component.translatable("stow.ui.undo",undoName());
            var undo=button(label,b->undo(),x,y,Math.min(fitWidth(label,60),Math.max(60,w/3)),false);undo.setTooltip(Tooltip.create(Component.translatable("stow.ui.undo.tip")));x+=undo.getWidth()+4;
        }
        if(!wide&&detailOpen){Component back=Component.translatable("stow.ui.back");int bw=fitWidth(back,50);button(back,b->closeDetail(),left+w-bw,y,bw,false);}
        if(segment()==Segment.PROJECT&&store!=null&&!addMode&&(wide||!detailOpen)){
            boolean finished=store.isFinished(),can=MaterialPlanner.canFinish(store);
            Component label=Component.translatable(finished?"stow.need.reopen":"stow.need.finish");
            var finish=button(label,b->{store.setFinished(!store.isFinished());ChestMemory.saveLater(store);UiNotifications.show(store.isFinished()?"stow.ui.feedback.finished":"stow.ui.feedback.reopened",store.projectName());refresh();},x,y,fitWidth(label,56),false);
            finish.active=finished||can;finish.setTooltip(Tooltip.create(Component.translatable(finished||can?"stow.ui.project.finish-tip":"stow.ui.project.finish-locked")));
        }
    }
    public String undoName(){
        if(store==null||!store.canUndo())return "";String label=store.undoLabel();
        if(label.startsWith("Remove "))return Component.translatable("stow.ui.undo.remove",MemoryItems.name(label.substring(7))).getString();
        if(label.startsWith("Forget "))return Component.translatable("stow.ui.undo.forget",label.substring(7)).getString();
        if(label.startsWith("Delete "))return Component.translatable("stow.ui.undo.delete",label.substring(7)).getString();
        if(label.equals("Import materials"))return Component.translatable("stow.ui.undo.import").getString();
        if(label.equals("Finish project"))return Component.translatable("stow.ui.undo.finish").getString();
        if(label.equals("Reopen project"))return Component.translatable("stow.ui.undo.reopen").getString();
        return label;
    }
    public void undo(){if(store!=null&&store.undo()){ChestMemory.saveLater(store);UiNotifications.show("stow.feedback.undo");refresh();}}
    public void copyList(){if(store==null)return;minecraft.keyboardHandler.setClipboard(MaterialListExporter.export(store.projectName(),store.goals()));UiNotifications.show("stow.export.copied",store.goals().size());}
    private void switchSegment(Segment seg){
        if(seg==segment()){refresh();return;}
        session.segment=seg;addMode=false;detailOpen=false;scroll=0;popup=Popup.NONE;inlineFor="";error="";
        query.setResponder(v->{});query.setValue(session.query.getOrDefault(seg,""));query.setResponder(this::queryChanged);
        query.setHint(queryHint(seg));setFocused(query);refresh();
    }
    private void projectHeader(){
        int y=panelTop+4,x=listX+4;
        if(addMode){
            Component done=Component.translatable("stow.ui.project.add-done");
            button(done,b->{addMode=false;query.setValue("");refresh();},listX+listW-fitWidth(done,50)-4,panelTop,fitWidth(done,50),false);
            return;
        }
        Component name=Component.translatable("stow.ui.project.switcher",store.projectName());
        int nameW=Math.min(fitWidth(name,80),listW/2);
        var switcher=button(name,b->togglePopup(Popup.PROJECTS),x,y,nameW,false);switcher.setTooltip(Tooltip.create(Component.translatable("stow.ui.project.switcher-tip")));x+=nameW+4;
        Component scope=Component.translatable(store.usesAllChests()?"stow.ui.project.scope-all":"stow.ui.project.scope",number(store.includedChestCount()));
        int scopeW=Math.min(fitWidth(scope,80),Math.max(80,listW-nameW-12));
        button(scope,b->togglePopup(Popup.SCOPE),x,y,scopeW,false).setTooltip(Tooltip.create(Component.translatable("stow.ui.project.scope-tip")));
        y+=24;x=listX+4;
        long missing=0,done=0,all=store.goals().size();for(var g:store.goals())if(MaterialPlanner.progress(store,g).enough())done++;else missing++;
        for(var filter:ProjectFilter.values()){
            long n=filter==ProjectFilter.MISSING?missing:filter==ProjectFilter.DONE?done:all;
            Component label=Component.translatable("stow.ui.project.chip."+filter.name().toLowerCase(Locale.ROOT),number(n));int bw=fitWidth(label,40);
            dyn(new PlannerButton(label,b->{session.projectFilter=filter;refresh();},x,y,bw,18,false).selected(()->session.projectFilter==filter));x+=bw+3;
        }
        Component add=Component.translatable("stow.ui.project.add"),imp=Component.translatable(wide?"stow.import.title":"stow.ui.import.short");
        int right=listX+listW-4;int iw=fitWidth(imp,30),aw=fitWidth(add,30);
        if(right-iw-aw-4>=x){
            dyn(new PlannerButton(imp,b->minecraft.gui.setScreen(new MaterialImportScreen(this,store)),right-iw,y,iw,18,false)).setTooltip(Tooltip.create(Component.translatable("stow.ui.project.import-tip")));
            dyn(new PlannerButton(add,b->{addMode=true;query.setValue("");setFocused(query);refresh();},right-iw-aw-3,y,aw,18,false)).setTooltip(Tooltip.create(Component.translatable("stow.ui.project.add-tip")));
        }
    }
    private void containerHeader(){
        int y=panelTop+2,x=listX+4;
        Component sort=Component.translatable("stow.ui.containers.sort."+session.containerSort.name().toLowerCase(Locale.ROOT));int sw=fitWidth(sort,40);
        for(var filter:ContainerFilter.values()){
            Component label=Component.translatable("stow.ui.containers.chip."+filter.name().toLowerCase(Locale.ROOT));int bw=fitWidth(label,30);
            if(x+bw>listX+listW-sw-8)break;
            dyn(new PlannerButton(label,b->{session.containerFilter=filter;scroll=0;refresh();},x,y,bw,18,false).selected(()->session.containerFilter==filter));x+=bw+3;
        }
        var sortButton=dyn(new PlannerButton(sort,b->{session.containerSort=ContainerSort.values()[(session.containerSort.ordinal()+1)%ContainerSort.values().length];refresh();},listX+listW-sw-4,y,sw,18,false));
        sortButton.setTooltip(Tooltip.create(Component.translatable("stow.ui.containers.sort-tip")));
        if(StorageOwners.available()||!store.ownerChoices().isEmpty()){
            var owners=ownerChoices();Component owner=Component.translatable("stow.owner.filter",owners.getOrDefault(session.ownerFilter.isEmpty()?"all":session.ownerFilter,"?"));int ow=fitWidth(owner,40);
            if(x+ow<listX+listW-sw-8)dyn(new PlannerButton(owner,b->{var keys=new ArrayList<>(owners.keySet());String cur=session.ownerFilter.isEmpty()?"all":session.ownerFilter;String next=keys.get((keys.indexOf(cur)+1)%keys.size());session.ownerFilter=next.equals("all")?"":next;refresh();},x,y,ow,18,false)).setTooltip(Tooltip.create(Component.translatable("stow.owner.filter-hint")));
        }
    }
    private Map<String,String> ownerChoices(){
        var choices=new LinkedHashMap<String,String>();for(String key:List.of("all","mine","shared","unknown","mixed"))choices.put(key,Component.translatable("stow.owner."+key).getString());
        choices.putAll(store.ownerChoices());return choices;
    }

    // ---------------------------------------------------------------- now view and detail widgets
    private int nowRecentY,nowToggleY;
    private int nowCol(){return wide?w/2:w;}
    private void nowWidgets(){
        int x=left+8,col=nowCol(),cardTop=panelTop+17;
        Component open=Component.translatable(store.goals().isEmpty()?"stow.ui.now.add-first":"stow.ui.now.open-project");int ow=fitWidth(open,60);
        var openButton=button(open,b->{switchSegment(Segment.PROJECT);if(store.goals().isEmpty()){addMode=true;refresh();}},left+col-8-ow-4,cardTop+7,ow,false);openButton.setHeight(16);
        long stale=store.chests().stream().filter(StowIndex::stale).count();
        if(stale>0){Component review=Component.translatable("stow.ui.now.review");int rw=fitWidth(review,50);var r=button(review,b->{session.containerFilter=ContainerFilter.STALE;switchSegment(Segment.CONTAINERS);},left+col-8-rw,panelTop+52,rw,false);r.setHeight(16);}
        nowRecentY=panelTop+76;int y=nowRecentY+10,bx=x;
        for(String id:session.recentItems){
            if(bx+20>left+col-8)break;String item=id;
            var b=dyn(new PlannerButton(Component.empty(),btn->query.setValue(MemoryItems.name(item)),bx,y,20,20,false).item(item));
            b.setTooltip(Tooltip.create(Component.literal(MemoryItems.name(item))));bx+=22;
        }
        nowToggleY=y+30;y=nowToggleY+10;bx=x;
        for(var entry:StowActions.all(this)){
            if(!List.of("glow","stop-glow","drag","dock","auto_tool").contains(entry.id()))continue;
            Component label=entry.title();int bw=fitWidth(label,50);
            if(bx+bw>left+col-8){bx=x;y+=22;}
            var b=button(label,btn->entry.run().run(),bx,y,bw,false);b.active=entry.enabled();bx+=bw+4;
        }
    }
    private void detailWidgets(){
        var row=current();if(row==null)return;
        int x=detailX+8,right=detailX+detailW-8,y=contentBottom()-26,bw=(right-x-8)/3;
        switch(row.kind()){
            case ITEM,CATALOGUE->{String id=(String)row.data();var index=StowIndex.of(store);var nearest=index.nearest(id);
                var show=button(Component.translatable("stow.ui.find.show-nearest"),b->{ChestMemory.show(store,nearest,id);session.recentItem(id);StowFind.closeAll();},x,y,bw,true);
                show.active=nearest!=null;show.setTooltip(Tooltip.create(Component.translatable(nearest!=null?"stow.ui.find.show-tip":"stow.ui.find.no-source")));
                boolean has=store.hasGoal(id);
                button(Component.translatable(has?"stow.ui.find.open-goal":"stow.ui.find.add-goal"),b->{if(!has){store.setGoal(id,Math.max(1,(int)Math.min(64,Math.max(1,StowIndex.carried(id)))));ChestMemory.saveLater(store);UiNotifications.show("stow.ui.feedback.added",MemoryItems.name(id),store.projectName());}
                    session.selection.put(Segment.PROJECT,"goal:"+id);session.projectFilter=ProjectFilter.ALL;session.query.put(Segment.PROJECT,"");switchSegment(Segment.PROJECT);},x+bw+4,y,bw,false);
                var copy=button(Component.translatable("stow.ui.find.copy-id"),b->{minecraft.keyboardHandler.setClipboard(id);UiNotifications.show("stow.ui.feedback.copied",id);},x+bw*2+8,y,bw,false);
                copy.setTooltip(Tooltip.create(Component.literal(id)));
            }
            case CONTAINER->containerDetailWidgets((SavedChest)row.data(),x,right,y);
            case ACTION->{var action=(StowActions.Entry)row.data();var run=button(Component.translatable("stow.ui.find.run"),b->runAction(action),x,y,Math.min(120,right-x),true);run.active=action.enabled();}
            case PROJECT->{var project=(Project)row.data();boolean active=project.id().equals(store.activeProjectId());
                var sw=button(Component.translatable(active?"stow.ui.find.open-project":"stow.ui.find.switch"),b->{if(!active){store.switchProject(project.id());ChestMemory.saveLater(store);UiNotifications.show("stow.ui.feedback.switched",project.name());}switchSegment(Segment.PROJECT);},x,y,Math.min(140,right-x),true);sw.active=true;}
            case NEED->{var data=(Object[])row.data();button(Component.translatable("stow.ui.find.need-add"),b->addNeed((String)data[0],(int)data[1]),x,y,Math.min(140,right-x),true);}
            case GOAL->goalDetailWidgets((Object[])row.data(),x,right,y);
            case MORE->button(Component.translatable("stow.ui.find.expand"),b->expand((String)row.data()),x,y,Math.min(120,right-x),true);
            default->{}
        }
    }
    private void runAction(StowActions.Entry action){
        if(!action.enabled()){error=action.reason()==null?"":action.reason().getString();return;}
        session.remember(query.getValue());action.run().run();
        if(minecraft.gui.screen()==this)refresh();
    }
    private void expand(String group){expanded.add(group);refresh();}
    private void addNeed(String id,int amount){
        var existing=store.goal(id);store.setGoal(id,amount);ChestMemory.saveLater(store);session.remember(query.getValue());
        UiNotifications.show(existing==null?"stow.ui.feedback.need-set":"stow.ui.feedback.need-updated",number(amount),MemoryItems.name(id));
        query.setValue("");session.selection.put(Segment.PROJECT,"goal:"+id);if(segment()==Segment.PROJECT)refresh();
    }
    private void containerDetailWidgets(SavedChest chest,int x,int right,int y){
        var loc=chest.location();int bw=(right-x-12)/4;
        button(Component.translatable("stow.ui.containers.show"),b->{ChestMemory.select(loc);session.recentContainer(loc.key());StowFind.closeAll();},x,y,bw,true);
        boolean included=store.isIncluded(loc);
        button(Component.translatable(included?"stow.ui.containers.exclude":"stow.ui.containers.include"),b->{store.toggleIncluded(loc);ChestMemory.saveLater(store);UiNotifications.show(store.isIncluded(loc)?"stow.ui.feedback.included":"stow.ui.feedback.excluded",store.displayName(chest),store.projectName());refresh();},x+bw+4,y,bw,false)
            .setTooltip(Tooltip.create(Component.translatable("stow.ui.containers.include-tip",store.projectName())));
        button(Component.translatable("stow.ui.containers.forget"),b->{String name=store.displayName(chest);ChestMemory.forget(store,loc);UiNotifications.show("stow.ui.feedback.forgot",name);detailOpen=false;refresh();},x+bw*2+8,y,bw,false)
            .setTooltip(Tooltip.create(Component.translatable("stow.ui.containers.forget-tip")));
        var claim=button(Component.translatable("stow.ui.containers.claim-short"),b->minecraft.gui.setScreen(dev.stow.client.memory.AreaClaimScreen.direct(this,store)),x+bw*3+12,y,bw,false);
        claim.active=AreaClaims.available();claim.setTooltip(Tooltip.create(Component.translatable(AreaClaims.available()?"stow.ui.containers.claim-tip":"stow.owner.requires-companion")));
        // rename inline at the top of the detail
        int ty=panelTop+6;
        inline=dyn(new StowEditBox(font,x,ty,right-x-44,18,Component.translatable("stow.ui.containers.rename")));inlineFor="rename:"+loc.key();
        inline.setMaxLength(48);inline.setValue(store.customName(loc));inline.setHint(Component.literal(chest.title()));
        button(Component.translatable("stow.ui.save"),b->renameInline(chest),right-40,ty-1,40,false);
        if(StorageOwners.editable(loc)){
            int oy=y-24;int ow=(right-x-8)/3;
            button(Component.translatable("stow.owner.claim"),b->StorageOwners.request(store,loc,dev.stow.companion.protocol.StorageOwnerProtocol.CLAIM),x,oy,ow,false);
            button(Component.translatable("stow.owner.share"),b->StorageOwners.request(store,loc,dev.stow.companion.protocol.StorageOwnerProtocol.SHARE),x+ow+4,oy,ow,false);
            button(Component.translatable("stow.owner.private"),b->StorageOwners.request(store,loc,dev.stow.companion.protocol.StorageOwnerProtocol.PRIVATE),x+ow*2+8,oy,ow,false);
        }
    }
    private void renameInline(SavedChest chest){
        if(inline==null)return;String value=inline.getValue().strip();
        store.rename(chest.location(),value);ChestMemory.saveLater(store);
        UiNotifications.show(value.isEmpty()?"stow.ui.feedback.name-cleared":"stow.ui.feedback.renamed",value.isEmpty()?chest.title():value);setFocused(query);refresh();
    }
    private void goalDetailWidgets(Object[] data,int x,int right,int y){
        var goal=(MaterialGoal)data[0];String id=goal.itemId();int bw=(right-x-8)/3;
        int ty=panelTop+30;
        inline=dyn(new StowEditBox(font,x+60,ty,70,18,Component.translatable("stow.ui.project.need")));inlineFor="need:"+id;
        inline.setMaxLength(7);inline.setValue(Integer.toString(goal.target()));var box=inline;inline.setResponder(s->{String digits=s.replaceAll("[^0-9]","");if(!digits.equals(s))box.setValue(digits);});
        button(Component.translatable("stow.ui.save"),b->saveNeed(id),x+134,ty-1,40,false);
        for(int i=0;i<3;i++){int step=new int[]{1,64,1728}[i];String sym="+"+(i==0?"1":i==1?"64":"1 sh");
            button(Component.literal(sym),b->{store.updateTarget(id,(int)Math.min(1_000_000,(long)store.goal(id).target()+step));ChestMemory.saveLater(store);refresh();},x+178+i*(Math.max(30,(right-x-178)/3)),ty-1,Math.max(28,(right-x-178)/3-2),false);}
        var index=StowIndex.of(store);var nearest=index.nearest(id);
        var show=button(Component.translatable("stow.ui.project.show-sources"),b->{ChestMemory.show(store,nearest,id);StowFind.closeAll();},x,y,bw,true);show.active=nearest!=null;
        boolean hud=store.isHudGoal(id,Integer.MAX_VALUE);
        button(Component.translatable(hud?"stow.ui.project.hud-off":"stow.ui.project.hud-on"),b->{MaterialPlanner.toggleHud(store,id);ChestMemory.saveLater(store);refresh();},x+bw+4,y,bw,false)
            .setTooltip(Tooltip.create(Component.translatable("stow.ui.project.hud-tip")));
        button(Component.translatable("stow.ui.project.remove"),b->{store.removeGoal(id);ChestMemory.saveLater(store);UiNotifications.show("stow.ui.feedback.removed",MemoryItems.name(id));refresh();},x+bw*2+8,y,bw,false)
            .setTooltip(Tooltip.create(Component.translatable("stow.ui.project.remove-tip")));
        boolean visible=store.isGoalVisible(id);
        button(Component.translatable(visible?"stow.ui.project.strip-hide":"stow.ui.project.strip-show"),b->{store.toggleGoalVisible(id);ChestMemory.saveLater(store);refresh();},x,y-24,bw*2+4,false)
            .setTooltip(Tooltip.create(Component.translatable("stow.ui.project.strip-tip")));
    }
    private void saveNeed(String id){
        if(inline==null)return;int value;try{value=Integer.parseInt(inline.getValue().strip());}catch(NumberFormatException e){error=Component.translatable("stow.ui.project.need-invalid").getString();return;}
        if(value<=0||value>1_000_000){error=Component.translatable("stow.ui.project.need-invalid").getString();return;}
        store.updateTarget(id,value);ChestMemory.saveLater(store);UiNotifications.show("stow.ui.feedback.need-updated",number(value),MemoryItems.name(id));error="";inlineFor="";setFocused(query);refresh();
    }

    // ---------------------------------------------------------------- popups
    public void openProjects(){if(segment()!=Segment.PROJECT)switchSegment(Segment.PROJECT);addMode=false;popup=Popup.PROJECTS;refresh();}
    private void togglePopup(Popup which){popup=popup==which?Popup.NONE:which;deleteArmed=0;error="";rebuildDynamic();}
    private <T extends AbstractWidget> T pop(T widget){popupWidgets.add(addRenderableWidget(widget));return widget;}
    private void popupContents(){
        popupW=Math.min(260,w-16);popupX=popup==Popup.HELP?left+w-popupW:listX+4;popupY=popup==Popup.HELP?24:panelTop+26;
        int x=popupX+6,y=popupY+6,iw=popupW-12;
        switch(popup){
            case PROJECTS->{
                for(var project:store.projects()){
                    boolean active=project.id().equals(store.activeProjectId());
                    pop(new PlannerButton(Component.literal(project.name()),b->{store.switchProject(project.id());ChestMemory.saveLater(store);popup=Popup.NONE;UiNotifications.show("stow.ui.feedback.switched",project.name());refresh();},x,y,iw,18,false).selected(()->active));y+=19;
                }
                y+=4;
                projectName=pop(new StowEditBox(font,x,y,iw,18,Component.translatable("stow.ui.project.name-hint")));projectName.setMaxLength(32);projectName.setHint(Component.translatable("stow.ui.project.name-hint"));y+=22;
                int bw=(iw-8)/3;
                createButton=pop(new PlannerButton(Component.translatable("stow.ui.project.new"),b->createProject(),x,y,bw,18,true));
                createButton.active=store.projects().size()<16;if(!createButton.active)createButton.setTooltip(Tooltip.create(Component.translatable("stow.ui.project.limit")));
                pop(new PlannerButton(Component.translatable("stow.ui.project.rename"),b->renameProject(),x+bw+4,y,bw,18,false));
                var delete=pop(new PlannerButton(Component.translatable(deleteArmed>System.currentTimeMillis()?"stow.ui.project.delete-confirm":"stow.ui.project.delete"),b->deleteProject(),x+bw*2+8,y,bw,18,false));
                delete.active=store.projects().size()>1;delete.setTooltip(Tooltip.create(Component.translatable(store.projects().size()>1?"stow.ui.project.delete-tip":"stow.ui.project.delete-last")));
                y+=22;
            }
            case SCOPE->{
                pop(new PlannerButton(Component.translatable("stow.ui.project.scope-use-all",number(store.size())),b->{store.selectAllChests(true);ChestMemory.saveLater(store);popup=Popup.NONE;refresh();},x,y,iw,18,false).selected(store::usesAllChests));y+=20;
                pop(new PlannerButton(Component.translatable("stow.ui.project.scope-use-chosen",number(store.usesAllChests()?0:store.includedChestCount())),b->{if(store.usesAllChests()){store.selectAllChests(false);ChestMemory.saveLater(store);}popup=Popup.NONE;refresh();},x,y,iw,18,false).selected(()->!store.usesAllChests()));y+=20;
                pop(new PlannerButton(Component.translatable("stow.ui.project.scope-choose"),b->{popup=Popup.NONE;session.containerFilter=ContainerFilter.ALL;switchSegment(Segment.CONTAINERS);},x,y,iw,18,false));y+=20;
            }
            case HELP->y+=10*12;
            default->{}
        }
        popupH=y-popupY+(error.isEmpty()?2:12);
    }
    private void createProject(){
        String name=projectName==null?"":projectName.getValue().strip();
        if(name.isEmpty())name=Component.translatable("stow.ui.project.default-name",store.projects().size()+1).getString();
        try{store.createProject(name);ChestMemory.saveLater(store);popup=Popup.NONE;UiNotifications.show("stow.ui.feedback.created",store.projectName());refresh();}
        catch(IllegalArgumentException e){error=e.getMessage();rebuildDynamic();}
    }
    private void renameProject(){
        String name=projectName==null?"":projectName.getValue().strip();
        if(name.isEmpty()){error=Component.translatable("stow.ui.project.name-hint").getString();rebuildDynamic();return;}
        try{store.renameProject(name);ChestMemory.saveLater(store);popup=Popup.NONE;UiNotifications.show("stow.ui.feedback.renamed",store.projectName());refresh();}
        catch(IllegalArgumentException e){error=e.getMessage();rebuildDynamic();}
    }
    private void deleteProject(){
        if(store.projects().size()<=1)return;
        if(deleteArmed<System.currentTimeMillis()){deleteArmed=System.currentTimeMillis()+4000;rebuildDynamic();return;}
        String name=store.projectName();store.deleteProject();ChestMemory.saveLater(store);deleteArmed=0;popup=Popup.NONE;
        UiNotifications.show("stow.ui.feedback.deleted",name);refresh();
    }

    // ---------------------------------------------------------------- rendering
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        lastMouseX=mx;lastMouseY=my;
        if(minecraft.level==null)extractPanorama(g,delta);
        extractBlurredBackground(g);extractMenuBackground(g);g.fill(0,0,width,height,0x50000000);
        boolean popupOpen=popup!=Popup.NONE;int bx=popupOpen&&inside(mx,my,popupX,popupY,popupW,popupH)?Integer.MIN_VALUE:mx,by=popupOpen&&inside(mx,my,popupX,popupY,popupW,popupH)?Integer.MIN_VALUE:my;
        g.text(font,title,left,8,CREAM);
        if(store!=null){String context=fit(message,w-font.width(title)-60);g.text(font,context,left+font.width(title)+8,8,MUTED);}
        int segRight=left;for(var child:dynamic)if(child.getY()==48)segRight=Math.max(segRight,child.getRight());
        if(!error.isEmpty())right(g,fit(error,left+w-segRight-8),left+w,53,CLAY);
        if(store==null){panel(g,left,panelTop,w,contentBottom()-panelTop);g.centeredText(font,Component.translatable("stow.ui.no-world"),width/2,panelTop+30,MUTED);}
        else if(segment()==Segment.FIND&&query.getValue().isBlank())drawNow(g);
        else{
            if(showingList())drawList(g,bx,by);
            if(showingDetail())drawDetail(g);
        }
        for(var child:children())if(child instanceof AbstractWidget widget&&!popupWidgets.contains(widget))widget.extractRenderState(g,bx,by,delta);
        if(popupOpen){
            g.nextStratum();
            g.fill(popupX,popupY,popupX+popupW,popupY+popupH,0xF02E3033);g.outline(popupX,popupY,popupW,popupH,GOLD);
            if(popup==Popup.HELP)drawHelp(g);
            if(!error.isEmpty())text(g,error,popupX+6,popupY+popupH-11,popupW-12,CLAY);
            for(var widget:popupWidgets)widget.extractRenderState(g,mx,my,delta);
        }
    }
    private void drawHelp(GuiGraphicsExtractor g){
        int x=popupX+6,y=popupY+6;
        section(g,Component.translatable("stow.ui.help.title").getString(),x,y);y+=10;
        for(int i=1;i<=9;i++){text(g,Component.translatable("stow.ui.help."+i).getString(),x,y,popupW-12,i%2==0?MUTED:CREAM);y+=12;}
    }
    private void drawList(GuiGraphicsExtractor g,int mx,int my){
        int bottom=contentBottom();panel(g,listX,panelTop,listW,bottom-panelTop);
        if(segment()==Segment.PROJECT&&!addMode){
            int y=listTop-10;int cols=listX+listW-8;
            if(wide){right(g,Component.translatable("stow.ui.project.col.src").getString(),cols-22,y,DIM);right(g,Component.translatable("stow.ui.project.col.missing").getString(),cols-56,y,DIM);
                right(g,Component.translatable("stow.ui.project.col.have").getString(),cols-110,y,DIM);right(g,Component.translatable("stow.ui.project.col.need").getString(),cols-160,y,DIM);}
            g.horizontalLine(listX+2,listX+listW-3,listTop-1,LINE);
        }
        if(rows.isEmpty()){drawEmpty(g);return;}
        g.enableScissor(listX+1,listTop,listX+listW-1,bottom-1);
        int y=listTop-scroll;
        for(int i=0;i<rows.size();i++){
            var row=rows.get(i);int h=row.height();
            if(y+h>=listTop&&y<bottom)drawRow(g,row,listX+3,y,listW-10,i==selected,inside(mx,my,listX,Math.max(listTop,y),listW,h)&&my<bottom&&row.selectable(),keyboard&&i==selected);
            y+=h;
        }
        g.disableScissor();
        int total=totalHeight(),visible=listHeight();
        if(total>visible){int track=bottom-listTop-2,thumb=Math.max(12,track*visible/total),ty=listTop+(track-thumb)*scroll/Math.max(1,total-visible);
            g.fill(listX+listW-4,listTop,listX+listW-2,bottom-2,0xFF383D3F);g.fill(listX+listW-4,ty,listX+listW-2,ty+thumb,MUTED);}
    }
    private void drawEmpty(GuiGraphicsExtractor g){
        int cx=listX+listW/2,y=listTop+24;String key=switch(segment()){
            case FIND->"stow.ui.find.empty";
            case PROJECT->addMode?(query.getValue().isBlank()?"stow.ui.project.add-empty":"stow.ui.find.empty"):store.goals().isEmpty()?"stow.ui.project.empty":session.projectFilter==ProjectFilter.MISSING?"stow.ui.project.all-done":"stow.ui.find.empty";
            case CONTAINERS->store.size()==0?"stow.ui.containers.empty":"stow.ui.find.empty";
        };
        for(var line:font.split(Component.translatable(key),listW-24)){g.centeredText(font,line,cx,y,MUTED);y+=11;}
        if(segment()==Segment.FIND){y+=6;for(int i=1;i<=3;i++){g.centeredText(font,Component.translatable("stow.ui.now.try."+i),cx,y,DIM);y+=11;}}
    }
    private void drawRow(GuiGraphicsExtractor g,Row row,int x,int y,int w,boolean sel,boolean hover,boolean ring){
        if(row.kind()==Kind.HEADER){section(g,row.title(),x+4,y+5);return;}
        row(g,x,y,w,ROW,sel,hover,ring);
        int tx=x+24;
        if(!row.icon().isEmpty())g.item(row.icon(),x+4,y+3);
        else if(row.data() instanceof StowActions.Entry action&&action.glyph()!=null)dev.stow.client.ui.UIIcons.draw(g,action.glyph(),x+4,y+3,action.enabled()?CREAM:DIM);
        else if(row.kind()==Kind.MORE){tx=x+8;}
        if(row.kind()==Kind.GOAL){drawGoalRow(g,row,x,y,w,tx);return;}
        int rightW=row.right().isEmpty()?0:font.width(row.right())+6;
        boolean disabled=row.data() instanceof StowActions.Entry action&&!action.enabled();
        if(row.sub().isEmpty())text(g,row.title(),tx,y+7,w-tx+x-rightW-4,disabled?DIM:row.kind()==Kind.MORE?GOLD:CREAM);
        else{text(g,row.title(),tx,y+2,w-tx+x-rightW-4,disabled?DIM:CREAM);text(g,row.sub(),tx,y+12,w-tx+x-rightW-4,row.subColor());}
        if(rightW>0)right(g,row.right(),x+w-4,y+7,row.rightColor());
    }
    private void drawGoalRow(GuiGraphicsExtractor g,Row row,int x,int y,int w,int tx){
        var data=(Object[])row.data();var goal=(MaterialGoal)data[0];var p=(MaterialPlanner.Progress)data[1];
        boolean done=p.enough();int cols=x+w-4;
        boolean hud=store.isHudGoal(goal.itemId(),Integer.MAX_VALUE),hidden=!store.isGoalVisible(goal.itemId());
        if(wide){
            int nameW=w-(tx-x)-176;
            text(g,row.title(),tx,y+3,nameW,hidden?DIM:CREAM);progress(g,tx,y+15,Math.max(20,nameW),p.counted(),goal.target());
            right(g,number(goal.target()),cols-152,y+7,MUTED);right(g,number(p.counted()),cols-102,y+7,MUTED);
            right(g,done?"✓":number(p.missing()),cols-48,y+7,done?SAGE:OCHRE);right(g,Integer.toString(sourcesInScope(goal.itemId())),cols-14,y+7,DIM);
        }else{
            text(g,row.title(),tx,y+2,w-(tx-x)-60,hidden?DIM:CREAM);
            text(g,number(p.counted())+" / "+number(goal.target()),tx,y+12,w-(tx-x)-60,MUTED);
            right(g,done?"✓":"−"+number(p.missing()),cols-14,y+7,done?SAGE:OCHRE);
        }
        if(hud)right(g,"★",cols,y+7,GOLD);
    }
    private void drawNow(GuiGraphicsExtractor g){
        int x=left,y=panelTop,col=nowCol();panel(g,x,y,w,contentBottom()-y);
        int cx=x+8,cardTop=y+17,cardW=col-16;
        section(g,Component.translatable("stow.ui.now.project").getString(),cx,y+7);
        card(g,cx,cardTop,cardW,30);
        var goals=store.goals();long done=goals.stream().filter(gl->MaterialPlanner.progress(store,gl).enough()).count();
        int textW=cardW-110;
        text(g,store.projectName(),cx+6,cardTop+5,textW,CREAM);
        if(goals.isEmpty())text(g,Component.translatable("stow.ui.now.project-empty").getString(),cx+6,cardTop+17,textW,MUTED);
        else{text(g,Component.translatable("stow.ui.now.project-progress",number(done),number(goals.size())).getString(),cx+6,cardTop+16,textW,MUTED);progress(g,cx+6,cardTop+26,textW,done,goals.size());}
        long stale=store.chests().stream().filter(StowIndex::stale).count();
        text(g,Component.translatable(stale>0?"stow.ui.now.stale":"stow.ui.now.fresh",number(stale),number(store.size())).getString(),cx,y+56,cardW-70,stale>0?OCHRE:DIM);
        section(g,Component.translatable("stow.ui.now.recent").getString(),cx,nowRecentY);
        if(session.recentItems.isEmpty())text(g,Component.translatable("stow.ui.now.recent-empty").getString(),cx,nowRecentY+16,cardW,DIM);
        section(g,Component.translatable("stow.ui.now.toggles").getString(),cx,nowToggleY);
        int rx=wide?x+col+8:cx,ry=wide?y+7:nowToggleY+60,rw=wide?w-col-16:cardW;
        if(ry+40>contentBottom())return;
        section(g,Component.translatable("stow.ui.now.try").getString(),rx,ry);ry+=10;
        for(int i=1;i<=4;i++){for(var line:font.split(Component.translatable("stow.ui.now.try."+i),rw)){if(ry+9>contentBottom())return;g.text(font,line,rx,ry,i%2==1?CREAM:MUTED);ry+=10;}ry+=3;}
        if(!wide)return;
        ry+=8;section(g,Component.translatable("stow.ui.now.keys").getString(),rx,ry);ry+=10;
        for(int i=1;i<=4;i++){text(g,Component.translatable("stow.ui.now.key."+i).getString(),rx,ry,rw,DIM);ry+=11;}
    }
    private int sourcesInScope(String id){var stat=StowIndex.of(store).item(id);if(stat==null)return 0;int n=0;for(var s:stat.sources())if(store.isIncluded(s.chest().location()))n++;return n;}
    private void drawDetail(GuiGraphicsExtractor g){
        int bottom=contentBottom();panel(g,detailX,panelTop,detailW,bottom-panelTop);
        var row=current();int x=detailX+8,y=panelTop+8,iw=detailW-16;
        if(row==null){
            String key=segment()==Segment.CONTAINERS?"stow.ui.containers.pick":segment()==Segment.PROJECT?"stow.ui.project.pick":"stow.ui.find.pick";
            int yy=panelTop+30;for(var line:font.split(Component.translatable(key),iw)){g.centeredText(font,line,detailX+detailW/2,yy,DIM);yy+=11;}return;
        }
        switch(row.kind()){
            case ITEM,CATALOGUE->drawItemDetail(g,(String)row.data(),x,y,iw,bottom-30);
            case CONTAINER->drawContainerDetail(g,(SavedChest)row.data(),x,panelTop+30,iw,bottom-56);
            case GOAL->drawGoalDetail(g,(Object[])row.data(),x,y,iw);
            case ACTION->{var a=(StowActions.Entry)row.data();
                text(g,a.title().getString(),x,y,iw,CREAM);y+=14;
                if(a.shortcut()!=null){for(var line:font.split(a.shortcut().description(),iw)){g.text(font,line,x,y,MUTED);y+=10;}y+=4;
                    text(g,Component.translatable("stow.ui.find.shortcut",a.shortcutLabel().isEmpty()?Component.translatable("stow.shortcuts.unbound").getString():a.shortcutLabel()).getString(),x,y,iw,DIM);y+=12;}
                if(!a.enabled()&&a.reason()!=null)for(var line:font.split(a.reason(),iw)){g.text(font,line,x,y,OCHRE);y+=10;}}
            case PROJECT->{var pr=(Project)row.data();text(g,pr.name(),x,y,iw,CREAM);y+=14;
                text(g,Component.translatable(pr.id().equals(store.activeProjectId())?"stow.ui.project.active":"stow.ui.find.switch-hint").getString(),x,y,iw,MUTED);}
            case NEED->{for(var line:font.split(Component.literal(row.title()),iw)){g.text(font,line,x,y,CREAM);y+=10;}y+=4;
                var data=(Object[])row.data();var goal=store.goal((String)data[0]);
                text(g,goal==null?Component.translatable("stow.ui.find.need-new",store.projectName()).getString():Component.translatable("stow.ui.find.need-replace",number(goal.target())).getString(),x,y,iw,MUTED);}
            case MORE->text(g,row.title(),x,y,iw,MUTED);
            default->{}
        }
    }
    private void drawItemDetail(GuiGraphicsExtractor g,String id,int x,int y,int iw,int limit){
        g.item(MemoryItems.icon(id),x,y);text(g,MemoryItems.name(id),x+22,y+1,iw-22,CREAM);text(g,id,x+22,y+11,iw-22,DIM);y+=26;
        var index=StowIndex.of(store);var stat=index.item(id);long carried=StowIndex.carried(id),stored=stat==null?0:stat.stored();
        stat(g,x,y,iw,Component.translatable("stow.ui.find.detail.stored").getString(),number(stored));y+=11;
        stat(g,x,y,iw,Component.translatable("stow.ui.find.detail.carried").getString(),number(carried));y+=11;
        var goal=store.goal(id);
        if(goal!=null){var p=MaterialPlanner.progress(store,goal);stat(g,x,y,iw,Component.translatable("stow.ui.find.detail.project",store.projectName()).getString(),number(p.counted())+" / "+number(goal.target()));y+=11;}
        y+=6;section(g,Component.translatable("stow.ui.find.detail.sources").getString(),x,y);y+=10;
        if(stat==null){text(g,Component.translatable("stow.ui.find.detail.none").getString(),x,y,iw,DIM);return;}
        var sources=index.sourcesNearestFirst(id);int shown=0;
        for(var s:sources){
            if(y+20>limit){text(g,Component.translatable("stow.ui.find.detail.more",sources.size()-shown).getString(),x,y,iw,DIM);break;}
            double d=StowIndex.distance(s.chest());boolean stale=StowIndex.stale(s.chest());
            g.item(MemoryItems.storageIcon(s.chest()),x,y);
            String right="×"+number(s.count());int rw=font.width(right)+4;
            text(g,store.displayName(s.chest()),x+20,y,iw-20-rw,CREAM);
            text(g,distance(d)+" · "+ChestMemoryStore.age(s.chest().lastSeen()),x+20,y+9,iw-20-rw,stale?OCHRE:DIM);
            right(g,right,x+iw,y+4,MUTED);y+=20;shown++;
        }
    }
    private void stat(GuiGraphicsExtractor g,int x,int y,int iw,String label,String value){int vw=font.width(value);text(g,label,x,y,iw-vw-6,MUTED);right(g,value,x+iw,y,CREAM);}
    private void drawContainerDetail(GuiGraphicsExtractor g,SavedChest chest,int x,int y,int iw,int limit){
        var loc=chest.location();double d=StowIndex.distance(chest);
        stat(g,x,y,iw,Component.translatable("stow.ui.containers.detail.where").getString(),loc.x()+" "+loc.y()+" "+loc.z());y+=11;
        stat(g,x,y,iw,Component.translatable("stow.ui.containers.detail.distance").getString(),d>=0?distance(d):shortDimension(loc.dimension()));y+=11;
        stat(g,x,y,iw,Component.translatable("stow.ui.containers.detail.seen").getString(),ChestMemoryStore.age(chest.lastSeen()));y+=11;
        stat(g,x,y,iw,Component.translatable("stow.ui.containers.detail.owner").getString(),StorageOwners.label(chest));y+=11;
        stat(g,x,y,iw,Component.translatable("stow.ui.containers.detail.project",store.projectName()).getString(),Component.translatable(store.isIncluded(loc)?"stow.ui.containers.in":"stow.ui.containers.out").getString());y+=11;
        if(StowIndex.stale(chest)){text(g,Component.translatable("stow.ui.containers.detail.stale").getString(),x,y,iw,OCHRE);y+=11;}
        y+=4;section(g,Component.translatable("stow.ui.containers.detail.contents",chest.items().size()).getString(),x,y);y+=10;
        int slots=Math.max(1,Math.min(9,iw/18));int rowsFit=Math.max(0,(limit-y)/18);
        if(chest.items().isEmpty())text(g,Component.translatable("stow.ui.containers.detail.empty").getString(),x,y,iw,DIM);
        else if(rowsFit>0)MemoryItems.drawPreview(g,chest.items().size()>slots*rowsFit?MemoryItems.sortPreview(chest.items()).subList(0,slots*rowsFit):chest.items(),x,y,slots,lastMouseX,lastMouseY,panelTop);
    }
    private int lastMouseX,lastMouseY;
    private void drawGoalDetail(GuiGraphicsExtractor g,Object[] data,int x,int y,int iw){
        var goal=(MaterialGoal)data[0];String id=goal.itemId();var p=MaterialPlanner.progress(store,goal);
        g.item(MemoryItems.icon(id),x,y);text(g,MemoryItems.name(id),x+22,y+4,iw-22,CREAM);
        text(g,Component.translatable("stow.ui.project.need").getString(),x,panelTop+35,56,MUTED);
        y=panelTop+56;
        long target=goal.target(),stacks=target/64,rest=target%64;
        String breakdown=target>=1728?Component.translatable("stow.ui.project.breakdown-shulkers",String.format(Locale.ROOT,"%.1f",target/1728.0),number(stacks)).getString():Component.translatable("stow.ui.project.breakdown",number(stacks),number(rest)).getString();
        text(g,breakdown,x,y,iw,DIM);y+=14;
        long stored=StowIndex.of(store).item(id)==null?0:StowIndex.of(store).item(id).stored();
        stat(g,x,y,iw,Component.translatable("stow.ui.find.detail.carried").getString(),number(p.inventory()));y+=11;
        stat(g,x,y,iw,Component.translatable("stow.ui.project.in-scope").getString(),number(p.chests()));y+=11;
        stat(g,x,y,iw,Component.translatable("stow.ui.project.out-scope").getString(),number(Math.max(0,stored-p.chests())));y+=11;
        if(!Stow.config.materialsIncludeInventory){text(g,Component.translatable("stow.ui.project.inventory-excluded").getString(),x,y,iw,DIM);y+=11;}
        y+=4;progress(g,x,y,iw,p.counted(),target);y+=6;
        text(g,p.enough()?Component.translatable("stow.ui.project.goal-done").getString():Component.translatable("stow.ui.project.goal-missing",number(p.missing())).getString(),x,y,iw,p.enough()?SAGE:OCHRE);
    }

    // ---------------------------------------------------------------- input
    private void closeDetail(){detailOpen=false;inlineFor="";setFocused(query);refresh();}
    private void openDetail(){if(current()==null)return;if(!wide){detailOpen=true;rebuildDynamic();}}
    private int rowAt(double mx,double my){
        if(!showingList()||!inside(mx,my,listX,listTop,listW,contentBottom()-listTop))return -1;
        int y=listTop-scroll;for(int i=0;i<rows.size();i++){int h=rows.get(i).height();if(my>=y&&my<y+h)return rows.get(i).selectable()?i:-1;y+=h;}return -1;
    }
    private boolean inPopup(double mx,double my){return popup!=Popup.NONE&&inside(mx,my,popupX,popupY,popupW,popupH);}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        double mx=event.x(),my=event.y();
        if(popup!=Popup.NONE){
            if(inPopup(mx,my)){for(var widget:popupWidgets)if(widget.isMouseOver(mx,my)){setFocused(widget);widget.mouseClicked(event,twice);return true;}return true;}
            popup=Popup.NONE;error="";rebuildDynamic();return true;
        }
        boolean showingNow=segment()==Segment.FIND&&query.getValue().isBlank();
        int index=showingNow?-1:rowAt(mx,my);
        if(index>=0){
            select(index,false);
            if(event.button()==1&&rows.get(index).kind()==Kind.ITEM){activate(true);return true;}
            if(twice||rows.get(index).kind()==Kind.MORE)activate(false);else if(!wide)openDetail();
            return true;
        }
        return super.mouseClicked(event,twice);
    }
    @Override public boolean mouseScrolled(double mx,double my,double sx,double sy){
        if(popup!=Popup.NONE)return true;
        if(showingList()&&inside(mx,my,listX,listTop,listW,contentBottom()-listTop)){scroll-=(int)Math.round(sy*ROW);clampScroll();return true;}
        return super.mouseScrolled(mx,my,sx,sy);
    }
    /** Enter: the row's primary action. Shift+Enter (or right-click on an item): the secondary one. */
    private void activate(boolean secondary){
        var row=current();if(row==null)return;
        switch(row.kind()){
            case ITEM,CATALOGUE->{
                String id=(String)row.data();
                if(segment()==Segment.PROJECT&&addMode){quickAdd(id);return;}
                session.recentItem(id);session.remember(query.getValue());
                var nearest=StowIndex.of(store).nearest(id);
                if(secondary||nearest==null){if(!store.hasGoal(id)){store.setGoal(id,64);ChestMemory.saveLater(store);UiNotifications.show("stow.ui.feedback.added",MemoryItems.name(id),store.projectName());refresh();}else UiNotifications.show("stow.ui.feedback.already",MemoryItems.name(id));return;}
                ChestMemory.show(store,nearest,id);StowFind.closeAll();
            }
            case CONTAINER->{var chest=(SavedChest)row.data();if(secondary){store.toggleIncluded(chest.location());ChestMemory.saveLater(store);refresh();return;}
                ChestMemory.select(chest.location());session.recentContainer(chest.location().key());StowFind.closeAll();}
            case ACTION->runAction((StowActions.Entry)row.data());
            case PROJECT->{var pr=(Project)row.data();if(!pr.id().equals(store.activeProjectId())){store.switchProject(pr.id());ChestMemory.saveLater(store);UiNotifications.show("stow.ui.feedback.switched",pr.name());}switchSegment(Segment.PROJECT);}
            case NEED->{var d=(Object[])row.data();addNeed((String)d[0],(int)d[1]);}
            case GOAL->{if(secondary){MaterialPlanner.toggleHud(store,((MaterialGoal)((Object[])row.data())[0]).itemId());ChestMemory.saveLater(store);refresh();}else{openDetail();focusInline();}}
            case MORE->expand((String)row.data());
            default->{}
        }
    }
    private void quickAdd(String id){
        var existing=store.goal(id);if(existing==null){store.setGoal(id,64);ChestMemory.saveLater(store);UiNotifications.show("stow.ui.feedback.added",MemoryItems.name(id),store.projectName());}
        else UiNotifications.show("stow.ui.feedback.already",MemoryItems.name(id));
        query.setValue("");setFocused(query);
    }
    private void focusInline(){if(inline!=null){setFocused(inline);inline.moveCursorToEnd(false);inline.setHighlightPos(0);}}
    private void move(int step){
        if(rows.isEmpty())return;int from=selected<0?(step>0?-1:rows.size()):selected;
        int next=firstSelectable(from+step,step);
        if(next<0&&Math.abs(step)>1)next=firstSelectable(step>0?rows.size()-1:0,-Integer.signum(step));
        if(next>=0)select(next,true);
    }
    private void recallHistory(int step){
        if(session.history.isEmpty())return;var list=new ArrayList<>(session.history);
        historyIndex=Math.clamp(historyIndex+step,-1,list.size()-1);
        int keep=historyIndex;query.setValue(historyIndex<0?"":list.get(historyIndex));historyIndex=keep;query.moveCursorToEnd(false);
    }
    @Override public boolean keyPressed(KeyEvent event){
        int key=event.key();boolean ctrl=event.hasControlDown(),shift=event.hasShiftDown();
        boolean inlineFocused=inline!=null&&getFocused()==inline,nameFocused=projectName!=null&&getFocused()==projectName;
        if(event.isEscape()){
            if(popup!=Popup.NONE){popup=Popup.NONE;error="";rebuildDynamic();return true;}
            if(inlineFocused){inlineFor="";setFocused(query);rebuildDynamic();return true;}
            if(addMode){addMode=false;query.setValue("");refresh();return true;}
            if(!wide&&detailOpen){closeDetail();return true;}
            onClose();return true;
        }
        if(StowShortcuts.matches(Action.PALETTE,StowShortcuts.chord(event))&&!inlineFocused&&!nameFocused){onClose();return true;}
        if(ctrl&&key>=InputConstants.KEY_1&&key<=InputConstants.KEY_3){switchSegment(Segment.values()[key-InputConstants.KEY_1]);return true;}
        if(ctrl&&key==InputConstants.KEY_Z&&!inlineFocused&&!nameFocused){undo();return true;}
        if(ctrl&&key==InputConstants.KEY_I&&store!=null){minecraft.gui.setScreen(new MaterialImportScreen(this,store));return true;}
        if(nameFocused&&(key==InputConstants.KEY_RETURN||key==InputConstants.KEY_NUMPADENTER)){createProject();return true;}
        if(inlineFocused&&(key==InputConstants.KEY_RETURN||key==InputConstants.KEY_NUMPADENTER)){
            var row=current();if(row!=null&&row.kind()==Kind.CONTAINER)renameInline((SavedChest)row.data());else if(row!=null&&row.kind()==Kind.GOAL)saveNeed(((MaterialGoal)((Object[])row.data())[0]).itemId());return true;}
        if(inlineFocused||nameFocused)return super.keyPressed(event);
        if(popup!=Popup.NONE)return super.keyPressed(event);
        boolean queryEmpty=query.getValue().isEmpty();
        switch(key){
            case InputConstants.KEY_DOWN->{if(segment()==Segment.FIND&&queryEmpty&&historyIndex>=0){recallHistory(-1);return true;}move(1);return true;}
            case InputConstants.KEY_UP->{if(segment()==Segment.FIND&&(queryEmpty||historyIndex>=0)&&(selected<=firstSelectable(0,1))){recallHistory(1);return true;}move(-1);return true;}
            case InputConstants.KEY_PAGEDOWN->{move(Math.max(1,listHeight()/ROW));return true;}
            case InputConstants.KEY_PAGEUP->{move(-Math.max(1,listHeight()/ROW));return true;}
            case InputConstants.KEY_RETURN,InputConstants.KEY_NUMPADENTER->{if(current()==null&&!rows.isEmpty())select(firstSelectable(0,1),true);activate(shift);return true;}
            case InputConstants.KEY_F2->{var row=current();if(row!=null&&(row.kind()==Kind.CONTAINER||row.kind()==Kind.GOAL)){openDetail();focusInline();}return true;}
            case InputConstants.KEY_BACKSPACE->{if(!wide&&detailOpen&&queryEmpty){closeDetail();return true;}}
            case InputConstants.KEY_DELETE->{var row=current();
                if(row!=null&&row.kind()==Kind.GOAL&&queryEmpty){String id=((MaterialGoal)((Object[])row.data())[0]).itemId();store.removeGoal(id);ChestMemory.saveLater(store);UiNotifications.show("stow.ui.feedback.removed",MemoryItems.name(id));refresh();return true;}}
            case InputConstants.KEY_SPACE->{var row=current();
                if(queryEmpty&&row!=null&&(row.kind()==Kind.GOAL||row.kind()==Kind.CONTAINER)){activate(true);return true;}}
            case InputConstants.KEY_TAB->{if(!shift){var row=current();if(row!=null&&(row.kind()==Kind.CONTAINER||row.kind()==Kind.GOAL)&&inline!=null){openDetail();focusInline();return true;}}}
            default->{}
        }
        if(getFocused()!=query&&getFocused() instanceof AbstractWidget)return super.keyPressed(event);
        if(query.keyPressed(event)){setFocused(query);return true;}
        return super.keyPressed(event);
    }
    @Override public boolean charTyped(CharacterEvent event){
        boolean other=getFocused()!=null&&getFocused()!=query&&getFocused() instanceof EditBox;
        if(!other&&popup==Popup.NONE){if(getFocused()!=query)setFocused(query);return query.charTyped(event);}
        return super.charTyped(event);
    }
    @Override public void tick(){
        if(store!=null&&store.revision()!=seenRevision&&!(inline!=null&&getFocused()==inline))refresh();
        if(deleteArmed>0&&deleteArmed<System.currentTimeMillis()){deleteArmed=0;if(popup==Popup.PROJECTS)rebuildDynamic();}
        if(parent instanceof AbstractContainerScreen<?> container&&minecraft.player!=null&&minecraft.player.containerMenu!=container.getMenu()&&minecraft.player.containerMenu==minecraft.player.inventoryMenu){/* container closed remotely: Done returns to the world */}
    }
    @Override public void onClose(){
        session.query.put(segment(),query==null?"":query.getValue());
        setFocused(null);Screen back=parent;
        if(back instanceof AbstractContainerScreen<?> container&&minecraft.player!=null&&minecraft.player.containerMenu!=container.getMenu())back=null;
        minecraft.gui.setScreen(back);
    }
    @Override public boolean isPauseScreen(){return false;}
}
