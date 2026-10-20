package dev.stow.client.stow;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stow.Stow;
import dev.stow.StowConfig;
import dev.stow.StowConfig.*;
import dev.stow.client.input.StowShortcuts;
import dev.stow.client.input.StowShortcuts.Action;
import dev.stow.client.inventory.SmartDeposit;
import dev.stow.client.memory.*;
import dev.stow.client.ui.StowEditBox;
import dev.stow.client.ui.UIIcons;
import dev.stow.client.ui.UiNotifications;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import static dev.stow.client.stow.StowTheme.*;

/**
 * Native settings (redesign v2 §3.6), replacing the Cloth Config screen. Every change applies
 * live and is written with the existing atomic settings.json save; Ctrl+Z undoes the last change.
 * Field names and meanings are those of 1.0.6; nothing is renamed.
 */
public final class SettingsScreen extends Screen {
    public enum Category { INVENTORY,DEPOSIT,STORAGE,PROJECTS,TOOLS,HUD,CONTROLS,UPDATES;
        Component title(){return Component.translatable("stow.settings.cat."+name().toLowerCase(Locale.ROOT));} }
    enum Type { TOGGLE,CYCLE,SLIDER,NUMBER,LINK }
    /** One setting row. get/set work on boxed values so that all types share the undo stack. */
    record Setting(Category category,String id,Component label,Component help,Type type,Supplier<Object> get,Consumer<Object> set,Object defaults,Object[] values,Function<Object,Component> name,int min,int max,Function<Screen,Screen> link) {
        boolean changed(){return type!=Type.LINK&&!Objects.equals(get.get(),defaults);}
        String text(){return (label.getString()+" "+(help==null?"":help.getString())+" "+id).toLowerCase(Locale.ROOT);}
    }
    private record Undo(String id,Runnable restore) {}
    private record Line(int height,Setting setting,Action action,String keepId,String header) {}

    private final Screen parent;
    private Category category;
    private final List<Setting> settings=new ArrayList<>();
    private final Deque<Undo> undo=new ArrayDeque<>();
    private final List<AbstractWidget> body=new ArrayList<>();
    private final List<MemoryItems.Entry> catalogue=MemoryItems.catalogue();
    private EditBox search,keepAdd;
    private String keepFocus="",keepQuery="";
    private List<Line> lines=List.of();
    private int left,w,navW,bodyX,bodyW,top,bottom,scroll;
    private boolean wide;
    private Action waiting;
    private Component help=Component.empty();

    private SettingsScreen(Screen parent,Category category){super(Component.translatable("stow.settings.title"));this.parent=parent;this.category=category;define();}
    public static Screen open(Screen parent){return new SettingsScreen(parent,Category.INVENTORY);}
    public static Screen controls(Screen parent){return new SettingsScreen(parent,Category.CONTROLS);}
    /** The K shortcut and Find's Keep action: Deposit & keep, with this item's row ready to edit. */
    public static Screen keep(Screen parent,String itemId){var s=new SettingsScreen(parent,Category.DEPOSIT);s.keepFocus=itemId==null?"":itemId;return s;}
    /** For What's new "Show me" links and Find: open a category by name. */
    public static Screen category(Screen parent,String name){
        for(var c:Category.values())if(c.name().equalsIgnoreCase(name))return new SettingsScreen(parent,c);
        return open(parent);
    }

