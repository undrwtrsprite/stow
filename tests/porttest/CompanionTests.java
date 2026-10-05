package porttest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stow.*;
import dev.stow.StowConfig.Shortcut;
import dev.stow.client.hud.*;
import dev.stow.client.input.StowShortcuts;
import dev.stow.client.input.StowShortcuts.Action;
import dev.stow.client.inventory.*;
import dev.stow.client.memory.*;
import dev.stow.client.ui.*;
import java.util.*;
import java.lang.reflect.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

final class CompanionTests {
    private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;System.out.println("COMPANION PASS: "+message);}
    static int run(Minecraft mc,FeatureTest.TestScreen parent)throws Exception {
        var config=Stow.config;var shortcuts=new LinkedHashMap<>(config.shortcuts);var keep=new LinkedHashMap<>(config.keepAmounts);
        var previous=mc.gui.screen();
        try{marker(mc);bindings(mc,parent);keepRules(mc,parent);deposit(mc,parent);layouts(mc,parent);}
        finally{config.shortcuts=shortcuts;config.keepAmounts=keep;Stow.config=config;config.save();SmartDeposit.cancel();SurvivalDock.clear();mc.gui.setScreen(previous);}
        return checks;
    }
    private static void marker(Minecraft mc){
        var state=new GuiRenderState();SlotMarkers.lock(new GuiGraphicsExtractor(mc,state,320,240),8,0);
        List<net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState> rectangles=new ArrayList<>();state.forEachElement(e->{if(e instanceof net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState r)rectangles.add(r);},GuiRenderState.TraverseRange.ALL);
        check(rectangles.size()>=8&&rectangles.stream().anyMatch(r->r.col1()==0xFF20262A&&Math.abs(r.x1()-r.x0())==1&&Math.abs(r.y1()-r.y0())==2),"pin marker has a padlock silhouette and visible keyhole");
    }
    private static EditBox field(Screen screen){return (EditBox)screen.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();}
    private static void bindings(Minecraft mc,FeatureTest.TestScreen parent)throws Exception{
        check(StowShortcuts.matches(Action.PALETTE,new Shortcut(false,InputConstants.KEY_K,InputConstants.MOD_CONTROL)),"palette defaults to Ctrl+K");
        check(StowShortcuts.matches(Action.SORT,new Shortcut(true,InputConstants.MOUSE_BUTTON_MIDDLE,InputConstants.MOD_SHIFT))&&StowShortcuts.matches(Action.SORT,new Shortcut(true,InputConstants.MOUSE_BUTTON_MIDDLE,InputConstants.MOD_CONTROL)),"sort shortcut retains Shift and Ctrl order variants");
        var edited=new Shortcut(false,InputConstants.KEY_L,InputConstants.MOD_CONTROL);
        mc.gui.setScreen(new ShortcutEditorScreen(parent));var editor=(ShortcutEditorScreen)mc.gui.screen();field(editor).setValue("Pin slot");FeatureTest.press(FeatureTest.button(editor,StowShortcuts.label(Action.PIN.defaults())));
        editor.keyPressed(FeatureTest.key(InputConstants.KEY_L,InputConstants.KEYCODE_L,InputConstants.MOD_CONTROL));
        check(StowShortcuts.binding(Action.PIN).equals(Action.PIN.defaults()),"shortcut edits remain a draft before Save");FeatureTest.press(FeatureTest.button(editor,"Save"));
        check(StowShortcuts.binding(Action.PIN).equals(edited)&&FeatureTest.reloadConfig().shortcuts.get("PIN").equals(edited),"shortcut Save persists key and modifiers");
        var slot=parent.getMenu().slots.get(4);boolean pinned=PinnedSlots.isPinned(slot);
        check(StowShortcuts.inventoryKey(parent,slot,FeatureTest.key(InputConstants.KEY_L,InputConstants.KEYCODE_L,InputConstants.MOD_CONTROL))&&PinnedSlots.isPinned(slot)!=pinned,"edited pin shortcut operates on a real inventory slot");PinnedSlots.toggle(slot);
        Stow.config.shortcuts.put("TRACK",edited);
        check(!StowShortcuts.matches(Action.PIN,edited)&&!StowShortcuts.matches(Action.TRACK,edited),"duplicate stow bindings cannot trigger ambiguous actions");Stow.config.shortcuts.remove("TRACK");
        var vanillaInventory=InputConstants.getKey(mc.options.keyInventory.saveString());
        check(!StowShortcuts.conflicts(Action.DEPOSIT,new Shortcut(vanillaInventory.getType()==InputConstants.Type.MOUSE,vanillaInventory.getValue(),0),Map.of()).isEmpty(),"editor detects a Minecraft control conflict");
        mc.gui.setScreen(new ShortcutEditorScreen(parent));editor=(ShortcutEditorScreen)mc.gui.screen();field(editor).setValue("Pin slot");FeatureTest.press(FeatureTest.button(editor,StowShortcuts.label(edited)));editor.keyPressed(FeatureTest.key(InputConstants.KEY_DELETE,127,0));FeatureTest.press(FeatureTest.button(editor,"Back"));
        check(StowShortcuts.binding(Action.PIN).equals(edited),"Back discards shortcut edits");Stow.config.shortcuts.clear();Stow.config.save();
        mc.gui.setScreen(new CommandPaletteScreen(parent,ItemStack.EMPTY));var palette=(CommandPaletteScreen)mc.gui.screen();field(palette).setValue("equipment");
        check(palette.results().size()==1,"palette filters action names");palette.keyPressed(FeatureTest.key(InputConstants.KEY_RETURN,13,0));check(mc.gui.screen() instanceof EquipmentScreen,"Enter executes selected palette action");mc.gui.screen().onClose();check(mc.gui.screen()==palette,"tool returns to palette");palette.onClose();check(mc.gui.screen()==parent,"palette returns to the inventory");
    }
    private static void keepRules(Minecraft mc,FeatureTest.TestScreen parent){
        Stow.config.keepAmounts=new LinkedHashMap<>(Map.of("minecraft:cobblestone",10));mc.gui.setScreen(new KeepAmountsScreen(parent,"minecraft:cobblestone"));var screen=(KeepAmountsScreen)mc.gui.screen();
        var amount=(EditBox)screen.children().stream().filter(EditBox.class::isInstance).skip(1).findFirst().orElseThrow();amount.setValue("64");
        check(Stow.config.keepAmounts.get("minecraft:cobblestone")==10,"keep amounts are staged before Save");FeatureTest.press(FeatureTest.button(screen,"Save"));check(Stow.config.keepAmounts.get("minecraft:cobblestone")==64,"keep amount saves the exact minimum");
        mc.gui.setScreen(new KeepAmountsScreen(parent,"minecraft:cobblestone"));screen=(KeepAmountsScreen)mc.gui.screen();amount=(EditBox)screen.children().stream().filter(EditBox.class::isInstance).skip(1).findFirst().orElseThrow();amount.setValue("-5");
        check(!FeatureTest.button(screen,"Save").active&&KeepAmountsScreen.parse("1000001")<0,"invalid keep amounts cannot be saved");screen.onClose();check(Stow.config.keepAmounts.get("minecraft:cobblestone")==64,"cancel keeps the existing minimum");
        Stow.config.keepAmounts.clear();
    }
    private static void deposit(Minecraft mc,FeatureTest.TestScreen parent)throws Exception{
        Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);
        var player=(net.minecraft.client.player.RemotePlayer)unsafe.allocateInstance(net.minecraft.client.player.RemotePlayer.class);
        var level=(net.minecraft.client.multiplayer.ClientLevel)unsafe.allocateInstance(net.minecraft.client.multiplayer.ClientLevel.class);
        var listener=(net.minecraft.client.multiplayer.ClientPacketListener)unsafe.allocateInstance(net.minecraft.client.multiplayer.ClientPacketListener.class);
        FeatureTest.setField(net.minecraft.client.multiplayer.ClientPacketListener.class,listener,"enabledFeatures",net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
        FeatureTest.setField(net.minecraft.client.multiplayer.ClientLevel.class,level,"connection",listener);FeatureTest.setField(net.minecraft.world.entity.Entity.class,player,"level",level);
        Inventory inventory=new Inventory(player,new EntityEquipment());FeatureTest.setField(Player.class,player,"inventory",inventory);FeatureTest.setField(Player.class,player,"abilities",new Abilities());
        var chest=new SimpleContainer(27);var menu=ChestMenu.threeRows(44,inventory,chest);player.containerMenu=menu;
        chest.setItem(0,new ItemStack(Items.COBBLESTONE,60));inventory.setItem(0,new ItemStack(Items.COBBLESTONE,10));inventory.setItem(9,new ItemStack(Items.COBBLESTONE,64));inventory.setItem(10,new ItemStack(Items.COBBLESTONE,40));inventory.setItem(11,new ItemStack(Items.COBBLESTONE,14));
        ItemStack named=new ItemStack(Items.COBBLESTONE,7);named.set(DataComponents.CUSTOM_NAME,Component.literal("Do not mix"));inventory.setItem(12,named);inventory.setItem(13,new ItemStack(Items.DIAMOND,5));
        Slot pinned=menu.slots.stream().filter(s->s.container==inventory&&s.getContainerSlot()==10).findFirst().orElseThrow();PinnedSlots.toggle(pinned);
        var before=FeatureTest.contents(menu);var plan=SmartDeposit.plan(menu,player,Map.of("minecraft:cobblestone",80),true);
        check(plan.amount()==55&&!plan.affects(pinned),"deposit preview preserves pins, hotbar and aggregate keep amount");
        check(plan.moves().stream().noneMatch(m->m.source().getItem().has(DataComponents.CUSTOM_NAME)||m.source().getItem().is(Items.DIAMOND)),"deposit skips differently named items and types absent from storage");
        var session=new SmartDeposit.Session(menu,plan,(id,button)->menu.clicked(id,button,ContainerInput.PICKUP,player));while(session.step())check(menu.getCarried().isEmpty(),"deposit cycle finishes with an empty cursor");
        check(!session.failed()&&session.moved()==55&&menu.getCarried().isEmpty()&&FeatureTest.contents(menu).equals(before),"partial deposit uses vanilla transactions without losing items or components");
        check(MaterialPlanner.inventoryCount(inventory,ItemStack.EMPTY,"minecraft:cobblestone")==80&&inventory.getItem(0).getCount()==10&&inventory.getItem(10).getCount()==40,"deposit leaves exactly the keep minimum and protected stacks");PinnedSlots.toggle(pinned);
        check(SmartDeposit.plan(parent.getMenu(),player,Map.of(),false).amount()==0,"machine/custom menus are excluded from deposit");
        chest.clearContent();check(SmartDeposit.plan(menu,player,Map.of(),false).amount()==0,"empty storage does not acquire arbitrary item types");
        for(int i=0;i<27;i++)chest.setItem(i,new ItemStack(Items.COBBLESTONE,64));check(SmartDeposit.plan(menu,player,Map.of(),false).amount()==0,"full storage produces no deposit");
        chest.clearContent();chest.setItem(0,new ItemStack(Items.COBBLESTONE,1));var stale=SmartDeposit.plan(menu,player,Map.of(),false);inventory.setItem(9,new ItemStack(Items.COBBLESTONE,50));int[] sent={0};session=new SmartDeposit.Session(menu,stale,(id,button)->sent[0]++);inventory.getItem(9).shrink(1);
        check(!session.step()&&sent[0]==0&&session.failed(),"incoming changes stop the pending plan before sending clicks");
        var random=new Random(8321);
        for(int fixture=0;fixture<25;fixture++){
            chest.clearContent();inventory.clearContent();menu.setCarried(ItemStack.EMPTY);
            for(int i=0;i<27;i++)if(random.nextBoolean()){var item=new ItemStack(random.nextBoolean()?Items.COBBLESTONE:Items.DIRT,1+random.nextInt(64));if(random.nextInt(5)==0)item.set(DataComponents.CUSTOM_NAME,Component.literal("Named"));chest.setItem(i,item);}
            for(int i=0;i<36;i++)if(random.nextBoolean()){var item=new ItemStack(random.nextBoolean()?Items.COBBLESTONE:Items.DIRT,1+random.nextInt(64));if(random.nextInt(5)==0)item.set(DataComponents.CUSTOM_NAME,Component.literal("Named"));inventory.setItem(i,item);}
            var keep=Map.of("minecraft:cobblestone",random.nextInt(200),"minecraft:dirt",random.nextInt(200));var totals=FeatureTest.contents(menu);
            long stone=MaterialPlanner.inventoryCount(inventory,ItemStack.EMPTY,"minecraft:cobblestone"),dirt=MaterialPlanner.inventoryCount(inventory,ItemStack.EMPTY,"minecraft:dirt");
            var moves=SmartDeposit.plan(menu,player,keep,fixture%2==0);session=new SmartDeposit.Session(menu,moves,(id,button)->menu.clicked(id,button,ContainerInput.PICKUP,player));int ticks=0;while(session.step()){if(++ticks>40)throw new AssertionError("deposit did not finish");if(!menu.getCarried().isEmpty())throw new AssertionError("carried item after cycle");}
            check(!session.failed()&&FeatureTest.contents(menu).equals(totals)&&menu.getCarried().isEmpty(),"random deposit "+fixture+" preserves every count and component");
            check(MaterialPlanner.inventoryCount(inventory,ItemStack.EMPTY,"minecraft:cobblestone")>=Math.min(stone,keep.get("minecraft:cobblestone"))&&MaterialPlanner.inventoryCount(inventory,ItemStack.EMPTY,"minecraft:dirt")>=Math.min(dirt,keep.get("minecraft:dirt")),"random deposit "+fixture+" respects both keep amounts");
        }
        chest.clearContent();inventory.clearContent();var bundle=new ItemStack(Items.BUNDLE,1);bundle.set(DataComponents.BUNDLE_CONTENTS,new net.minecraft.world.item.component.BundleContents(List.of(new ItemStackTemplate(Items.DIAMOND,5))));chest.setItem(0,bundle.copy());inventory.setItem(9,bundle.copy());
        var bundled=FeatureTest.contents(menu);session=new SmartDeposit.Session(menu,SmartDeposit.plan(menu,player,Map.of(),false),(id,button)->menu.clicked(id,button,ContainerInput.PICKUP,player));while(session.step()){}
        check(!session.failed()&&inventory.getItem(9).isEmpty()&&FeatureTest.contents(menu).equals(bundled),"filled bundles move intact into empty storage slots");
        var box=new SimpleContainer(27);box.setItem(0,new ItemStack(Items.SHULKER_BOX,1));inventory.clearContent();inventory.setItem(9,new ItemStack(Items.SHULKER_BOX,1));var shulker=new ShulkerBoxMenu(45,inventory,box);
        check(SmartDeposit.plan(shulker,player,Map.of(),false).amount()==0,"shulker deposit respects the ban on nested shulker boxes");
        // The real button and queue use the same menu transactions as the planner harness.
        chest.clearContent();inventory.clearContent();chest.setItem(0,new ItemStack(Items.COBBLESTONE,1));inventory.setItem(9,new ItemStack(Items.COBBLESTONE,64));
        var local=(net.minecraft.client.player.LocalPlayer)unsafe.allocateInstance(net.minecraft.client.player.LocalPlayer.class);FeatureTest.setField(Player.class,local,"inventory",inventory);FeatureTest.setField(Player.class,local,"abilities",new Abilities());FeatureTest.setField(net.minecraft.world.entity.Entity.class,local,"level",level);FeatureTest.setField(net.minecraft.client.player.LocalPlayer.class,local,"minecraft",mc);local.containerMenu=menu;
        var oldPlayer=mc.player;var oldMode=mc.gameMode;var oldScreen=mc.gui.screen();
        try{
            mc.player=local;mc.gameMode=new net.minecraft.client.multiplayer.MultiPlayerGameMode(mc,listener){@Override public void handleContainerInput(int mid,int slot,int button,ContainerInput input,Player ignored){menu.clicked(slot,button,input,player);}};
            var screen=new FeatureTest.MemoryTestScreen(menu,inventory);mc.gui.setScreen(screen);FeatureTest.press(FeatureTest.button(screen,"Smart deposit"));check(SmartDeposit.busy(menu),"inventory deposit icon starts the actual queue");while(SmartDeposit.busy(menu))SmartDeposit.tick(mc);
            check(inventory.getItem(9).isEmpty()&&menu.getCarried().isEmpty(),"live deposit queue transfers the stack and clears the cursor");
            equipment(mc,inventory);
            var dockState=new GuiRenderState();SurvivalDock.draw(new GuiGraphicsExtractor(mc,dockState,320,240),null);List<Object> icons=new ArrayList<>();dockState.forEachItem(icons::add);check(!icons.isEmpty(),"equipment warning icons render on the survival dock");
            List<Object> rectangles=new ArrayList<>();dockState.forEachElement(rectangles::add,GuiRenderState.TraverseRange.ALL);check(rectangles.isEmpty(),"HUD emits no background or fullscreen tint");
            var tracker=new FloatingMaterialTracker(200,40,100,120,id->{});tracker.update(null,true);check(tracker.visible,"equipment warnings appear beside the inventory even without material goals");
            tracker.mouseClicked(new MouseButtonEvent(202,42,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false);check(mc.gui.screen() instanceof EquipmentScreen,"inventory equipment icon opens its durability details");mc.gui.screen().onClose();
        }finally{SmartDeposit.cancel();SurvivalDock.clear();mc.player=oldPlayer;mc.gameMode=oldMode;mc.gui.setScreen(oldScreen);}
    }
    private static void equipment(Minecraft mc,Inventory inventory){
        for(var item:List.of(Items.IRON_PICKAXE,Items.IRON_HELMET))item.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE,1).set(DataComponents.MAX_DAMAGE,250).set(DataComponents.DAMAGE,0).set(DataComponents.ITEM_NAME,Component.translatable(item.getDescriptionId())).set(DataComponents.ITEM_MODEL,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)).build());
        inventory.clearContent();inventory.setSelectedSlot(0);var worn=new ItemStack(Items.IRON_PICKAXE);worn.setDamageValue(230);inventory.setItem(0,worn);inventory.setItem(1,worn.copy());
        int helmet=Inventory.EQUIPMENT_SLOT_MAPPING.int2ObjectEntrySet().stream().filter(e->e.getValue()==net.minecraft.world.entity.EquipmentSlot.HEAD).findFirst().orElseThrow().getIntKey();var hat=new ItemStack(Items.IRON_HELMET);hat.setDamageValue(240);inventory.setItem(helmet,hat);
        var entries=EquipmentWatch.entries(inventory);check(entries.size()==2&&entries.getFirst().slot()==helmet,"watch includes held and worn items but skips backpack spares");check(EquipmentWatch.warnings(inventory).size()==2&&entries.getFirst().remaining()==10,"watch reports exact remaining durability and urgent-first warnings");
        inventory.getItem(0).setDamageValue(0);check(EquipmentWatch.warnings(inventory).size()==1,"repair immediately removes the held-item warning");inventory.setSelectedSlot(2);check(EquipmentWatch.entries(inventory).size()==1,"switching hands updates the monitored equipment");
    }
    private static void layouts(Minecraft mc,Screen parent){
        for(int width:List.of(320,427,640,854))for(int height:List.of(240,360,480,720)){
            for(Screen screen:List.of(new CommandPaletteScreen(parent,ItemStack.EMPTY),new ShortcutEditorScreen(parent),new KeepAmountsScreen(parent,"cobble"),new EquipmentScreen(parent),new WorkspaceScreen(parent),new DockLayoutScreen(parent))){
                screen.init(width,height);screen.extractRenderState(new GuiGraphicsExtractor(mc,new GuiRenderState(),width,height),-100,-100,0);
                var widgets=screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).toList();
                for(var widget:widgets)if(widget.getX()<0||widget.getY()<0||widget.getRight()>width||widget.getBottom()>height)throw new AssertionError("companion widget outside "+screen.getTitle().getString()+" "+width+"x"+height);
                for(int i=0;i<widgets.size();i++)for(int j=i+1;j<widgets.size();j++){var a=widgets.get(i);var b=widgets.get(j);if(a.getX()<b.getRight()&&a.getRight()>b.getX()&&a.getY()<b.getBottom()&&a.getBottom()>b.getY())throw new AssertionError("companion controls overlap "+screen.getTitle().getString());}
            }
            for(var corner:StowConfig.DockCorner.values()){var box=SurvivalDock.bounds(width,height,180,190,corner,1000,1000,150);check(box.x()>=0&&box.y()>=0&&box.x()+box.width()<=width&&box.y()+box.height()<=height,"HUD stays in bounds at "+width+"x"+height+" "+corner);}
        }
        check(true,"all companion screens have non-overlapping controls at every tested viewport");
        var originalX=Stow.config.dockOffsetX;mc.gui.setScreen(new DockLayoutScreen(parent));var editor=(DockLayoutScreen)mc.gui.screen();var box=editor.previewBounds();var press=new MouseButtonEvent(box.x()+4,box.y()+4,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));editor.mouseClicked(press,false);editor.mouseDragged(new MouseButtonEvent(80,120,press.buttonInfo()),-50,-20);editor.mouseReleased(press);
        check(Stow.config.dockOffsetX==originalX,"dragging HUD preview leaves live settings unchanged");FeatureTest.press(FeatureTest.button(editor,"Save"));check(Stow.config.dockOffsetX!=originalX,"HUD placement applies on Save");Stow.config.dockOffsetX=originalX;Stow.config.dockOffsetY=62;
    }
    static net.minecraft.client.player.LocalPlayer previewPlayer(Minecraft mc,AbstractContainerMenu menu)throws Exception{
        Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);
        var player=(net.minecraft.client.player.LocalPlayer)unsafe.allocateInstance(net.minecraft.client.player.LocalPlayer.class);
        var inventory=new Inventory(player,new EntityEquipment());FeatureTest.setField(Player.class,player,"inventory",inventory);FeatureTest.setField(Player.class,player,"abilities",new Abilities());FeatureTest.setField(net.minecraft.client.player.LocalPlayer.class,player,"minecraft",mc);player.containerMenu=menu;
        var pickaxe=new ItemStack(Items.IRON_PICKAXE);pickaxe.setDamageValue(230);inventory.setItem(0,pickaxe);
        int helmet=Inventory.EQUIPMENT_SLOT_MAPPING.int2ObjectEntrySet().stream().filter(e->e.getValue()==net.minecraft.world.entity.EquipmentSlot.HEAD).findFirst().orElseThrow().getIntKey();var hat=new ItemStack(Items.IRON_HELMET);hat.setDamageValue(240);inventory.setItem(helmet,hat);
        for(int i=9;i<17;i++)inventory.setItem(i,new ItemStack(Items.COBBLESTONE,64));inventory.setItem(17,new ItemStack(Items.COBBLESTONE,12));return player;
    }
}
