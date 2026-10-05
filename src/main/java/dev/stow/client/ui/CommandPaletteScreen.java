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
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Searchable actions and existing storage/projects; never invents new world navigation. */
public final class CommandPaletteScreen extends ToolListScreen {
    public record Command(String title,String detail,String icon,Runnable run,BooleanSupplier enabled) {}
    private final ItemStack context;
    private List<Command> commands=List.of();private int selected;private String lastQuery="";
    public CommandPaletteScreen(Screen parent,ItemStack context){super(Component.translatable("stow.palette.title"),parent);this.context=context.copy();}
    @Override protected void init(){begin(34,Component.translatable("stow.palette.search"));refresh();setFocused(search);}
    private void add(List<Command> list,String key,String icon,Action shortcut,Runnable run,BooleanSupplier enabled){list.add(new Command(Component.translatable(key).getString(),shortcut==null?"":StowShortcuts.label(StowShortcuts.binding(shortcut)),icon,run,enabled));}
    public List<Command> results(){return commands;}
    @Override protected void refresh(){
        if(!search.getValue().equals(lastQuery)){selected=0;lastQuery=search.getValue();}
        var all=new ArrayList<Command>();var store=ChestMemory.currentStore();
        var container=parent instanceof AbstractContainerScreen<?> screen?screen:null;
        add(all,"stow.workspace.title","minecraft:compass",null,()->minecraft.gui.setScreen(new WorkspaceScreen(this)),()->true);
        add(all,"stow.watch.title","minecraft:iron_chestplate",Action.EQUIPMENT,()->minecraft.gui.setScreen(new EquipmentScreen(this)),()->true);
        add(all,"stow.need.title","minecraft:book",null,()->minecraft.gui.setScreen(new MaterialsScreen(this,store)),()->store!=null);
        add(all,"stow.menu.storage","minecraft:chest",null,()->minecraft.gui.setScreen(new ChestMemoryScreen(this,store,"")),()->store!=null);
        add(all,"stow.menu.projects","minecraft:crafting_table",Action.PROJECTS,()->minecraft.gui.setScreen(new ProjectsScreen(this,store)),()->store!=null);
        add(all,"stow.action.deposit","minecraft:hopper",Action.DEPOSIT,()->{minecraft.gui.setScreen(parent);SmartDeposit.start(container);},()->container!=null&&SmartDeposit.supported(container.getMenu())&&!InventorySorting.busy(container.getMenu())&&!SmartDeposit.busy(container.getMenu())&&SmartDeposit.preview(container.getMenu()).amount()>0);
        add(all,"stow.keep.title","minecraft:bundle",Action.KEEP,()->minecraft.gui.setScreen(new KeepAmountsScreen(this,context.isEmpty()?"":SmartDeposit.itemId(context))),()->true);
        add(all,"stow.shortcuts.title","minecraft:tripwire_hook",null,()->minecraft.gui.setScreen(new ShortcutEditorScreen(this)),()->true);
        add(all,"stow.dock.layout","minecraft:painting",null,()->minecraft.gui.setScreen(new DockLayoutScreen(this)),()->true);
        add(all,"stow.settings.title","minecraft:comparator",null,()->minecraft.gui.setScreen(StowSettingsScreen.create(this)),()->true);
        add(all,Stow.config.dockEnabled?"stow.dock.hide":"stow.dock.show","minecraft:spyglass",Action.DOCK,()->{Stow.config.dockEnabled=!Stow.config.dockEnabled;Stow.config.save();refresh();},()->true);
        if(!context.isEmpty())add(all,"stow.palette.track-held","minecraft:paper",Action.TRACK,()->MaterialPlanner.quickAdd(this,store,context),()->store!=null);
        String query=search.getValue().strip();String[] words=query.toLowerCase(Locale.ROOT).split("\\s+");
        var found=new ArrayList<Command>();
        for(Command command:all){String name=(command.title()+" "+command.detail()).toLowerCase(Locale.ROOT);if(Arrays.stream(words).allMatch(name::contains))found.add(command);}
        if(store!=null&&!query.isBlank()){
            for(var project:store.projects())if(project.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))found.add(new Command(Component.translatable("stow.palette.project",project.name()).getString(),"","minecraft:crafting_table",()->{store.switchProject(project.id());ChestMemory.saveLater(store);onClose();},()->true));
            if(!store.search(query,"",null).isEmpty())found.add(new Command(Component.translatable("stow.palette.find",query).getString(),"","minecraft:chest",()->minecraft.gui.setScreen(new ChestMemoryScreen(this,store,query)),()->true));
            var need=java.util.regex.Pattern.compile("(?i)need\\s+(\\d{1,7})\\s+(.+)").matcher(query);
            if(need.matches()){
                int target=Integer.parseInt(need.group(1));String item=need.group(2);
                if(target>0&&target<=1_000_000)found.add(new Command(Component.translatable("stow.palette.need",target,item).getString(),"","minecraft:paper",()->minecraft.gui.setScreen(new MaterialPickerScreen(this,store,item,target)),()->true));
            }
        }
        commands=List.copyOf(found);resetRows(commands.size());selected=Math.clamp(selected,page*pageSize,Math.max(page*pageSize,Math.min(commands.size()-1,(page+1)*pageSize-1)));
        for(int i=page*pageSize;i<Math.min(commands.size(),(page+1)*pageSize);i++){
            Command command=commands.get(i);final int index=i;
            var button=row(new PlannerButton(Component.literal(command.title()),b->{if(command.enabled().getAsBoolean())command.run().run();},left+8,rowY(i-page*pageSize),bodyWidth-16,28,false).item(command.icon()).selected(()->selected==index));
            button.active=command.enabled().getAsBoolean();
        }
    }
    @Override public boolean keyPressed(KeyEvent event){
        if(event.key()==InputConstants.KEY_DOWN||event.key()==InputConstants.KEY_UP){
            selected=Math.clamp(selected+(event.key()==InputConstants.KEY_DOWN?1:-1),0,Math.max(0,commands.size()-1));page=selected/pageSize;refresh();return true;
        }
        if(event.key()==InputConstants.KEY_RETURN||event.key()==InputConstants.KEY_NUMPADENTER){if(!commands.isEmpty()&&commands.get(selected).enabled().getAsBoolean())commands.get(selected).run().run();return true;}
        return super.keyPressed(event);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        chrome(g,delta);g.text(font,Component.translatable("stow.palette.hint"),left+8,72,0xFFAAAAAA);
        if(commands.isEmpty())g.text(font,Component.translatable("stow.palette.empty"),left+8,92,0xFFAAAAAA);
        super.extractRenderState(g,x,y,delta);
    }
}