    // ---------------------------------------------------------------- setting definitions
    private static Component s(String key){return Component.translatable("stow.settings."+key);}
    private static Component tipOrNull(String key){return net.minecraft.locale.Language.getInstance().has("stow.settings."+key)?s(key):null;}
    private void toggle(Category c,String key,BooleanSupplier get,Consumer<Boolean> set,boolean defaults){
        settings.add(new Setting(c,key,s(key),tipOrNull(key+".tip"),Type.TOGGLE,()->get.getAsBoolean(),v->set.accept((Boolean)v),defaults,null,null,0,0,null));
    }
    private <E extends Enum<E>> void cycle(Category c,String key,E[] values,Supplier<E> get,Consumer<E> set,E defaults,Function<E,Component> name){
        settings.add(new Setting(c,key,s(key),tipOrNull(key+".tip"),Type.CYCLE,get::get,v->{@SuppressWarnings("unchecked") E e=(E)v;set.accept(e);},defaults,values,v->{@SuppressWarnings("unchecked") E e=(E)v;return name.apply(e);},0,0,null));
    }
    private void slider(Category c,String key,IntSupplier get,IntConsumer set,int defaults,int min,int max){
        settings.add(new Setting(c,key,s(key),tipOrNull(key+".tip"),Type.SLIDER,()->get.getAsInt(),v->set.accept((Integer)v),defaults,null,null,min,max,null));
    }
    private void number(Category c,String key,IntSupplier get,IntConsumer set,int defaults,int min,int max){
        settings.add(new Setting(c,key,s(key),tipOrNull(key+".tip"),Type.NUMBER,()->get.getAsInt(),v->set.accept((Integer)v),defaults,null,null,min,max,null));
    }
    private void link(Category c,String id,Component label,Component help,Function<Screen,Screen> open){
        settings.add(new Setting(c,id,label,help,Type.LINK,()->null,v->{},null,null,null,0,0,open));
    }
    private static Component sortName(SortOrder v){return Component.translatable("stow.sort."+v.name().toLowerCase(Locale.ROOT));}
    private void define(){
        var c=Stow.config;
        // Inventory & sorting
        cycle(Category.INVENTORY,"normal",SortOrder.values(),()->c.sortOrder,v->c.sortOrder=v,SortOrder.CREATIVE,SettingsScreen::sortName);
        cycle(Category.INVENTORY,"shift",SortOrder.values(),()->c.shiftSortOrder,v->c.shiftSortOrder=v,SortOrder.AMOUNT,SettingsScreen::sortName);
        cycle(Category.INVENTORY,"control",SortOrder.values(),()->c.controlSortOrder,v->c.controlSortOrder=v,SortOrder.NAME,SettingsScreen::sortName);
        cycle(Category.INVENTORY,"hotbar",HotbarScoping.values(),()->c.hotbarScoping,v->c.hotbarScoping=v,HotbarScoping.SOFT,v->s("hotbar."+v.name().toLowerCase(Locale.ROOT)));
        cycle(Category.INVENTORY,"drag",DragMode.values(),()->c.dragMode,v->c.dragMode=v,DragMode.BOTH,v->Component.translatable("stow.inventory.drag."+v.name().toLowerCase(Locale.ROOT)));
        cycle(Category.INVENTORY,"pin.style",PinStyle.values(),()->c.pinStyle,v->c.pinStyle=v,PinStyle.LOCK,v->s("pin."+v.name().toLowerCase(Locale.ROOT)));
        toggle(Category.INVENTORY,"pin.transfers",()->c.pinProtectTransfers,v->c.pinProtectTransfers=v,true);
        toggle(Category.INVENTORY,"bundle.drag",()->c.bundleDrag,v->c.bundleDrag=v,true);
        toggle(Category.INVENTORY,"shulker-previews",()->c.shulkerPreviews,v->c.shulkerPreviews=v,true);
        toggle(Category.INVENTORY,"creative-search",()->c.optimizeCreativeSearchSort,v->c.optimizeCreativeSearchSort=v,true);
        // Deposit & keep amounts (the keep table is drawn below these rows)
        toggle(Category.DEPOSIT,"deposit.matching",()->c.depositMatchingOnly,v->c.depositMatchingOnly=v,true);
        toggle(Category.DEPOSIT,"deposit.hotbar",()->c.depositKeepHotbar,v->c.depositKeepHotbar=v,true);
        toggle(Category.DEPOSIT,"take-missing",()->c.takeMissing,v->c.takeMissing=v,true);
        // Storage & finding
        toggle(Category.STORAGE,"glow",()->c.chestGlow,ChestMemory::setGlowEnabled,true);
        cycle(Category.STORAGE,"layout",LayoutMode.values(),()->c.stowLayout,v->c.stowLayout=v,LayoutMode.AUTO,v->s("layout."+v.name().toLowerCase(Locale.ROOT)));
        toggle(Category.STORAGE,"chests.auto-assign",()->c.autoAssignChests,v->c.autoAssignChests=v,true);
        link(Category.STORAGE,"open-containers",Component.translatable("stow.settings.open-containers"),null,p->StowScreen.open(p,net.minecraft.world.item.ItemStack.EMPTY,StowSession.Segment.CONTAINERS));
        // Projects
        toggle(Category.PROJECTS,"materials.include-inventory",()->c.materialsIncludeInventory,v->c.materialsIncludeInventory=v,true);
        toggle(Category.PROJECTS,"materials.page",()->c.rememberMaterialPage,v->c.rememberMaterialPage=v,true);
        toggle(Category.PROJECTS,"dock.materials",()->c.dockMaterials,v->c.dockMaterials=v,true);
        toggle(Category.PROJECTS,"dock.finished",()->c.dockFinished,v->c.dockFinished=v,true);
        slider(Category.PROJECTS,"dock.rows",()->c.dockRows,v->c.dockRows=v,3,3,5);
        link(Category.PROJECTS,"open-project",Component.translatable("stow.settings.open-project"),null,p->StowScreen.open(p,net.minecraft.world.item.ItemStack.EMPTY,StowSession.Segment.PROJECT));
        // Tools & automation
        toggle(Category.TOOLS,"refill",()->c.handRefill,v->c.handRefill=v,true);
        slider(Category.TOOLS,"refill.threshold",()->c.refillThreshold,v->c.refillThreshold=v,8,0,16);
        toggle(Category.TOOLS,"tool-refill",()->c.toolRefill,v->c.toolRefill=v,true);
        cycle(Category.TOOLS,"tool-refill.mode",ToolRefillMode.values(),()->c.toolRefillMode,v->c.toolRefillMode=v,ToolRefillMode.KEEP_ONE,v->s("tool-refill."+v.name().toLowerCase(Locale.ROOT)));
        toggle(Category.TOOLS,"tool-pick",()->c.toolPick,v->c.toolPick=v,true);
        toggle(Category.TOOLS,"auto-tool",()->c.autoTool,v->c.autoTool=v,false);
        toggle(Category.TOOLS,"bulk-strip",()->c.bulkStrip,v->c.bulkStrip=v,true);
        cycle(Category.TOOLS,"attack.mode",AttackMode.values(),()->c.attackMode,v->c.attackMode=v,AttackMode.HOLD,v->s(v==AttackMode.INTERVAL?"attack.interval-mode":"attack.hold"));
        number(Category.TOOLS,"attack.interval",()->c.attackIntervalMs,v->c.attackIntervalMs=v,500,50,60_000);
        // HUD & glow
        link(Category.HUD,"arrange",Component.translatable("stow.ui.arrange.title"),Component.translatable("stow.settings.arrange.tip"),p->new ArrangeHudScreen(p,null));
        toggle(Category.HUD,"stock",()->c.buildingStock,v->c.buildingStock=v,true);
        slider(Category.HUD,"stock.scale",()->c.buildingStockScale,v->c.buildingStockScale=v,135,100,200);
        toggle(Category.HUD,"dock.enabled",()->c.dockEnabled&&c.equipmentWatch,v->{c.dockEnabled=v;c.equipmentWatch=v;},true);
        toggle(Category.HUD,"equipment.all",()->c.equipmentAll,v->c.equipmentAll=v,true);
        toggle(Category.HUD,"equipment.percent",()->c.equipmentPercent,v->c.equipmentPercent=v,false);
        slider(Category.HUD,"equipment.threshold",()->c.equipmentThreshold,v->c.equipmentThreshold=v,15,1,50);
        toggle(Category.HUD,"automation-hud",()->c.automationHud,v->c.automationHud=v,true);
        toggle(Category.HUD,"notifications",()->c.notifications,v->c.notifications=v,true);
        link(Category.HUD,"equipment",Component.translatable("stow.action.equipment"),null,dev.stow.client.ui.EquipmentScreen::new);
        // Updates & help
        toggle(Category.UPDATES,"whats-new",()->c.whatsNew,v->c.whatsNew=v,true);
        link(Category.UPDATES,"whats-new-open",Component.translatable("stow.whatsnew.open"),null,dev.stow.client.ui.WhatsNew::screen);
        link(Category.UPDATES,"legend",Component.translatable("stow.settings.legend"),Component.translatable("stow.settings.legend.tip"),p->{c.keyLegendShown=0;c.save();UiNotifications.show("stow.settings.legend.done");return null;});
    }

