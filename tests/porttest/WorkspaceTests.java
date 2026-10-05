package porttest;

import java.util.*;
import java.nio.file.*;
import dev.stow.*;
import dev.stow.client.memory.*;
import dev.stow.client.hud.*;
import dev.stow.client.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.*;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.properties.ChestType;
import static dev.stow.client.memory.ChestMemoryStore.*;

final class WorkspaceTests {
    static int checks;
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;System.out.println("WORKSPACE PASS: "+message);}
    static int run(Minecraft mc,FeatureTest.TestScreen parent)throws Exception{
        var old=mc.gui.screen();String config=new com.google.gson.Gson().toJson(Stow.config);
        try{
            Stow.config.dockRows=3;
            var store=new ChestMemoryStore(Files.createTempDirectory("stow-workspace-"),"workspace");var ids=List.of("minecraft:cobblestone","minecraft:dirt","minecraft:diamond","minecraft:sand","minecraft:oak_planks");for(var id:ids)store.setGoal(id,1000);
            check(store.hudGoals(3).size()==3&&!store.isHudGoal(ids.getFirst(),3),"ordinary HUD fillers are displayed without becoming starred");
            store.toggleHudGoal(ids.get(4),3);check(store.hudGoals(3).stream().map(MaterialGoal::itemId).toList().equals(List.of(ids.get(4),ids.get(0),ids.get(1))),"one star is first and ordinary goals fill the remaining two rows");
            store.toggleHudGoal(ids.get(3),3);check(store.hudGoals(3).stream().map(MaterialGoal::itemId).toList().equals(List.of(ids.get(4),ids.get(3),ids.get(0))),"two starred goals precede ordinary goals without duplication");
            store.toggleHudGoal(ids.get(4),3);check(store.hudGoals(5).size()==5&&store.hudGoals(5).getFirst().itemId().equals(ids.get(3)),"removing a star keeps that goal available and five rows fill normally");
            store.toggleGoalVisible(ids.get(0));check(store.hudGoals(3).stream().noneMatch(g->g.itemId().equals(ids.get(0))),"hidden unstarred goals are excluded from automatic HUD filling");store.toggleGoalVisible(ids.get(0));
            var materials=new MaterialsScreen(parent,store);mc.gui.setScreen(materials);var search=(EditBox)materials.children().stream().filter(w->w instanceof EditBox box&&box.getMessage().getString().equals("Search name or item ID")).findFirst().orElseThrow();search.setValue("diamond");var amounts=materials.children().stream().filter(w->w instanceof EditBox box&&box.getMessage().getString().equals("Target amount")).toList();check(amounts.size()==1,"material search narrows a long project to its exact visible row");
            ((EditBox)amounts.getFirst()).setValue("321");FeatureTest.press(FeatureTest.button(materials,"Save target"));check(store.goals().stream().filter(g->g.itemId().equals(ids.get(2))).findFirst().orElseThrow().target()==321,"filtered row edits update the correct material instead of the first goal");
            materials.init(320,240);check(FeatureTest.button(materials,"Storage")!=null&&FeatureTest.button(materials,"Projects")!=null,"small GUI size exposes direct menu navigation tabs");
            var selection=new ChestMemoryScreen(parent,store,"",true,null);mc.gui.setScreen(selection);selection.init(320,240);FeatureTest.press(FeatureTest.button(selection,"Storage"));check(mc.gui.screen() instanceof ChestMemoryScreen storage&&!storage.isSelectionMode(),"Storage navigation exits project selection into the full saved-container menu");
            var preview=new DockLayoutScreen(parent);mc.gui.setScreen(preview);preview.init(640,480);var state=new GuiRenderState();preview.extractRenderState(new GuiGraphicsExtractor(mc,state,640,480),-100,-100,0);var icons=new ArrayList<net.minecraft.client.renderer.state.gui.GuiItemRenderState>();state.forEachItem(icons::add);check(icons.size()==2&&icons.get(0).x()==icons.get(1).x(),"equipment layout preview aligns both icons to one right edge");
            var smaller=BuildingStockHud.bounds(320,240,60,4,false);Stow.config.buildingStockScale=200;var larger=BuildingStockHud.bounds(320,240,60,4,false);check(larger.scale()>smaller.scale()&&larger.x()+60*larger.scale()<=320,"building count size increases readability while retaining screen bounds");Stow.config.dockRows=100;Stow.config.save();var saved=FeatureTest.reloadConfig();check(saved.dockRows==5&&saved.buildingStockScale==200,"HUD row count clamps to five and building count size persists");
            nativeOutline(mc);
            polish(mc,parent);
            scrollAndIcons(mc,parent);
        }finally{Stow.config=new com.google.gson.Gson().fromJson(config,StowConfig.class);Stow.config.save();mc.gui.setScreen(old);}
        return checks;
    }
    static EditBox amount(net.minecraft.client.gui.screens.Screen screen){return (EditBox)screen.children().stream().filter(w->w instanceof EditBox b&&b.getMessage().getString().equals("Target amount")).findFirst().orElseThrow();}
    static EditBox search(net.minecraft.client.gui.screens.Screen screen){return (EditBox)screen.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();}
    static void polish(Minecraft mc,FeatureTest.TestScreen parent)throws Exception{
        var oldPlayer=mc.player;int oldScale=mc.options.guiScale().get();var oldStore=ChestMemory.currentStore();
        var store=new ChestMemoryStore(Files.createTempDirectory("stow-polish-"),"polish");
        try{
            for(var item:MemoryItems.catalogue().stream().limit(20).toList())store.setGoal(item.id(),1000);
            FeatureTest.setMemoryField("store",store);Stow.config.dockScale=Stow.config.projectScale=100;Stow.config.buildingStockScale=135;
            for(int gui=1;gui<=4;gui++){
                mc.options.guiScale().set(gui);mc.resizeGui();int w=mc.getWindow().getGuiScaledWidth(),h=mc.getWindow().getGuiScaledHeight();float actual=(float)mc.getWindow().getGuiScale();
                var block=BuildingStockHud.bounds(w,h,100,actual,false);var equipment=SurvivalDock.bounds(w,h,100,40,StowConfig.DockCorner.BOTTOM_RIGHT,8,62,100);var project=SurvivalDock.bounds(w,h,100,54,StowConfig.DockCorner.TOP_RIGHT,8,8,100);
                check(Math.abs(block.scale()-equipment.scale())<.0001f&&Math.abs(block.scale()-project.scale())<.0001f,"stock, equipment and project icons/text share their default size at GUI "+gui);
                var placement=new DockLayoutScreen(parent,true);mc.gui.setScreen(placement);var box=placement.previewBounds();check(box.x()>=0&&box.y()==Stow.config.projectOffsetY&&box.y()+box.height()<=h,"project preview uses the live screen coordinates at GUI "+gui);
            }
            var materials=new MaterialsScreen(parent,store);mc.gui.setScreen(materials);materials.init(320,240);var edit=amount(materials);edit.setValue("456");FeatureTest.press(FeatureTest.button(materials,">"));FeatureTest.press(FeatureTest.button(materials,"<"));check(amount(materials).getValue().equals("456"),"unsaved material quantity survives paging away and back");
            materials.setFocused(amount(materials));amount(materials).moveCursorTo(2,false);materials.init(427,360);check(amount(materials).getValue().equals("456")&&amount(materials).isFocused()&&amount(materials).getCursorPosition()==2,"resize preserves quantity draft, typing focus and caret");
            store.setGoal("minecraft:diamond",48);materials.tick();check(amount(materials).isFocused()&&amount(materials).getValue().equals("456"),"incoming goal additions retain active quantity editing");
            FeatureTest.press(FeatureTest.button(materials,"Save target"));check(store.goals().getFirst().target()==456,"saved draft commits the correct goal after pagination and resize");
            var firstProject=store.activeProjectId();store.createProject("Second");store.setGoal(store.projects().size()>1?MemoryItems.catalogue().getFirst().id():"minecraft:cobblestone",23);materials.tick();check(amount(materials).getValue().equals("23"),"switching projects discards drafts belonging to the previous project");store.switchProject(firstProject);
            var picker=new MaterialPickerScreen(parent,store,"",1000);mc.gui.setScreen(picker);search(picker).setValue("cobblestone");picker.keyPressed(FeatureTest.key(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,13,0));check(amount(picker).isFocused()&&amount(picker).getHighlighted().equals("1000"),"Enter from item search chooses a result and selects its quantity for immediate typing");amount(picker).setValue("42");picker.keyPressed(FeatureTest.key(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,13,0));check(store.goals().stream().anyMatch(g->g.itemId().equals("minecraft:cobblestone")&&g.target()==42)&&mc.gui.screen()==parent,"second Enter saves the selected material and returns to the original screen");
            var projects=new ProjectsScreen(parent,store);mc.gui.setScreen(projects);var name=search(projects);int projectCount=store.projects().size();projects.setFocused(name);name.setValue("Renamed build");projects.keyPressed(FeatureTest.key(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,13,0));check(store.projectName().equals("Renamed build")&&store.projects().size()==projectCount,"Enter edits the active project name without accidentally creating another project");
            var nested=new CommandPaletteScreen(new MaterialsScreen(parent,store),net.minecraft.world.item.ItemStack.EMPTY);mc.gui.setScreen(nested);check(nested.rootScreen()==parent,"palette resolves the original inventory through nested utility screens");search(nested).setValue("shortcuts");check(nested.results().size()==1,"detailed shortcut editor is searchable without cluttering default palette");nested.keyPressed(FeatureTest.key(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,13,0));check(mc.gui.screen() instanceof ShortcutEditorScreen,"palette opens shortcut editor directly");mc.gui.screen().onClose();check(mc.gui.screen()==parent,"palette utility action skips intermediate menu layers on return");
            mc.gui.setScreen(nested);check(!nested.mouseScrolled(0,0,0,-1),"wheel outside palette does not change its selection");
            Stow.config.projectOffsetX=8;Stow.config.projectOffsetY=8;var placement=new DockLayoutScreen(parent,true);mc.gui.setScreen(placement);var box=placement.previewBounds();var press=new net.minecraft.client.input.MouseButtonEvent(box.x()+4,box.y()+4,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));placement.mouseClicked(press,false);placement.mouseDragged(new net.minecraft.client.input.MouseButtonEvent(80,120,press.buttonInfo()),-50,20);placement.mouseReleased(press);check(Stow.config.projectOffsetX==8&&Stow.config.projectOffsetY==8,"project HUD drag remains a draft until Save");FeatureTest.press(FeatureTest.button(placement,"Save"));check(Stow.config.projectOffsetX!=8&&Stow.config.dockScale==100,"project placement Save changes only project HUD configuration");
            mc.player=CompanionTests.previewPlayer(mc,parent.getMenu());Stow.config.dockMaterials=false;Stow.config.dockEnabled=true;Stow.config.equipmentWatch=true;Stow.config.equipmentAll=true;Stow.config.dockCorner=StowConfig.DockCorner.BOTTOM_RIGHT;Stow.config.dockOffsetX=8;Stow.config.dockScale=100;
            var before=new GuiRenderState();SurvivalDock.clear();SurvivalDock.draw(new GuiGraphicsExtractor(mc,before,320,240),null);var icons=new ArrayList<net.minecraft.client.renderer.state.gui.GuiItemRenderState>();before.forEachItem(icons::add);var after=new GuiRenderState();SurvivalDock.notice("stow.deposit.done",524);SurvivalDock.draw(new GuiGraphicsExtractor(mc,after,320,240),null);var noticed=new ArrayList<net.minecraft.client.renderer.state.gui.GuiItemRenderState>();after.forEachItem(noticed::add);check(icons.size()==noticed.size()&&icons.getFirst().x()>0&&noticed.getFirst().x()>0,"equipment retains right-aligned icons when a status heading appears");
            var settings=(me.shedaniel.clothconfig2.gui.AbstractConfigScreen)StowSettingsScreen.create(parent);mc.gui.setScreen(settings);settings.init(320,240);check(settings.getCategorizedEntries().values().stream().flatMap(List::stream).filter(e->e.getFieldName().getString().equals("Show equipment HUD")).count()==1,"equipment visibility has one clearly named setting");
            for(String label:List.of("Shared HUD size (%)","Keep Shift-transfers out of pinned slots","Horizontal margin")){
                var entry=settings.getCategorizedEntries().values().stream().flatMap(List::stream).filter(e->e.getFieldName().getString().equals(label)).findFirst().orElseThrow();
                entry.extractRenderState(new GuiGraphicsExtractor(mc,new GuiRenderState(),320,240),0,10,20,150,entry.getItemHeight(),-100,-100,false,0);
                check(entry.getItemHeight()>24&&entry.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).allMatch(w->w.getY()>=25),"narrow setting wraps its label above native controls: "+label);
            }
        }finally{SurvivalDock.clear();mc.player=oldPlayer;FeatureTest.setMemoryField("store",oldStore);mc.options.guiScale().set(oldScale);mc.resizeGui();}
    }
    static int listIndex(Object screen)throws Exception{Class<?> type=screen.getClass();while(type!=null){try{var f=type.getDeclaredField("page");f.setAccessible(true);return f.getInt(screen);}catch(NoSuchFieldException ignored){type=type.getSuperclass();}}throw new AssertionError("missing index");}
    static void scrollAndIcons(Minecraft mc,FeatureTest.TestScreen parent)throws Exception{
        var old=ChestMemory.currentStore();var oldPlayer=mc.player;
        var store=new ChestMemoryStore(Files.createTempDirectory("stow-motion-"),"motion");
        try{
            for(var item:MemoryItems.catalogue().stream().limit(30).toList())store.setGoal(item.id(),1000);
            FeatureTest.setMemoryField("store",store);
            var materials=new MaterialsScreen(parent,store);mc.gui.setScreen(materials);materials.init(640,480);
            check(materials.mouseScrolled(300,140,0,-1)&&listIndex(materials)==1,"material wheel advances one item instead of a whole page");
            materials.mouseScrolled(300,140,0,-.25);materials.mouseScrolled(300,140,0,-.25);check(listIndex(materials)==1,"high-resolution wheel input accumulates instead of skipping rows");
            materials.mouseScrolled(300,140,0,-.5);check(listIndex(materials)==2,"fractional wheel input produces exactly one row when accumulated");
            check(!materials.mouseScrolled(0,0,0,-1)&&listIndex(materials)==2,"scrolling outside the materials list leaves its position unchanged");
            var motion=new ListMotion();motion.moveTo(1,68,1);check(motion.offset()>0&&motion.offset()<=68,"list transition starts in the scroll direction and never overshoots");Thread.sleep(180);check(motion.offset()==0,"list motion settles after 160ms without a lingering animated hitbox");
            check(motion.wheelSteps(-.4)==0&&motion.wheelSteps(.5)==0&&motion.wheelSteps(.5)==1,"reversing a fractional wheel gesture clears the opposite remainder");
            var picker=new MaterialPickerScreen(parent,store,"",1000);mc.gui.setScreen(picker);picker.init(640,480);picker.mouseScrolled(300,80,0,-1);check(listIndex(picker)==2,"material picker scrolls one two-column row without swapping columns");
            for(int i=0;i<20;i++)store.remember(new SavedChest(new Location("minecraft:overworld",i*3,64,0,"Chest"),"Chest "+i,0,List.of(new MemoryItem("minecraft:cobblestone","Cobblestone",64))));
            var chest=new ChestMemoryScreen(parent,store,"",true,null);mc.gui.setScreen(chest);chest.init(640,480);chest.mouseScrolled(300,90,0,-1);check(listIndex(chest)==2,"storage selection wheel advances one stable two-column row");
            var shortcuts=new ShortcutEditorScreen(parent);mc.gui.setScreen(shortcuts);shortcuts.init(640,480);shortcuts.mouseScrolled(300,90,0,-1);check(listIndex(shortcuts)==1,"shortcut list uses the same one-row scrolling");
            var widget=shortcuts.children().stream().filter(w->w instanceof Button b&&b.getY()==84).map(Button.class::cast).findFirst().orElseThrow();
            for(int frame=0;frame<8;frame++)shortcuts.extractRenderState(new GuiGraphicsExtractor(mc,new GuiRenderState(),640,480),-100,-100,0);
            check(widget.getY()>=84&&widget.getY()<=132,"repeated animation frames retain the original widget position instead of accumulating offsets");
            Thread.sleep(180);shortcuts.extractRenderState(new GuiGraphicsExtractor(mc,new GuiRenderState(),640,480),-100,-100,0);
            check(widget.getY()==84,"scrolling widget hitboxes settle at their original row baseline");

            Stow.config.dockCorner=StowConfig.DockCorner.BOTTOM_RIGHT;Stow.config.dockOffsetX=Stow.config.dockOffsetY=0;mc.player=CompanionTests.previewPlayer(mc,parent.getMenu());
            var editor=new DockLayoutScreen(parent);mc.gui.setScreen(editor);editor.init(640,480);var box=editor.previewBounds();
            var rows=SurvivalDock.equipmentRows();var live=SurvivalDock.bounds(640,480,SurvivalDock.contentWidth(rows,null),SurvivalDock.contentHeight(rows,null),Stow.config.dockCorner,0,0,Stow.config.dockScale);
            check(box.equals(live)&&box.x()+box.width()==640&&box.y()+box.height()==480,"live equipment preview exactly matches the actual bottom-right screen edge");
            var settings=(me.shedaniel.clothconfig2.gui.AbstractConfigScreen)StowSettingsScreen.create(parent);
            // Validate actual downloaded PNG resources and the texture submitted by the client.
            Set<String> textures=new HashSet<>();
            for(var kind:UIIcons.Kind.values()){
                var state=new GuiRenderState();UIIcons.draw(new GuiGraphicsExtractor(mc,state,640,480),kind,20,20,0xFFFFFFFF);
                var sprites=new ArrayList<net.minecraft.client.renderer.state.gui.BlitRenderState>();
                state.forEachElement(e->{if(e instanceof net.minecraft.client.renderer.state.gui.BlitRenderState r)sprites.add(r);},GuiRenderState.TraverseRange.ALL);
                try(var input=mc.getResourceManager().getResourceOrThrow(kind.texture()).open()){
                    var bitmap=javax.imageio.ImageIO.read(input);boolean ink=false;
                    for(int y=0;y<bitmap.getHeight();y++)for(int x=0;x<bitmap.getWidth();x++)ink|=(bitmap.getRGB(x,y)>>>24)>0;
                    check(ink&&bitmap.getWidth()==64&&bitmap.getHeight()==64&&sprites.size()==1
                        &&sprites.getFirst().x1()-sprites.getFirst().x0()==UIIcons.SIZE
                        &&sprites.getFirst().y1()-sprites.getFirst().y0()==UIIcons.SIZE
                        &&sprites.getFirst().textureSetup().texure0().texture().getLabel().contains(kind.texture().getPath()),
                        "downloaded icon loads and renders a complete centered texture: "+kind);
                }
                textures.add(kind.texture().toString());
            }
            check(textures.size()==UIIcons.Kind.values().length,"different actions and drag modes never reuse an icon asset");
        }finally{mc.player=oldPlayer;FeatureTest.setMemoryField("store",old);}
    }
    static void nativeOutline(Minecraft mc){
        var frame=new LevelRenderState();frame.shouldShowEntityOutlines=true;frame.cameraRenderState.pos=net.minecraft.world.phys.Vec3.ZERO;
        check(frame instanceof NativeChestOutline.Frame,"native outline render-state mixin is loaded in the actual Fabric client");
        var chest=new ChestRenderState();chest.blockEntityType=BlockEntityTypes.CHEST;chest.blockPos=BlockPos.ZERO;chest.facing=Direction.SOUTH;chest.type=ChestType.SINGLE;chest.material=ChestRenderState.ChestMaterialType.REGULAR;chest.open=.5f;
        var shulker=new ShulkerBoxRenderState();shulker.blockEntityType=BlockEntityTypes.SHULKER_BOX;shulker.blockPos=new BlockPos(2,0,0);shulker.direction=Direction.UP;shulker.progress=.5f;
        for(var state:List.of(chest,shulker)){
            ((NativeChestOutline.Frame)frame).stow$outlines(List.of(new NativeChestOutline.Entry(state.blockPos,state,null)));var storage=new SubmitNodeStorage();NativeChestOutline.submit(frame,storage,mc.levelRenderer.blockEntityRenderDispatcher());
            check(!storage.order(0).outline.isEmpty(),"animated "+state.blockEntityType+" produces native spectral outline geometry");
            check(storage.getSubmitsPerOrder().values().stream().allMatch(c->c.allPhases().stream().allMatch(p->p==c.outline||p.isEmpty())),"outline transfer contributes no solid, translucent or depth geometry");
        }
        var model=new BlockModelRenderState();new BlockModelResolver(mc.getModelManager()).update(model,Blocks.BARREL.defaultBlockState(),BlockDisplayContext.create());((NativeChestOutline.Frame)frame).stow$outlines(List.of(new NativeChestOutline.Entry(BlockPos.ZERO,null,model)));var storage=new SubmitNodeStorage();NativeChestOutline.submit(frame,storage,mc.levelRenderer.blockEntityRenderDispatcher());check(!storage.order(0).outline.isEmpty(),"barrel model uses the same native spectral outline phase");
        frame.shouldShowEntityOutlines=false;var hidden=new SubmitNodeStorage();NativeChestOutline.submit(frame,hidden,mc.levelRenderer.blockEntityRenderDispatcher());check(hidden.getSubmitsPerOrder().isEmpty(),"panoramic/non-player render frames do not emit storage outlines");
        var doubleState=Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE,ChestType.LEFT);check(NativeChestOutline.positions(BlockPos.ZERO,doubleState).size()==2,"native outline covers both halves of a double chest");
    }
}
