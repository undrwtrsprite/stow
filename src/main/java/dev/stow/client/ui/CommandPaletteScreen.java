package dev.stow.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stow.Stow;
import dev.stow.client.input.StowShortcuts;
import dev.stow.client.input.StowShortcuts.Action;
import dev.stow.client.inventory.*;
import dev.stow.client.memory.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Searchable actions and existing storage/projects; never invents new world navigation. */
public final class CommandPaletteScreen extends MemoryScreenBase {
    public record Command(String title,String detail,String icon,Runnable run,BooleanSupplier enabled) {}
    private final ItemStack context;
    private final List<MemoryItems.Entry> catalogue=MemoryItems.catalogue();
    private List<Command> commands=List.of();private int selected,first,visible,left,top,panelWidth,panelHeight;
    private EditBox search;private String lastQuery="";private final List<Button> rows=new ArrayList<>();
    public CommandPaletteScreen(Screen parent,ItemStack context){super(Component.translatable("stow.palette.title"),parent,null);this.context=context.copy();}
    @Override protected int sidebarWidth(){return 0;}
    @Override protected void init(){
        panelWidth=Math.min(480,width-24);visible=Math.max(1,Math.min(6,(height-126)/28));panelHeight=96+visible*28;
        left=(width-panelWidth)/2;top=Math.max(12,(height-panelHeight)/3);rows.clear();
        search=addRenderableWidget(new EditBox(font,left+10,top+30,panelWidth-20,22,Component.translatable("stow.palette.search")));
        search.setMaxLength(80);search.setHint(Component.translatable("stow.palette.search"));search.setValue(lastQuery);
        search.setResponder(value->{selected=0;first=0;lastQuery=value;refresh();});refresh();setFocused(search);
    }
    private void add(List<Command> list,String key,String icon,Action shortcut,Runnable run,BooleanSupplier enabled){list.add(new Command(Component.translatable(key).getString(),shortcut==null?"":StowShortcuts.label(StowShortcuts.binding(shortcut)),icon,run,enabled));}
    public List<Command> results(){return commands;}
    private void refresh(){
        
        var all=new ArrayList<Command>();var store=ChestMemory.currentStore();Screen origin=rootScreen();
        var container=origin instanceof AbstractContainerScreen<?> screen?screen:null;
        if(store!=null){
            if(!context.isEmpty())all.add(new Command(Component.translatable("stow.palette.track-item",context.getHoverName().getString()).getString(),"","ui:TRACK",()->MaterialPlanner.quickAdd(origin,store,context),()->true));
            add(all,"stow.need.title","ui:MATERIALS",null,()->minecraft.gui.setScreen(new MaterialsScreen(origin,store)),()->true);
            add(all,"stow.menu.storage","ui:STORAGE",null,()->minecraft.gui.setScreen(new ChestMemoryScreen(origin,store,"")),()->true);
            add(all,"stow.menu.projects","ui:PROJECT",Action.PROJECTS,()->minecraft.gui.setScreen(new ProjectsScreen(origin,store)),()->true);
        }
        if(container!=null&&SmartDeposit.supported(container.getMenu()))add(all,"stow.action.deposit","ui:DEPOSIT",Action.DEPOSIT,()->{minecraft.gui.setScreen(origin);SmartDeposit.start(container);},()->true);
        add(all,"stow.watch.title","ui:EQUIPMENT",Action.EQUIPMENT,()->minecraft.gui.setScreen(new EquipmentScreen(origin)),()->true);
        add(all,"stow.settings.title","ui:SETTINGS",null,()->minecraft.gui.setScreen(StowSettingsScreen.create(origin)),()->true);
        if(!search.getValue().isBlank()){
            add(all,"stow.shortcuts.title","ui:KEYBOARD",null,()->minecraft.gui.setScreen(new ShortcutEditorScreen(origin)),()->true);
            add(all,"stow.keep.title","ui:KEEP",Action.KEEP,()->minecraft.gui.setScreen(new KeepAmountsScreen(origin,"")),()->true);
            add(all,"stow.glow.toggle","ui:EYE",null,()->{ChestMemory.setGlowEnabled(!Stow.config.chestGlow);onClose();},()->true);
            add(all,"stow.action.auto_tool","minecraft:iron_shovel",Action.AUTO_TOOL,()->{StowShortcuts.dispatch(Action.AUTO_TOOL,origin,null);onClose();},()->true);
            if(origin==null)add(all,"stow.action.bulk_strip","minecraft:iron_axe",Action.BULK_STRIP,()->{onClose();StowShortcuts.dispatch(Action.BULK_STRIP,null,null);},()->Stow.config.bulkStrip||BulkStrip.busy());
        }
        String query=search.getValue().strip();String[] words=query.toLowerCase(Locale.ROOT).split("\\s+");
        var found=new ArrayList<Command>();
        for(Command command:all){String name=(command.title()+" "+command.detail()).toLowerCase(Locale.ROOT);if(Arrays.stream(words).allMatch(name::contains))found.add(command);}
        if(store!=null&&!query.isBlank()){
            for(var project:store.projects())if(project.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))found.add(new Command(Component.translatable("stow.palette.project",project.name()).getString(),"","ui:PROJECT",()->{store.switchProject(project.id());ChestMemory.saveLater(store);onClose();},()->true));
            if(!store.search(query,"",null).isEmpty())found.add(new Command(Component.translatable("stow.palette.find",query).getString(),"","ui:FIND",()->minecraft.gui.setScreen(new ChestMemoryScreen(origin,store,query)),()->true));
            var need=java.util.regex.Pattern.compile("(?i)need\\s+(\\d{1,7})\\s+(.+)").matcher(query);
            if(!need.matches())for(var item:MemoryItems.search(catalogue,query).stream().limit(3).toList())found.add(new Command(Component.translatable("stow.palette.track-item",item.name()).getString(),"",item.id(),()->minecraft.gui.setScreen(new MaterialPickerScreen(origin,store,item.id(),1000)),()->true));
            if(need.matches()){
                int target=Integer.parseInt(need.group(1));String item=need.group(2);
                if(target>0&&target<=1_000_000)found.add(new Command(Component.translatable("stow.palette.need",target,item).getString(),"","ui:TRACK",()->minecraft.gui.setScreen(new MaterialPickerScreen(origin,store,item,target)),()->true));
            }
        }
        commands=List.copyOf(found);panelHeight=96+Math.max(1,Math.min(visible,commands.size()))*28;selected=Math.clamp(selected,0,Math.max(0,commands.size()-1));
        first=Math.clamp(first,0,Math.max(0,commands.size()-visible));
        if(selected<first)first=selected;if(selected>=first+visible)first=selected-visible+1;
        rows.forEach(this::removeWidget);rows.clear();
        for(int i=first;i<Math.min(commands.size(),first+visible);i++){
            final int index=i;Command command=commands.get(i);
            var button=new PaletteButton(command,index,left+10,top+66+(i-first)*28,panelWidth-24);
            rows.add(addRenderableWidget(button));
        }
        trackList(rows,top+66,top+66+visible*28,first,28,1);
    }
    private void execute(){if(!commands.isEmpty()&&commands.get(selected).enabled().getAsBoolean())commands.get(selected).run().run();}
    @Override public boolean keyPressed(KeyEvent event){
        if(StowShortcuts.matches(Action.PALETTE,StowShortcuts.chord(event))){onClose();return true;}
        if(event.key()==InputConstants.KEY_DOWN||event.key()==InputConstants.KEY_UP){
            selected=Math.clamp(selected+(event.key()==InputConstants.KEY_DOWN?1:-1),0,Math.max(0,commands.size()-1));
            if(selected<first)first=selected;if(selected>=first+visible)first=selected-visible+1;refresh();return true;
        }
        if(event.key()==InputConstants.KEY_RETURN||event.key()==InputConstants.KEY_NUMPADENTER){execute();return true;}
        return super.keyPressed(event);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(vertical!=0&&x>=left&&x<left+panelWidth&&y>=top&&y<top+panelHeight){first=Math.clamp(first+listMotion.wheelSteps(-vertical),0,Math.max(0,commands.size()-visible));selected=Math.clamp(selected,first,Math.max(first,Math.min(commands.size()-1,first+visible-1)));refresh();return true;}return false;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        extractBlurredBackground(g);g.fill(0,0,width,height,0x50000000);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,net.minecraft.resources.Identifier.withDefaultNamespace("textures/gui/menu_list_background.png"),left,top,0,0,panelWidth,panelHeight,panelWidth,panelHeight,32,32,0xFF444444);
        g.outline(left,top,panelWidth,panelHeight,0xFF666666);
        g.text(font,title,left+10,top+11,0xFFFFFFFF);
        String close="Esc";g.text(font,close,left+panelWidth-font.width(close)-10,top+11,0xFFAAAAAA);
        g.text(font,Component.translatable("stow.palette.hint"),left+10,top+panelHeight-17,0xFFAAAAAA);
        if(commands.isEmpty())g.text(font,Component.translatable("stow.palette.empty"),left+10,top+72,0xFFAAAAAA);
        else if(!commands.get(selected).enabled().getAsBoolean())g.text(font,Component.translatable(commands.get(selected).icon().equals("ui:DEPOSIT")?"stow.deposit.open-container":"stow.palette.unavailable"),left+10,top+55,0xFFFFC388);
        if(commands.size()>visible){int track=visible*28-2;int thumb=Math.max(12,track*visible/commands.size());int y0=top+66+(track-thumb)*first/Math.max(1,commands.size()-visible);g.fill(left+panelWidth-8,top+66,left+panelWidth-6,top+66+track,0xFF353535);g.fill(left+panelWidth-8,y0,left+panelWidth-6,y0+thumb,0xFFAAAAAA);}
        beginList(g);endList(g);super.extractRenderState(g,x,y,delta);
        if(search.isFocused()&&search.getValue().isEmpty())g.text(font,font.plainSubstrByWidth(Component.translatable("stow.palette.search").getString(),panelWidth-36),search.getX()+8,search.getY()+7,0xFF777777);
    }
    private final class PaletteButton extends Button {
        private final Command command;private final int index;
        PaletteButton(Command command,int index,int x,int y,int width){super(x,y,width,26,Component.literal(command.title()),b->{selected=index;execute();},DEFAULT_NARRATION);this.command=command;this.index=index;}
        @Override protected void extractContents(GuiGraphicsExtractor g,int x,int y,float delta){
            setOverrideRenderHighlightedSprite(()->selected==index||isHoveredOrFocused());extractDefaultSprite(g);
            if(command.icon().startsWith("ui:"))UIIcons.draw(g,UIIcons.Kind.valueOf(command.icon().substring(3)),getX()+6,getY()+(getHeight()-UIIcons.SIZE)/2,0xFFE7ECEF);else g.item(MemoryItems.icon(command.icon()),getX()+6,getY()+5);
            boolean available=command.enabled().getAsBoolean();String shortcut=command.detail().equals(Component.translatable("stow.shortcuts.unbound").getString())?"":command.detail();int suffix=shortcut.isEmpty()?0:font.width(shortcut)+12;
            g.text(font,font.plainSubstrByWidth(command.title(),getWidth()-38-suffix),getX()+28,getY()+9,available?0xFFFFFFFF:0xFF999999);
            if(!shortcut.isEmpty())g.text(font,shortcut,getRight()-font.width(shortcut)-8,getY()+9,0xFFAAAAAA);
        }
    }
}