    // ---------------------------------------------------------------- changes and undo
    private void change(Setting setting,Object value){
        Object old=setting.get().get();if(Objects.equals(old,value))return;
        if(undo.isEmpty()||!undo.peekFirst().id().equals(setting.id()))undo.addFirst(new Undo(setting.id(),()->setting.set().accept(old)));
        while(undo.size()>50)undo.removeLast();
        setting.set().accept(value);Stow.config.save();
    }
    private void changeKeep(String id,int amount){
        var rules=Stow.config.keepAmounts;Integer old=rules.get(id);
        if(Objects.equals(old,amount>0?amount:null))return;
        String undoId="keep:"+id;
        if(undo.isEmpty()||!undo.peekFirst().id().equals(undoId))undo.addFirst(new Undo(undoId,()->{if(old==null)Stow.config.keepAmounts.remove(id);else Stow.config.keepAmounts.put(id,old);}));
        if(amount>0)rules.put(id,amount);else rules.remove(id);Stow.config.save();
    }
    private void changeShortcut(Action action,StowConfig.Shortcut shortcut){
        var old=Stow.config.shortcuts.get(action.name());
        undo.addFirst(new Undo("key:"+action.name(),()->{if(old==null)Stow.config.shortcuts.remove(action.name());else Stow.config.shortcuts.put(action.name(),old);}));
        if(shortcut.equals(action.defaults()))Stow.config.shortcuts.remove(action.name());else Stow.config.shortcuts.put(action.name(),shortcut);
        Stow.config.save();
    }
    private void undoLast(){
        var last=undo.pollFirst();if(last==null){UiNotifications.show("stow.settings.nothing-to-undo");return;}
        last.restore().run();Stow.config.save();UiNotifications.show("stow.settings.undone");rebuild();
    }

    // ---------------------------------------------------------------- layout
    @Override protected void init(){
        w=Math.min(720,width-16);left=(width-w)/2;wide=w>=520;navW=wide?Math.min(150,w/4):0;
        top=56;bottom=height-30;bodyX=left+navW+(wide?6:0);bodyW=w-navW-(wide?6:0);
        search=new StowEditBox(font,left,24,w,20,Component.translatable("stow.settings.search"));
        search.setHint(Component.translatable("stow.settings.search.hint"));search.setMaxLength(60);
        search.setResponder(v->{scroll=0;rebuild();});addRenderableWidget(search);setInitialFocus(search);
        addRenderableWidget(new PlannerButton(Component.translatable("gui.done"),b->onClose(),left,height-25,60,20,false));
        var undoButton=addRenderableWidget(new PlannerButton(Component.translatable("stow.settings.undo"),b->undoLast(),left+64,height-25,fitW(Component.translatable("stow.settings.undo"),60),20,false));
        undoButton.setTooltip(Tooltip.create(Component.translatable("stow.settings.undo.tip")));
        if(wide){int y=top+4;for(var c:Category.values()){
            addRenderableWidget(new PlannerButton(c.title(),b->{category=c;search.setValue("");scroll=0;waiting=null;rebuild();},left+4,y,navW-8,18,false).navigation().selected(()->search.getValue().isBlank()&&category==c));y+=20;}}
        else{
            Component label=Component.translatable("stow.settings.category",category.title());
            addRenderableWidget(new PlannerButton(label,b->{category=Category.values()[(category.ordinal()+1)%Category.values().length];scroll=0;waiting=null;rebuildWidgets();},left,top-2-20+0,w,18,false))
                .setTooltip(Tooltip.create(Component.translatable("stow.settings.category.tip")));
            top+=20;
        }
        rebuild();
    }
    private int fitW(Component c,int min){return Math.max(min,font.width(c)+14);}
    private List<Line> buildLines(){
        var out=new ArrayList<Line>();String q=search.getValue().strip().toLowerCase(Locale.ROOT);
        if(!q.isEmpty()){
            Category last=null;
            for(var setting:settings)if(setting.text().contains(q)||setting.category().title().getString().toLowerCase(Locale.ROOT).contains(q)){
                if(setting.category()!=last){out.add(new Line(14,null,null,null,setting.category().title().getString()));last=setting.category();}
                out.add(new Line(24,setting,null,null,null));
            }
            boolean any=false;
            for(var action:Action.values())if((action.title().getString()+" "+action.description().getString()+" "+StowShortcuts.label(StowShortcuts.binding(action))).toLowerCase(Locale.ROOT).contains(q)){
                if(!any){out.add(new Line(14,null,null,null,Category.CONTROLS.title().getString()));any=true;}out.add(new Line(24,null,action,null,null));}
            if(!q.isEmpty()&&(Component.translatable("stow.keep.title").getString().toLowerCase(Locale.ROOT).contains(q)||"keep behalten".contains(q))){out.add(new Line(14,null,null,null,Category.DEPOSIT.title().getString()));out.add(new Line(24,null,null,"#open",null));}
            return out;
        }
        for(var setting:settings)if(setting.category()==category)out.add(new Line(24,setting,null,null,null));
        if(category==Category.CONTROLS){out.add(new Line(14,null,null,null,"#controls-head"));for(var action:Action.values())out.add(new Line(24,null,action,null,null));}
        if(category==Category.DEPOSIT){
            out.add(new Line(14,null,null,null,Component.translatable("stow.keep.title").getString()));
            out.add(new Line(24,null,null,"#add",null));
            if(!keepQuery.isBlank())for(var entry:MemoryItems.search(catalogue,keepQuery).stream().filter(e->!Stow.config.keepAmounts.containsKey(e.id())).limit(5).toList())out.add(new Line(24,null,null,"+"+entry.id(),null));
            var ids=new ArrayList<String>();if(!keepFocus.isEmpty()&&!Stow.config.keepAmounts.containsKey(keepFocus))ids.add(keepFocus);ids.addAll(Stow.config.keepAmounts.keySet());
            for(String id:ids)out.add(new Line(24,null,null,id,null));
            if(ids.isEmpty())out.add(new Line(14,null,null,null,"#keep-empty"));
        }
        if(category==Category.UPDATES)out.add(new Line(14,null,null,null,"#version"));
        return out;
    }
    private int total(){int t=0;for(var l:lines)t+=l.height();return t;}
    private void rebuild(){
        if(search==null)return;
        boolean keepAddFocused=keepAdd!=null&&getFocused()==keepAdd;
        AbstractWidget focusedBody=getFocused() instanceof AbstractWidget wd&&body.contains(wd)?wd:null;String focusedKey=focusedBody==null?null:focusKey(focusedBody);
        body.forEach(this::removeWidget);body.clear();keepAdd=null;
        lines=buildLines();scroll=Math.clamp(scroll,0,Math.max(0,total()-(bottom-top-4)));
        int y=top+2-scroll;
        for(var line:lines){if(y+line.height()>top&&y<bottom-2)place(line,y);y+=line.height();}
        if(keepAdd!=null&&keepAddFocused){setFocused(keepAdd);}
        else if(focusedKey!=null)for(var wd:body)if(focusedKey.equals(focusKey(wd))){setFocused(wd);break;}
    }
    private final Map<AbstractWidget,String> keys=new HashMap<>();
    private String focusKey(AbstractWidget w){return keys.get(w);}
    private <T extends AbstractWidget> T add(T widget,String key){body.add(addRenderableWidget(widget));keys.put(widget,key);return widget;}
    static int parseAmount(String value){try{int amount=Integer.parseInt(value);return amount>=0&&amount<=1_000_000?amount:-1;}catch(NumberFormatException e){return -1;}}
    private int controlW(){return Math.min(150,Math.max(90,bodyW/3));}
    private int controlX(){return bodyX+bodyW-6-22-controlW();}
    private void place(Line line,int y){
        int cx=controlX(),cw=controlW(),rx=bodyX+bodyW-6-20;
        if(line.setting()!=null){
            var st=line.setting();
            switch(st.type()){
                case TOGGLE->{boolean on=(Boolean)st.get().get();
                    add(new PlannerButton(Component.translatable(on?"options.on":"options.off"),b->{change(st,!(Boolean)st.get().get());rebuild();},cx,y+2,cw,20,false).selected(()->(Boolean)st.get().get()),st.id());}
                case CYCLE->{
                    add(new PlannerButton(st.name().apply(st.get().get()),b->{var vals=st.values();int i=Arrays.asList(vals).indexOf(st.get().get());int step=minecraft.hasShiftDown()?-1:1;change(st,vals[Math.floorMod(i+step,vals.length)]);rebuild();},cx,y+2,cw,20,false),st.id())
                        .setTooltip(Tooltip.create(Component.translatable("stow.settings.cycle.tip")));}
                case SLIDER->add(new IntSlider(cx,y+2,cw,20,st),st.id());
                case NUMBER->{var box=add(new StowEditBox(font,cx+1,y+3,cw-2,18,st.label()),st.id());box.setMaxLength(6);box.setValue(Integer.toString((Integer)st.get().get()));
                    box.setResponder(v->{try{int n=Integer.parseInt(v.strip());if(n>=st.min()&&n<=st.max()){change(st,n);box.setTextColor(0xFFFFFFFF);}else box.setTextColor(0xFFFFAA91);}catch(NumberFormatException e){box.setTextColor(0xFFFFAA91);}});}
                case LINK->add(new PlannerButton(Component.translatable("stow.settings.open"),b->{var next=st.link().apply(this);if(next!=null)minecraft.gui.setScreen(next);else rebuild();},cx,y+2,cw,20,false),st.id());
            }
            if(st.type()!=Type.LINK){var reset=add(new PlannerButton(Component.empty(),b->{change(st,st.defaults());rebuild();},rx,y+2,20,20,false).icon(UIIcons.Kind.LEFT),st.id()+":reset");
                reset.active=st.changed();reset.setTooltip(Tooltip.create(Component.translatable("stow.settings.reset",st.name()!=null?st.name().apply(st.defaults()):Component.literal(String.valueOf(st.defaults())))));}
            return;
        }
        if(line.action()!=null){
            var action=line.action();var bound=StowShortcuts.binding(action);
            Component label=waiting==action?Component.translatable("stow.shortcuts.press"):Component.literal(StowShortcuts.label(bound));
            add(new PlannerButton(label,b->{waiting=action;setFocused(null);rebuild();},cx,y+2,cw,20,false).selected(()->waiting==action),"key:"+action.name())
                .setTooltip(Tooltip.create(Component.translatable("stow.settings.key.tip")));
            var reset=add(new PlannerButton(Component.empty(),b->{changeShortcut(action,action.defaults());waiting=null;rebuild();},rx,y+2,20,20,false).icon(UIIcons.Kind.LEFT),"key:"+action.name()+":reset");
            reset.active=!bound.equals(action.defaults());reset.setTooltip(Tooltip.create(Component.translatable("stow.settings.reset",Component.literal(StowShortcuts.label(action.defaults())))));
            return;
        }
        String keep=line.keepId();if(keep==null)return;
        if(keep.equals("#open")){add(new PlannerButton(Component.translatable("stow.settings.open"),b->{category=Category.DEPOSIT;search.setValue("");},cx,y+2,cw,20,false),"keep-open");return;}
        if(keep.equals("#add")){
            keepAdd=add(new StowEditBox(font,bodyX+8,y+3,bodyW-16,18,Component.translatable("stow.settings.keep.add")),"keep-add");
            keepAdd.setHint(Component.translatable("stow.settings.keep.add"));keepAdd.setMaxLength(60);keepAdd.setValue(keepQuery);
            var box=keepAdd;keepAdd.setResponder(v->{keepQuery=v;rebuild();});return;
        }
        if(keep.startsWith("+")){String id=keep.substring(1);
            add(new PlannerButton(Component.translatable("stow.settings.keep.add-one"),b->{changeKeep(id,64);keepQuery="";keepFocus=id;rebuild();},cx,y+2,cw,20,false),"keep+"+id);return;}
        var amount=add(new StowEditBox(font,cx+1,y+3,cw-2,18,Component.translatable("stow.keep.amount")),"keep:"+keep);amount.setMaxLength(7);
        Integer value=Stow.config.keepAmounts.get(keep);amount.setValue(value==null?"":Integer.toString(value));amount.setHint(Component.literal("0"));
        amount.setResponder(v->{int n=parseAmount(v.strip().isEmpty()?"0":v.strip());if(n<0){amount.setTextColor(0xFFFFAA91);return;}amount.setTextColor(0xFFFFFFFF);if(n>0||Stow.config.keepAmounts.containsKey(keep))changeKeep(keep,n);});
        if(keep.equals(keepFocus)&&value==null){setFocused(amount);}
        add(new PlannerButton(Component.literal("×"),b->{changeKeep(keep,0);if(keep.equals(keepFocus))keepFocus="";rebuild();},rx,y+2,20,20,false),"keep-x:"+keep)
            .setTooltip(Tooltip.create(Component.translatable("stow.settings.keep.remove")));
    }
    /** Integer slider that writes through change(), so drags coalesce into one undo step. */
    private final class IntSlider extends AbstractSliderButton {
        private final Setting setting;
        IntSlider(int x,int y,int w,int h,Setting setting){super(x,y,w,h,Component.empty(),fraction(setting));this.setting=setting;updateMessage();}
        private static double fraction(Setting s){return ((Integer)s.get().get()-s.min())/(double)Math.max(1,s.max()-s.min());}
        private int current(){return setting.min()+(int)Math.round(value*(setting.max()-setting.min()));}
        @Override protected void updateMessage(){setMessage(Component.literal(setting.id().contains("scale")?current()+" %":Integer.toString(current())));}
        @Override protected void applyValue(){change(setting,current());}
        @Override public void onRelease(MouseButtonEvent event){super.onRelease(event);rebuild();}
    }

    // ---------------------------------------------------------------- rendering
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float delta){}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(minecraft.level==null)extractPanorama(g,delta);
        extractBlurredBackground(g);extractMenuBackground(g);g.fill(0,0,width,height,0x50000000);
        g.text(font,title,left,8,CREAM);
        String context=search.getValue().isBlank()?category.title().getString():Component.translatable("stow.settings.results").getString();
        right(g,fit(context,w-font.width(title)-16),left+w,8,MUTED);
        if(wide)panel(g,left,top,navW,bottom-top);
        panel(g,bodyX,top,bodyW,bottom-top);
        help=Component.empty();
        g.enableScissor(bodyX+1,top+1,bodyX+bodyW-1,bottom-1);
        int y=top+2-scroll,labelW=controlX()-bodyX-16;
        for(var line:lines){
            int h=line.height();
            if(y+h>top&&y<bottom)drawLine(g,line,y,labelW,mx,my);
            y+=h;
        }
        g.disableScissor();
        int total=total(),visible=bottom-top-4;
        if(total>visible){int track=bottom-top-4,thumb=Math.max(12,track*visible/total),ty=top+2+(track-thumb)*scroll/Math.max(1,total-visible);g.fill(bodyX+bodyW-4,top+2,bodyX+bodyW-2,bottom-2,0xFF383D3F);g.fill(bodyX+bodyW-4,ty,bodyX+bodyW-2,ty+thumb,MUTED);}
        Component footer=waiting!=null?Component.translatable("stow.shortcuts.capture-hint"):help;
        if(!footer.getString().isEmpty()){int fx=left+64+fitW(Component.translatable("stow.settings.undo"),60)+8;text(g,footer.getString(),fx,height-19,left+w-fx,waiting!=null?GOLD:MUTED);}
        for(var child:children())if(child instanceof AbstractWidget widget){
            if(body.contains(widget)){g.enableScissor(bodyX+1,top+1,bodyX+bodyW-1,bottom-1);widget.extractRenderState(g,mx,my>=top&&my<bottom?my:Integer.MIN_VALUE,delta);g.disableScissor();}
            else widget.extractRenderState(g,mx,my,delta);
        }
    }
    private void drawLine(GuiGraphicsExtractor g,Line line,int y,int labelW,int mx,int my){
        int x=bodyX+8;boolean hover=inside(mx,my,bodyX,y,bodyW,line.height())&&my>=top&&my<bottom;
        if(line.header()!=null){
            switch(line.header()){
                case "#controls-head"->{section(g,Component.translatable("stow.settings.controls.action").getString(),x,y+4);section(g,Component.translatable("stow.settings.controls.where").getString(),x+labelW-70,y+4);section(g,Component.translatable("stow.settings.controls.key").getString(),controlX(),y+4);}
                case "#keep-empty"->text(g,Component.translatable("stow.keep.empty").getString(),x,y+3,bodyW-16,DIM);
                case "#version"->text(g,Component.translatable("stow.settings.version",dev.stow.client.ui.WhatsNew.currentVersion()).getString(),x,y+3,bodyW-16,DIM);
                default->section(g,line.header(),x,y+4);
            }
            return;
        }
        if(hover)g.fill(bodyX+2,y,bodyX+bodyW-2,y+line.height(),HOVER);
        if(line.setting()!=null){
            var st=line.setting();boolean focused=keys.entrySet().stream().anyMatch(e->e.getKey()==getFocused()&&e.getValue().startsWith(st.id()));
            if(st.changed())g.fill(bodyX+3,y+8,bodyX+6,y+16,GOLD);
            text(g,st.label().getString(),x,y+8,labelW,CREAM);
            if((hover||focused)&&st.help()!=null)help=st.help();
            else if((hover||focused)&&st.changed())help=Component.translatable("stow.settings.changed");
            return;
        }
        if(line.action()!=null){
            var action=line.action();var bound=StowShortcuts.binding(action);
            if(!bound.equals(action.defaults()))g.fill(bodyX+3,y+8,bodyX+6,y+16,GOLD);
            text(g,action.title().getString(),x,y+3,labelW-76,CREAM);
            var conflicts=StowShortcuts.conflicts(action,bound,Stow.config.shortcuts);
            if(!conflicts.isEmpty())text(g,Component.translatable("stow.settings.controls.conflict",String.join(", ",conflicts)).getString(),x,y+13,labelW-76,CLAY);
            else text(g,action.group().getString(),x,y+13,labelW-76,DIM);
            text(g,Component.translatable("stow.settings.where."+action.where()).getString(),x+labelW-70,y+8,72,MUTED);
            if(hover)help=action.description();
            return;
        }
        String keep=line.keepId();if(keep==null||keep.startsWith("#"))return;
        String id=keep.startsWith("+")?keep.substring(1):keep;
        g.item(MemoryItems.icon(id),x,y+4);text(g,MemoryItems.name(id),x+20,y+8,labelW-20,keep.startsWith("+")?MUTED:CREAM);
        if(hover)help=Component.translatable("stow.keep.hint");
    }

    // ---------------------------------------------------------------- input
    @Override public boolean mouseScrolled(double mx,double my,double sx,double sy){
        if(inside(mx,my,bodyX,top,bodyW,bottom-top)){
            if(waiting==null){for(var wd:body)if(wd instanceof IntSlider&&wd.isMouseOver(mx,my))return super.mouseScrolled(mx,my,sx,sy);}
            scroll-=(int)Math.round(sy*24);rebuild();return true;}
        return super.mouseScrolled(mx,my,sx,sy);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        if(waiting!=null){var action=waiting;waiting=null;changeShortcut(action,StowShortcuts.chord(event.buttonInfo()));rebuild();return true;}
        if(!inside(event.x(),event.y(),bodyX,top,bodyW,bottom-top)){
            var hidden=new ArrayList<AbstractWidget>();for(var wd:body)if(wd.visible){hidden.add(wd);wd.visible=false;}
            try{return super.mouseClicked(event,twice);}finally{for(var wd:hidden)wd.visible=true;}
        }
        return super.mouseClicked(event,twice);
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(waiting!=null){
            var action=waiting;
            if(event.isEscape()){waiting=null;rebuild();return true;}
            if(event.key()==InputConstants.KEY_DELETE||event.key()==InputConstants.KEY_BACKSPACE){waiting=null;changeShortcut(action,new StowConfig.Shortcut(false,-1,0));rebuild();return true;}
            if(Set.of(InputConstants.KEY_LSHIFT,InputConstants.KEY_RSHIFT,InputConstants.KEY_LCONTROL,InputConstants.KEY_RCONTROL,InputConstants.KEY_LALT,InputConstants.KEY_RALT,InputConstants.KEY_LGUI,InputConstants.KEY_RGUI).contains(event.key()))return true;
            waiting=null;changeShortcut(action,StowShortcuts.chord(event));rebuild();return true;
        }
        if(event.isEscape()){if(!search.getValue().isEmpty()&&getFocused()==search){search.setValue("");return true;}onClose();return true;}
        boolean typing=getFocused() instanceof EditBox box&&box!=search;
        if(event.hasControlDown()&&event.key()==InputConstants.KEY_Z&&!typing){undoLast();return true;}
        if(event.hasControlDown()&&event.key()==InputConstants.KEY_F){setFocused(search);search.moveCursorToEnd(false);return true;}
        if(getFocused()==search&&(event.key()==InputConstants.KEY_DOWN||event.key()==InputConstants.KEY_TAB)&&!body.isEmpty()){
            for(var wd:body)if(!(wd instanceof EditBox)||wd!=keepAdd){setFocused(wd);return true;}}
        return super.keyPressed(event);
    }
    @Override public void onClose(){
        Stow.config.save();setFocused(null);Screen back=parent;
        if(back instanceof AbstractContainerScreen<?> container&&minecraft.player!=null&&minecraft.player.containerMenu!=container.getMenu())back=null;
        minecraft.gui.setScreen(back);
    }
    @Override public boolean isPauseScreen(){return false;}
}
