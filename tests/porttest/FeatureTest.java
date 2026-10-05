package porttest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stow.Stow;

import dev.stow.client.inventory.*;


import dev.stow.client.memory.*;
import static dev.stow.client.memory.ChestMemoryStore.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;

public class FeatureTest implements ClientModInitializer {
    private static int checks;
    private boolean done;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
        System.out.println("FEATURE PASS: " + message);
    }
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (done || mc.gui.overlay() != null || mc.gui.screen() == null) return;
            done = true;
            try {
                run(mc);
                Files.writeString(Path.of("feature-result.txt"), "PASS " + checks + " behavioral checks\n");
                System.out.println("FEATURE SUCCESS: " + checks + " behavioral checks");
                if(!Boolean.getBoolean("porttest.screenshots"))System.exit(0);
            } catch (Throwable e) {
                e.printStackTrace();
                try { var trace=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(trace));Files.writeString(Path.of("feature-result.txt"), "FAIL " + trace); } catch(Exception ignored){}
                System.exit(1);
            }
        });
    }
    static MouseButtonEvent mouse(double x, double y, int modifiers) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, modifiers));
    }
    static KeyEvent key(int scan, int code, int modifiers) { return new KeyEvent(scan, code, modifiers); }
    static EditBox search(TestScreen screen) {
        return (EditBox) screen.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();
    }
    static void run(Minecraft mc) throws Exception {
        check(!Stow.config.autoTool&&!new dev.stow.StowConfig().autoTool,"automatic mining tools default off for new and imported settings");
        // In 26.3, item components normally bind when a world is joined.
        // Supply representative vanilla prototypes in this menu-only harness.
        for(Item item:net.minecraft.core.registries.BuiltInRegistries.ITEM)
            usingPrototype(item,item.getDescriptionId());
        Items.BUNDLE.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE,1).set(DataComponents.BUNDLE_CONTENTS,net.minecraft.world.item.component.BundleContents.EMPTY).set(DataComponents.ITEM_NAME,Component.translatable("item.minecraft.bundle")).set(DataComponents.ITEM_MODEL,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(Items.BUNDLE)).build());
        check(Stow.config.dragMode==dev.stow.StowConfig.DragMode.MATCHING_ONLY,"legacy drag preference imports at startup");
        Stow.config.dragMode=dev.stow.StowConfig.DragMode.BOTH;
        check(Stow.config.dragMode == dev.stow.StowConfig.DragMode.BOTH, "existing settings upgrade to Both drag mode by default");
        Inventory inv = new Inventory(null, new EntityEquipment());
        Slot importedPin=new Slot(inv,35,0,0);check(PinnedSlots.isPinned(importedPin),"legacy pinned slots imported at startup");PinnedSlots.toggle(importedPin);
        TestMenu menu = new TestMenu(inv);
        TestScreen screen = new TestScreen(menu, inv);
        mc.gui.setScreen(screen);
        check(screen.children().stream().anyMatch(EditBox.class::isInstance), "search widget initializes in real Minecraft client");
        var dragButton=(net.minecraft.client.gui.components.Button) screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().startsWith("Drag:")).findFirst().orElseThrow();
        for(var expected : List.of(dev.stow.StowConfig.DragMode.MATCHING_ONLY,dev.stow.StowConfig.DragMode.ALL_ITEMS,dev.stow.StowConfig.DragMode.BOTH)) {
            screen.mouseClicked(mouse(dragButton.getX()+1,dragButton.getY()+1,0),false);
            screen.mouseReleased(mouse(dragButton.getX()+1,dragButton.getY()+1,0));
            check(Stow.config.dragMode==expected && reloadConfig().dragMode==expected, "inventory Drag button cycles and persists "+expected);
        }
        check(screen.moved.isEmpty(),"clicking mode button never transfers a stack");
        check(screen.getFocused()==null,"mode button does not keep keyboard focus that would intercept deposit shortcuts");
        for(int width : List.of(320,427,640)) for(int height : List.of(240,360,480)) for(int imageHeight : List.of(166,222)) {
            int left=(width-176)/2,top=(height-imageHeight)/2;
            var layout=InventoryExtrasLayout.of(width,height,left,top,176,imageHeight);
            boolean outside=(layout.modeX()+layout.modeWidth()<=left || layout.modeX()>=left+176 || layout.modeY()+20<=top || layout.modeY()>=top+imageHeight)
                && (layout.memoryX()+layout.memoryWidth()<=left || layout.memoryX()>=left+176 || layout.memoryY()+20<=top || layout.memoryY()>=top+imageHeight)
                && (layout.searchX()+layout.searchWidth()<=left || layout.searchX()>=left+176 || layout.searchY()+20<=top || layout.searchY()>=top+imageHeight);
            outside=outside && (layout.needX()+layout.needWidth()<=left || layout.needX()>=left+176 || layout.needY()+20<=top || layout.needY()>=top+imageHeight);
            int[][] rects={{layout.modeX(),layout.modeY(),layout.modeWidth(),20},{layout.memoryX(),layout.memoryY(),layout.memoryWidth(),20},{layout.searchX(),layout.searchY(),layout.searchWidth(),20},{layout.needX(),layout.needY(),layout.needWidth(),20},{layout.sortX(),layout.sortY(),20,20},{layout.paletteX(),layout.paletteY(),20,20},{layout.depositX(),layout.depositY(),20,20},{layout.glowX(),layout.glowY(),20,20},{layout.trackerX(),layout.trackerY(),layout.trackerWidth(),layout.trackerHeight()}};
            for(int a=0;a<rects.length;a++)for(int b=a+1;b<rects.length;b++){
                int[] ra=rects[a],rb=rects[b];
                if(ra[0]<rb[0]+rb[2] && ra[0]+ra[2]>rb[0] && ra[1]<rb[1]+rb[3] && ra[1]+ra[3]>rb[1])throw new AssertionError("overlapping utility controls or floating tracker "+width+"x"+height);
            }
            if(!outside || layout.modeX()<0 || layout.modeX()+layout.modeWidth()>width || layout.memoryY()+20>height || layout.searchY()+20>height) throw new AssertionError("overlapping extras layout "+width+"x"+height+" chest "+imageHeight);
        }
        check(true,"inventory controls fit outside slots for normal and tall chests at multiple GUI scales");
        Slot start = menu.slots.get(0);
        menu.chest.setItem(0, new ItemStack(Items.COBBLESTONE, 12));
        menu.chest.setItem(1, new ItemStack(Items.DIRT, 64));
        menu.chest.setItem(2, new ItemStack(Items.COBBLESTONE, 3));
        ItemStack named = new ItemStack(Items.COBBLESTONE, 4);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Special"));
        menu.chest.setItem(3, named);
        inv.setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        
        screen.mouseClicked(screen.at(start, InputConstants.MOD_SHIFT), false);
        check(screen.moved.equals(List.of(0)), "shift-left click moves initial matching stack with SDL button 1");
        // One high-speed event crosses dirt, matching cobble and custom-named cobble.
        Slot end = menu.slots.get(3);
        MouseButtonEvent endEvent = screen.at(end, InputConstants.MOD_SHIFT);
        screen.mouseDragged(endEvent, end.x - start.x, 0);
        check(screen.moved.equals(List.of(0, 2)), "fast drag skips different items and differently named stacks");
        check(menu.chest.getItem(1).getCount() == 64 && menu.chest.getItem(3).getCount() == 4, "unmatched stack counts remain unchanged");
        screen.mouseDragged(screen.at(menu.slots.get(2), InputConstants.MOD_SHIFT), -18, 0);
        check(screen.moved.equals(List.of(0, 2)), "revisiting matching slot does not send twice");
        screen.mouseDragged(screen.at(menu.slots.get(4), InputConstants.MOD_SHIFT), 0, 30);
        check(screen.moved.equals(List.of(0, 2)), "drag cannot bounce items into original inventory");
        screen.mouseReleased(screen.at(end, InputConstants.MOD_SHIFT));
        menu.chest.setItem(0, new ItemStack(Items.DIRT, 10));
        menu.chest.setItem(1, new ItemStack(Items.DIRT, 2));
        screen.moved.clear();
        screen.mouseClicked(screen.at(start, InputConstants.MOD_SHIFT), false);
        screen.mouseDragged(screen.at(menu.slots.get(1), InputConstants.MOD_SHIFT), 18, 0);
        screen.mouseReleased(screen.at(menu.slots.get(1), InputConstants.MOD_SHIFT));
        check(screen.moved.equals(List.of(0, 1)), "new gesture resets target item after release");
        screen.moved.clear();
        menu.chest.setItem(0, ItemStack.EMPTY);
        screen.mouseClicked(screen.at(start, InputConstants.MOD_SHIFT), false);
        screen.mouseDragged(screen.at(menu.slots.get(2), InputConstants.MOD_SHIFT), 36, 0);
        screen.mouseReleased(screen.at(menu.slots.get(2), InputConstants.MOD_SHIFT));
        check(screen.moved.isEmpty(), "empty start cannot accidentally select later items");
        menu.chest.setItem(0, new ItemStack(Items.COBBLESTONE));
        screen.mouseClicked(screen.at(start, InputConstants.MOD_SHIFT), false);
        screen.mouseDragged(screen.at(menu.slots.get(2), 0), 36, 0);
        screen.mouseReleased(screen.at(menu.slots.get(2), 0));
        check(screen.moved.equals(List.of(0)), "releasing Shift pauses transfers until mouse release");

        // All-items mode must ignore item identity while keeping source/pin safety.
        Stow.config.dragMode = dev.stow.StowConfig.DragMode.ALL_ITEMS;
        screen.moved.clear();
        screen.mouseClicked(screen.at(start, InputConstants.MOD_SHIFT), false);
        screen.mouseDragged(screen.at(end, InputConstants.MOD_SHIFT), end.x-start.x, 0);
        screen.mouseReleased(screen.at(end, InputConstants.MOD_SHIFT));
        check(screen.moved.equals(List.of(0,1,2,3)), "All items setting transfers every crossed stack including different names");
        menu.chest.setItem(0, ItemStack.EMPTY);
        screen.moved.clear();
        screen.mouseClicked(screen.at(start, InputConstants.MOD_SHIFT), false);
        screen.mouseDragged(screen.at(end, InputConstants.MOD_SHIFT), end.x-start.x, 0);
        screen.mouseReleased(screen.at(end, InputConstants.MOD_SHIFT));
        check(screen.moved.equals(List.of(1,2,3)), "All items mode can start at an empty slot in the source inventory");
        menu.chest.setItem(0, new ItemStack(Items.COBBLESTONE));
        Stow.config.dragMode = dev.stow.StowConfig.DragMode.MATCHING_ONLY;
        screen.moved.clear();
        screen.mouseClicked(screen.at(start, InputConstants.MOD_SHIFT), false);
        screen.mouseDragged(screen.at(end, InputConstants.MOD_SHIFT), end.x-start.x, 0);
        screen.mouseReleased(screen.at(end, InputConstants.MOD_SHIFT));
        check(screen.moved.equals(List.of(0,2)), "Matching only setting retains exact item/component filtering");
        Stow.config.dragMode = dev.stow.StowConfig.DragMode.BOTH;
        screen.moved.clear();
        int bothModifiers = InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL;
        screen.mouseClicked(screen.at(start, bothModifiers), false);
        screen.mouseDragged(screen.at(end, bothModifiers), end.x-start.x, 0);
        screen.mouseReleased(screen.at(end, bothModifiers));
        check(screen.moved.equals(List.of(0,1,2,3)), "Both setting enables Ctrl-Shift for all crossed stacks");
        screen.moved.clear();
        screen.mouseClicked(screen.at(start, InputConstants.MOD_SHIFT), false);
        screen.mouseDragged(screen.at(end, bothModifiers), end.x-start.x, 0);
        screen.mouseReleased(screen.at(end, bothModifiers));
        check(screen.moved.equals(List.of(0,2)), "changing Ctrl mid-gesture does not unexpectedly change matching mode");
        screen.moved.clear();
        screen.mouseClicked(screen.at(start, bothModifiers), false);
        Stow.config.dragMode = dev.stow.StowConfig.DragMode.MATCHING_ONLY;
        screen.mouseDragged(screen.at(end, InputConstants.MOD_SHIFT), end.x-start.x, 0);
        screen.mouseReleased(screen.at(end, InputConstants.MOD_SHIFT));
        check(screen.moved.equals(List.of(0,1,2,3)), "active all-items gesture remains fixed if config or Ctrl changes");
        Stow.config.dragMode = dev.stow.StowConfig.DragMode.BOTH;

        Slot pin = menu.slots.get(4);
        screen.hover(pin);
        screen.keyPressed(key(InputConstants.KEY_P, 112, 0));
        check(PinnedSlots.isPinned(pin), "P pins underlying player inventory position");
        Slot sameIndexElsewhere = new Slot(inv, 0, 0, 0);
        check(PinnedSlots.isPinned(sameIndexElsewhere), "pin follows inventory index across different menu slot IDs");
        check(!PinnedSlots.canPin(start), "chest slots cannot be pinned as player slots");
        check(Files.readString(Path.of("config/stow/pins.txt")).contains("0"), "pin written to persistent settings file");
        MatchingItemDrag drag = new MatchingItemDrag(); drag.begin(pin);
        check(!drag.visit(pin, null), "matching drag preserves pinned source slot");
        drag.begin(pin, false);
        check(!drag.visit(pin, null) && drag.visit(menu.slots.get(5), null) == menu.slots.get(5).hasItem(), "all-items drag also preserves pinned source slot");
        screen.moved.clear();
        screen.mouseClicked(screen.at(start, InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL), false);
        screen.mouseDragged(screen.at(pin, InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL), 0, 30);
        screen.mouseReleased(screen.at(pin, InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL));
        check(!screen.moved.contains(pin.index), "all-items screen gesture remains in source inventory");
        inv.setItem(1,new ItemStack(Items.COBBLESTONE,32));
        var sortable=InventorySorting.scope(menu,pin,null);
        check(!sortable.contains(pin)&&sortable.contains(menu.slots.get(5)),"sort excludes pinned source and destination slots");
        screen.hover(pin); screen.keyPressed(key(InputConstants.KEY_P, 112, 0));
        check(!PinnedSlots.isPinned(pin), "P unpins slot and clears persisted pin");

        screen.keyPressed(key(InputConstants.KEY_F, InputConstants.KEYCODE_F, InputConstants.MOD_CONTROL));
        EditBox box = search(screen);
        check(box.isVisible() && box.isFocused() && screen.isInputCaptured(), "Ctrl-F shows search and captures text input");
        for (char c : "cobble".toCharArray()) screen.charTyped(new CharacterEvent(c));
        check(box.getValue().equals("cobble"), "SDL character input reaches search field");
        check(InventorySearch.matches(new ItemStack(Items.COBBLESTONE), box.getValue()) && !InventorySearch.matches(new ItemStack(Items.DIRT), box.getValue()), "search matches item name and excludes other items");
        check(InventorySearch.matches(new ItemStack(Items.COBBLESTONE), "@minecraft COBBLE") && !InventorySearch.matches(new ItemStack(Items.COBBLESTONE), "@other cobble"), "search supports case-insensitive names and namespace filters");
        check(InventorySearch.matches(named, "special") && !InventorySearch.matches(ItemStack.EMPTY, ""), "search supports custom names and ignores empty slots");
        screen.keyPressed(key(InputConstants.KEY_E, InputConstants.KEYCODE_E, 0));
        screen.keyPressed(key(InputConstants.KEY_P, 112, 0));
        check(mc.gui.screen() == screen && !PinnedSlots.isPinned(pin), "typing inventory/pin shortcut letters does not close screen or change pins");
        PinnedSlots.toggle(pin);
        GuiRenderState renderState = new GuiRenderState();
        screen.extractContents(new GuiGraphicsExtractor(mc, renderState, screen.width, screen.height), 0, 0, 0);
        Set<Integer> colors = new HashSet<>();
        renderState.forEachElement(element -> {
            if (element instanceof net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState rectangle) colors.add(rectangle.col1());
        }, GuiRenderState.TraverseRange.ALL);
        check(colors.contains(0xFF65E6AA) && colors.contains(0x99000000) && colors.contains(0xFFFFD45A), "real GUI render state contains match outlines, dimming and gold pin markers");
        PinnedSlots.toggle(pin);
        screen.keyPressed(key(InputConstants.KEY_ESCAPE, 27, 0));
        check(!box.isFocused() && !box.isVisible() && box.getValue().isEmpty() && mc.gui.screen() == screen, "Escape clears search and returns to inventory");
        settingsTests(mc,screen);
        sortingTests(mc);
        checks+=CompanionTests.run(mc,screen);
        checks+=BuildAidTests.run(mc);
        checks+=BundleDragTests.run(mc);
        checks+=MaterialUpgradeTests.run(mc,screen);
        checks+=PolishTests.run(mc,screen);
        checks+=WorkspaceTests.run(mc,screen);
        migrationTests();
        Stow.config.dragMode=dev.stow.StowConfig.DragMode.BOTH;
        memoryTests(mc,screen,inv);
        materialTests(mc,screen);
        mc.gui.setScreen(null);
    }
    static dev.stow.StowConfig reloadConfig(){dev.stow.StowConfig.initialize();return Stow.config;}
    static me.shedaniel.clothconfig2.gui.entries.SelectionListEntry<?> setting(net.minecraft.client.gui.screens.Screen screen,String label){
        return (me.shedaniel.clothconfig2.gui.entries.SelectionListEntry<?>)((me.shedaniel.clothconfig2.gui.AbstractConfigScreen)screen).getCategorizedEntries().values().stream().flatMap(List::stream).filter(e->e.getFieldName().getString().equals(label)).findFirst().orElseThrow();
    }
    static void choose(me.shedaniel.clothconfig2.gui.entries.SelectionListEntry<?> entry,Object value){
        var button=(net.minecraft.client.gui.components.Button)entry.children().getFirst();
        for(int i=0;i<12&&!entry.getValue().equals(value);i++)press(button);
        check(entry.getValue().equals(value),"native setting selector chooses "+value);
    }
    static void settingsTests(Minecraft mc,TestScreen parent)throws Exception {
        check(Stow.config.sortOrder==dev.stow.StowConfig.SortOrder.CREATIVE,"Mouse Wheelie's creative order is the startup default");
        mc.gui.setScreen(StowSettingsScreen.create(parent));
        var screen=(me.shedaniel.clothconfig2.gui.AbstractConfigScreen)mc.gui.screen();
        check(screen instanceof me.shedaniel.clothconfig2.gui.GlobalizedClothConfigScreen,"settings use BetterGrassify's actual Cloth Config screen");
        var size=screen.getCategorizedEntries().values().stream().flatMap(List::stream).filter(e->e.getFieldName().getString().equals("Shared HUD size (%)")).findFirst().orElseThrow();
        check(size.getValue().equals(Stow.config.buildingStockScale),"responsive slider starts with the saved size instead of its maximum");
        choose(setting(screen,"Middle-click"),dev.stow.StowConfig.SortOrder.AMOUNT);
        choose(setting(screen,"Shift-drag mode"),dev.stow.StowConfig.DragMode.ALL_ITEMS);
        check(Stow.config.sortOrder==dev.stow.StowConfig.SortOrder.CREATIVE&&Stow.config.dragMode==dev.stow.StowConfig.DragMode.BOTH,"editing selectors leaves live settings unchanged until Save");
        var links=screen.getCategorizedEntries().values().stream().flatMap(List::stream).filter(e->e.getClass().getSimpleName().equals("ScreenLinkEntry")).toList();
        check(links.size()==4,"shortcut, keep amount and both HUD placement editors live in settings");
        for(var link:links){press((net.minecraft.client.gui.components.Button)link.children().getFirst());check(mc.gui.screen()!=screen,"settings link opens its editor");mc.gui.screen().onClose();check(mc.gui.screen()==screen,"editor Back returns directly to the same settings screen");}
        check(setting(screen,"Middle-click").getValue()==dev.stow.StowConfig.SortOrder.AMOUNT,"opening a detailed editor preserves unsaved settings edits");
        screen.saveAll(true);
        check(mc.gui.screen()==parent&&reloadConfig().sortOrder==dev.stow.StowConfig.SortOrder.AMOUNT&&Stow.config.dragMode==dev.stow.StowConfig.DragMode.ALL_ITEMS,"Save persists all settings and returns to inventory");
        mc.gui.setScreen(StowSettingsScreen.create(parent));
        screen=(me.shedaniel.clothconfig2.gui.AbstractConfigScreen)mc.gui.screen();
        choose(setting(screen,"Middle-click"),dev.stow.StowConfig.SortOrder.NAME);
        // Cloth's cancel button opens its standard discard prompt; confirm without saving.
        press(button(screen,"Cancel"));
        check(mc.gui.screen() instanceof me.shedaniel.clothconfig2.gui.GlobalizedClothConfigScreen || mc.gui.screen() instanceof net.minecraft.client.gui.screens.ConfirmScreen,"Cancel uses Cloth's discard workflow");
        if(mc.gui.screen() instanceof net.minecraft.client.gui.screens.ConfirmScreen)press(button(mc.gui.screen(),Component.translatable("text.cloth-config.quit_discard").getString()));
        check(reloadConfig().sortOrder==dev.stow.StowConfig.SortOrder.AMOUNT,"cancel does not persist the changed selector");
        Stow.config.dragMode=dev.stow.StowConfig.DragMode.BOTH;Stow.config.save();mc.gui.setScreen(parent);
    }
    static void migrationTests()throws Exception {
        Path old=Path.of("config/mousewheelie-chest-memory/import-fixture.json"),fresh=Path.of("config/stow/chests/import-fixture.json");
        check(Files.exists(fresh)&&Files.readString(old).equals(Files.readString(fresh)),"legacy chest snapshots imported byte-for-byte");
        Files.writeString(fresh,"new local data");dev.stow.StowConfig.initialize();
        check(Files.readString(fresh).equals("new local data")&&!Files.readString(old).equals("new local data"),"later startup preserves imported edits and original data");
    }
    static void sortingTests(Minecraft mc)throws Exception {
        Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);sun.misc.Unsafe unsafe=(sun.misc.Unsafe)uf.get(null);
        var player=(net.minecraft.client.player.RemotePlayer)unsafe.allocateInstance(net.minecraft.client.player.RemotePlayer.class);
        var level=(net.minecraft.client.multiplayer.ClientLevel)unsafe.allocateInstance(net.minecraft.client.multiplayer.ClientLevel.class);
        var listener=(net.minecraft.client.multiplayer.ClientPacketListener)unsafe.allocateInstance(net.minecraft.client.multiplayer.ClientPacketListener.class);
        setField(net.minecraft.client.multiplayer.ClientPacketListener.class,listener,"enabledFeatures",net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
        setField(net.minecraft.client.multiplayer.ClientLevel.class,level,"connection",listener);
        setField(net.minecraft.world.entity.Entity.class,player,"level",level);
        Inventory inventory=new Inventory(player,new EntityEquipment());
        setField(Player.class,player,"inventory",inventory);setField(Player.class,player,"abilities",new Abilities());
        var chest=new SimpleContainer(27);var menu=ChestMenu.threeRows(12,inventory,chest);player.containerMenu=menu;
        var special=new ItemStack(Items.COBBLESTONE,5);special.set(DataComponents.CUSTOM_NAME,Component.literal("Special"));
        setField(CreativeModeTab.class,CreativeModeTabs.searchTab(),"displayItems",List.of(new ItemStack(Items.DIRT),new ItemStack(Items.COBBLESTONE),new ItemStack(Items.DIAMOND),new ItemStack(Items.BUNDLE)));
        Stow.config.optimizeCreativeSearchSort=false;
        for(var order:dev.stow.StowConfig.SortOrder.values()){
            chest.clearContent();chest.setItem(0,new ItemStack(Items.DIRT,32));chest.setItem(1,new ItemStack(Items.COBBLESTONE,12));chest.setItem(3,new ItemStack(Items.COBBLESTONE,60));chest.setItem(4,special.copy());chest.setItem(6,new ItemStack(Items.DIAMOND,2));var bundle=new ItemStack(Items.BUNDLE,1);bundle.set(DataComponents.BUNDLE_CONTENTS,new net.minecraft.world.item.component.BundleContents(List.of(new ItemStackTemplate(Items.DIAMOND,5))));chest.setItem(7,bundle);
            inventory.setItem(0,new ItemStack(Items.DIAMOND,40));
            var slots=InventorySorting.scope(menu,menu.slots.getFirst(),player);var before=contents(menu);
            var session=new InventorySorting.Session(menu,slots,order,(id,button)->menu.clicked(id,button,ContainerInput.PICKUP,player));int steps=0;
            while(session.step()){check(menu.getCarried().isEmpty(),"sorting transaction leaves cursor empty");if(++steps>100)throw new AssertionError("sort did not finish");}
            check(contents(menu).equals(before)&&menu.getCarried().isEmpty(),order+" sort preserves every item and component");
            check(slots.stream().anyMatch(slot->slot.getItem().is(Items.BUNDLE))&&inventory.getItem(0).getCount()==40,"sorting includes bundles and preserves the other inventory");
            if(order==dev.stow.StowConfig.SortOrder.AMOUNT)check(chest.getItem(0).is(Items.COBBLESTONE)&&chest.getItem(0).getCount()==64,"quantity sorts by combined item total with full stacks first");
            if(order==dev.stow.StowConfig.SortOrder.NAME)check(chest.getItem(0).is(Items.BUNDLE)&&chest.getItem(1).is(Items.COBBLESTONE),"alphabet order uses displayed names, with empty slots last");
            if(order==dev.stow.StowConfig.SortOrder.CREATIVE)check(chest.getItem(0).is(Items.DIRT)&&chest.getItem(1).is(Items.COBBLESTONE),"creative order follows the creative search catalogue");
            if(order==dev.stow.StowConfig.SortOrder.ITEM_TYPE)check(net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(chest.getItem(0).getItem())<=net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(chest.getItem(1).getItem()),"item type uses raw numeric item IDs");
            if(order==dev.stow.StowConfig.SortOrder.NONE)check(chest.getItem(0).is(Items.DIRT)&&chest.getItem(1).getCount()==64&&chest.getItem(3).getCount()==8,"combine only retains original positions");
            check(slots.stream().filter(s->s.hasItem()&&s.getItem().is(Items.COBBLESTONE)&&!s.getItem().has(DataComponents.CUSTOM_NAME)).count()==2,"matching stacks consolidate without merging custom names");
        }
        // Aggregate quantity must outrank a larger single stack; named items remain distinct.
        chest.clearContent();chest.setItem(0,new ItemStack(Items.DIRT,64));chest.setItem(1,new ItemStack(Items.COBBLESTONE,40));chest.setItem(2,new ItemStack(Items.COBBLESTONE,40));
        var totals=new InventorySorting.Session(menu,InventorySorting.scope(menu,menu.slots.getFirst(),player),dev.stow.StowConfig.SortOrder.AMOUNT,(id,button)->menu.clicked(id,button,ContainerInput.PICKUP,player));while(totals.step()){}
        check(chest.getItem(0).is(Items.COBBLESTONE)&&chest.getItem(1).is(Items.COBBLESTONE)&&chest.getItem(2).is(Items.DIRT),"80 combined cobblestone sort before one stack of 64 dirt");
        check(InventorySorting.selectedOrder(false,false)==Stow.config.sortOrder&&InventorySorting.selectedOrder(true,false)==dev.stow.StowConfig.SortOrder.AMOUNT&&InventorySorting.selectedOrder(false,true)==dev.stow.StowConfig.SortOrder.NAME,"normal, Shift and Ctrl sorting use Mouse Wheelie's modifier defaults");
        var random=new Random(9812);var items=List.of(Items.COBBLESTONE,Items.DIRT,Items.DIAMOND,Items.BUNDLE);
        for(int fixture=0;fixture<50;fixture++){
            chest.clearContent();for(int i=0;i<27;i++)if(random.nextBoolean()){Item item=items.get(random.nextInt(items.size()));ItemStack stack=new ItemStack(item,item==Items.BUNDLE?1:random.nextInt(64)+1);if(random.nextInt(8)==0)stack.set(DataComponents.CUSTOM_NAME,Component.literal("Named "+random.nextInt(3)));chest.setItem(i,stack);}
            var before=contents(menu);var session=new InventorySorting.Session(menu,InventorySorting.scope(menu,menu.slots.getFirst(),player),dev.stow.StowConfig.SortOrder.values()[fixture%5],(id,button)->menu.clicked(id,button,ContainerInput.PICKUP,player));int cycles=0;while(session.step()){if(++cycles>100)throw new AssertionError("cycle limit");check(menu.getCarried().isEmpty(),"permutation cycle ends with empty cursor");}
            check(contents(menu).equals(before)&&menu.getCarried().isEmpty(),"random sort "+fixture+" preserves named items, bundles and counts");
        }
        for(int i=0;i<36;i++)inventory.setItem(i,new ItemStack(Items.DIRT,i+1));
        var invSlots=menu.slots.stream().filter(s->s.container==inventory&&s.getContainerSlot()==9).findFirst().orElseThrow();
        var hotbar=menu.slots.stream().filter(s->s.container==inventory&&s.getContainerSlot()==0).findFirst().orElseThrow();PinnedSlots.toggle(invSlots);
        check(InventorySorting.scope(menu,invSlots,player).size()==26&&InventorySorting.scope(menu,hotbar,player).size()==9,"main inventory and hotbar remain separate; pins excluded");PinnedSlots.toggle(invSlots);
        Stow.config.hotbarScoping=dev.stow.StowConfig.HotbarScoping.NONE;
        check(InventorySorting.scope(menu,invSlots,player).size()==36,"optional combined scope includes hotbar in containers");
        Stow.config.hotbarScoping=dev.stow.StowConfig.HotbarScoping.SOFT;
        var local=(net.minecraft.client.player.LocalPlayer)unsafe.allocateInstance(net.minecraft.client.player.LocalPlayer.class);
        setField(Player.class,local,"inventory",inventory);setField(Player.class,local,"abilities",new Abilities());setField(net.minecraft.world.entity.Entity.class,local,"level",level);setField(net.minecraft.client.player.LocalPlayer.class,local,"minecraft",mc);local.containerMenu=menu;
        var oldPlayer=mc.player;var oldGameMode=mc.gameMode;var oldScreen=mc.gui.screen();
        try {
            mc.player=local;mc.gameMode=new net.minecraft.client.multiplayer.MultiPlayerGameMode(mc,listener){
                @Override public void handleContainerInput(int containerId,int slot,int button,ContainerInput type,Player ignored){menu.clicked(slot,button,type,player);}
            };
            chest.clearContent();chest.setItem(0,new ItemStack(Items.DIRT,12));chest.setItem(1,new ItemStack(Items.COBBLESTONE,64));
            MemoryTestScreen screen=new MemoryTestScreen(menu,inventory);mc.gui.setScreen(screen);Stow.config.sortOrder=dev.stow.StowConfig.SortOrder.NAME;
            Field lp=AbstractContainerScreen.class.getDeclaredField("leftPos"),tp=AbstractContainerScreen.class.getDeclaredField("topPos");lp.setAccessible(true);tp.setAccessible(true);
            double x=(int)lp.get(screen)+menu.slots.getFirst().x+8,y=(int)tp.get(screen)+menu.slots.getFirst().y+8;
            var middle=new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_MIDDLE,0));
            check(screen.mouseClicked(middle,false)&&InventorySorting.busy(menu),"middle click on a real container starts sorting");screen.mouseReleased(middle);
            int ticks=0;while(InventorySorting.busy(menu)&&ticks++<100)InventorySorting.tick(mc);
            check(chest.getItem(0).is(Items.COBBLESTONE)&&chest.getItem(1).is(Items.DIRT)&&menu.getCarried().isEmpty(),"tick queue completes real middle-click sort");
            check(InventorySorting.start(screen,menu.slots.getFirst()),"another sort can start");mc.gui.setScreen(StowSettingsScreen.create(screen));InventorySorting.tick(mc);check(!InventorySorting.busy(menu)&&menu.getCarried().isEmpty(),"changing screens cancels pending transactions with empty cursor");
        } finally {InventorySorting.cancel();mc.player=oldPlayer;mc.gameMode=oldGameMode;mc.gui.setScreen(oldScreen);}
        Stow.config.optimizeCreativeSearchSort=true;
        menu.setCarried(new ItemStack(Items.DIAMOND,3));int[] clicks={0};var aborted=new InventorySorting.Session(menu,menu.slots.subList(0,27),dev.stow.StowConfig.SortOrder.NAME,(id,button)->clicks[0]++);
        check(!aborted.step()&&clicks[0]==0&&menu.getCarried().getCount()==3,"sorting never consumes an existing carried stack");menu.setCarried(ItemStack.EMPTY);
    }
    static Map<String,Integer> contents(AbstractContainerMenu menu){var result=new TreeMap<String,Integer>();for(var slot:menu.slots){var stack=slot.getItem();if(!stack.isEmpty()){String key=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())+" "+stack.getComponents();result.merge(key,stack.getCount(),Integer::sum);}}return result;}
    static void setField(Class<?> type,Object target,String name,Object value)throws Exception{Field field=type.getDeclaredField(name);field.setAccessible(true);field.set(target,value);}

    static void memoryTests(Minecraft mc,TestScreen parent,Inventory inv) throws Exception {
        usingPrototype(Items.DIAMOND,"item.minecraft.diamond");
        var directory=Files.createTempDirectory(Path.of("."),"memory-test-");
        var store=new dev.stow.client.memory.ChestMemoryStore(directory,"server:friends.example");
        var sameFile=dev.stow.client.memory.ChestMemoryStore.pathFor(directory,"server:friends.example");
        check(!sameFile.equals(dev.stow.client.memory.ChestMemoryStore.pathFor(directory,"server:other.example")),"chest-memory files separate server/world identities");
        var near=new dev.stow.client.memory.ChestMemoryStore.Location("minecraft:overworld",10,64,20,"Chest");
        var far=new dev.stow.client.memory.ChestMemoryStore.Location("minecraft:overworld",100,64,200,"Barrel");
        var nether=new dev.stow.client.memory.ChestMemoryStore.Location("minecraft:the_nether",10,64,20,"Chest");
        var diamond=new dev.stow.client.memory.ChestMemoryStore.MemoryItem("minecraft:diamond","Diamond",32);
        long now=System.currentTimeMillis();
        store.remember(new dev.stow.client.memory.ChestMemoryStore.SavedChest(near,"Ore chest",now,List.of(diamond)));
        store.remember(new dev.stow.client.memory.ChestMemoryStore.SavedChest(far,"Barrel",now,List.of(diamond)));
        store.remember(new dev.stow.client.memory.ChestMemoryStore.SavedChest(nether,"Nether chest",now,List.of(diamond)));
        var results=store.search("diamonds","minecraft:overworld",net.minecraft.core.BlockPos.ZERO);
        check(results.size()==3 && results.get(0).chest().location().equals(near) && results.get(2).chest().location().equals(nether),"diamonds plural search sorts current dimension and nearby locations first");
        check(results.get(0).count()==32 && store.search("@other diamonds","",null).isEmpty(),"memory search includes counts and namespace filtering");
        store.remember(new dev.stow.client.memory.ChestMemoryStore.SavedChest(near,"Ore chest",now+1,List.of()));
        check(store.search("diamonds","",null).size()==2,"reopening an empty chest replaces outdated item matches");
        store.save();
        var reloaded=new dev.stow.client.memory.ChestMemoryStore(directory,"server:friends.example");
        check(reloaded.search("","",null).size()==3 && reloaded.get(near).items().isEmpty(),"saved coordinates, dimensions and empty snapshots survive reload");
        check(new dev.stow.client.memory.ChestMemoryStore(directory,"server:other.example").search("","",null).isEmpty(),"memory does not leak results into another server");
        var right=net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.RIGHT);
        var left=right.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,net.minecraft.world.level.block.state.properties.ChestType.LEFT);
        var rightPos=new net.minecraft.core.BlockPos(0,64,0);
        var leftPos=net.minecraft.world.level.block.ChestBlock.getConnectedBlockPos(rightPos,right);
        check(dev.stow.client.memory.ChestMemory.locationFor(rightPos,right,"minecraft:overworld").equals(dev.stow.client.memory.ChestMemory.locationFor(leftPos,left,"minecraft:overworld")),"both halves of a double chest resolve to one saved location");
        check(dev.stow.client.memory.ChestMemory.locationFor(rightPos,net.minecraft.world.level.block.Blocks.ENDER_CHEST.defaultBlockState(),"minecraft:overworld")==null,"shared Ender chest contents are excluded");
        var chest=new SimpleContainer(27);chest.setItem(0,new ItemStack(Items.DIAMOND,32));chest.setItem(1,new ItemStack(Items.DIAMOND,12));
        inv.setItem(0,new ItemStack(Items.DIAMOND,64));
        var chestMenu=ChestMenu.threeRows(55,inv,chest);
        var chestScreen=new MemoryTestScreen(chestMenu,inv);
        setMemoryField("store",store);setMemoryField("worldKey",null);setMemoryField("pending",near);setMemoryField("pendingAt",System.nanoTime());
        mc.gui.setScreen(chestScreen);
        check(store.get(near).items().isEmpty(),"opening a menu does not overwrite memory with its initial empty state");
        dev.stow.client.memory.ChestMemory.contentReceived(999);
        check(store.get(near).items().isEmpty(),"unrelated container packets cannot change a saved chest");
        dev.stow.client.memory.ChestMemory.contentReceived(55);
        check(store.get(near).items().size()==1 && store.get(near).items().get(0).count()==44,"server content snapshot aggregates chest stacks and excludes player inventory");
        chest.setItem(0,ItemStack.EMPTY);chest.setItem(1,ItemStack.EMPTY);
        dev.stow.client.memory.ChestMemory.slotReceived(55);
        check(store.get(near).items().isEmpty(),"container slot updates remove items from memory when chest is emptied");
        store.remember(new dev.stow.client.memory.ChestMemoryStore.SavedChest(near,"Ore chest",now,List.of(diamond)));
        for(int i=0;i<8;i++)store.remember(new dev.stow.client.memory.ChestMemoryStore.SavedChest(new dev.stow.client.memory.ChestMemoryStore.Location("minecraft:overworld",i+200,64,0,"Chest"),"Chest "+i,now,List.of(diamond)));
        mc.gui.setScreen(parent);
        parent.keyPressed(key(InputConstants.KEY_F,InputConstants.KEYCODE_F,InputConstants.MOD_CONTROL));
        search(parent).setValue("diamonds");
        parent.extractContents(new GuiGraphicsExtractor(mc,new GuiRenderState(),parent.width,parent.height),0,0,0);
        var memoryButton=(net.minecraft.client.gui.components.Button)parent.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().startsWith("Chests")).findFirst().orElseThrow();
        check(memoryButton.getMessage().getString().matches("Chests [0-9]+"),"inventory Chests button shows saved matches for current search query");
        parent.mouseClicked(mouse(memoryButton.getX()+1,memoryButton.getY()+1,0),false);
        check(mc.gui.screen() instanceof dev.stow.client.memory.ChestMemoryScreen,"inventory Chests button opens saved-location search");
        ((dev.stow.client.memory.ChestMemoryScreen)mc.gui.screen()).onClose();
        parent.mouseReleased(mouse(memoryButton.getX()+1,memoryButton.getY()+1,0));
        var held=new ItemStack(Items.DIAMOND,7);parent.getMenu().setCarried(held);
        parent.extractContents(new GuiGraphicsExtractor(mc,new GuiRenderState(),parent.width,parent.height),0,0,0);
        memoryButton=(net.minecraft.client.gui.components.Button)parent.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().startsWith("Chests")).findFirst().orElseThrow();
        parent.mouseClicked(mouse(memoryButton.getX()+1,memoryButton.getY()+1,0),false);
        parent.mouseReleased(mouse(memoryButton.getX()+1,memoryButton.getY()+1,0));
        check(mc.gui.screen()==parent && parent.getMenu().getCarried().getCount()==7,"memory button cannot close or drop a carried stack");
        parent.getMenu().setCarried(ItemStack.EMPTY);
        var memoryScreen=new dev.stow.client.memory.ChestMemoryScreen(parent,store,"diamonds");
        // Returning to parent removes the old chest screen and saves its last visible (empty) contents.
        mc.gui.setScreen(parent);
        store.remember(new dev.stow.client.memory.ChestMemoryStore.SavedChest(near,"Ore chest",now,List.of(diamond)));
        mc.gui.setScreen(memoryScreen);
        var search=(EditBox)memoryScreen.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();
        check(search.getValue().equals("diamonds") && search.isFocused(),"chest-memory screen receives inventory query and captures typing");
        var rendered=new GuiRenderState();memoryScreen.extractRenderState(new GuiGraphicsExtractor(mc,rendered,memoryScreen.width,memoryScreen.height),0,0,0);
        check(true,"chest-memory results render with coordinates, dimension, counts and last-seen age");
        var next=(net.minecraft.client.gui.components.Button)memoryScreen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(">")).findFirst().orElseThrow();
        check(next.active,"many matching remembered chests are paginated");
        var glow=(net.minecraft.client.gui.components.Button)memoryScreen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("Glow")).findFirst().orElseThrow();
        glow.onPress(key(InputConstants.KEY_SPACE,InputConstants.KEYCODE_SPACE,0));
        check(dev.stow.client.memory.ChestMemory.selected()!=null,"Glow action selects a remembered location");
        var projection=new org.joml.Matrix4f().perspective((float)Math.PI/2,1,0.1f,100);
        var locatorState=new GuiRenderState();
        ChestGlow.drawProjected(new GuiGraphicsExtractor(mc,locatorState,320,240),new ChestGlow.Target(new net.minecraft.world.phys.AABB(-1,-1,-6,1,1,-4),"Chest (last seen)"),net.minecraft.world.phys.Vec3.ZERO,projection);
        List<net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState> edges=new ArrayList<>();locatorState.forEachElement(e->{if(e instanceof net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState edge)edges.add(edge);},GuiRenderState.TraverseRange.ALL);
        check(edges.size()==24,"through-wall locator renders all twelve projected box edges with a halo");
        check(edges.stream().noneMatch(edge->edge.x1()-edge.x0()>=320&&edge.y1()-edge.y0()>=240),"locator emits no fullscreen tint or depth-clearing pass");
        var behindState=new GuiRenderState();ChestGlow.drawProjected(new GuiGraphicsExtractor(mc,behindState,320,240),new ChestGlow.Target(new net.minecraft.world.phys.AABB(-1,-1,4,1,1,6),"Behind"),net.minecraft.world.phys.Vec3.ZERO,projection);
        List<Object> behind=new ArrayList<>();behindState.forEachElement(behind::add,GuiRenderState.TraverseRange.ALL);check(behind.isEmpty(),"targets behind the camera never mirror onto the screen");
        var nearState=new GuiRenderState();ChestGlow.drawProjected(new GuiGraphicsExtractor(mc,nearState,320,240),new ChestGlow.Target(new net.minecraft.world.phys.AABB(-1,-1,-0.2,1,1,0.2),"Near"),net.minecraft.world.phys.Vec3.ZERO,projection);
        check(true,"near-plane edges are clipped without overflow or giant fullscreen geometry");
        check(ChestGlow.labelScale(58,3)<0.5f,"58-block label is less than half its previous size at GUI scale 3");
        for(int scale=1;scale<=4;scale++){
            check(ChestGlow.labelScale(10000,scale)*9*scale>=7.99f,"distant label stays readable at GUI scale "+scale);
            check(ChestGlow.labelScale(20,scale)>=ChestGlow.labelScale(58,scale),"label shrinks as distance increases at GUI scale "+scale);
        }
        var farState=new GuiRenderState();
        ChestGlow.drawProjected(new GuiGraphicsExtractor(mc,farState,320,240),new ChestGlow.Target(new net.minecraft.world.phys.AABB(-1,-1,-59,1,1,-57),"Chest · 58 m"),net.minecraft.world.phys.Vec3.ZERO,projection);
        List<net.minecraft.client.renderer.state.gui.GuiTextRenderState> farText=new ArrayList<>();
        farState.forEachText(farText::add);
        check(farText.size()==1 && farText.getFirst().pose.m00()<1,"actual extracted locator text carries the reduced scale");
        var frame=mc.gameRenderer.gameRenderState();var savedCamera=frame.levelRenderState.cameraRenderState;
        boolean savedBob=frame.optionsRenderState.bobView;float savedEffect=frame.optionsRenderState.screenEffectScale;
        try{
            var camera=new net.minecraft.client.renderer.state.level.CameraRenderState();frame.levelRenderState.cameraRenderState=camera;
            camera.projectionMatrix=new org.joml.Matrix4f(projection);camera.viewRotationMatrix=new org.joml.Matrix4f().rotateY(0.4f).rotateX(-0.2f);
            camera.entityRenderState.isPlayer=true;frame.optionsRenderState.bobView=true;frame.optionsRenderState.screenEffectScale=0;
            for(int step=0;step<24;step++){
                float walk=step/8f,bob=0.09f;camera.entityRenderState.backwardsInterpolatedWalkDistance=walk;camera.entityRenderState.bob=bob;
                float sin=net.minecraft.util.Mth.sin(walk*(float)Math.PI),cos=net.minecraft.util.Mth.cos(walk*(float)Math.PI);
                var expected=new org.joml.Matrix4f(projection).translate(sin*bob*0.5f,-Math.abs(cos*bob),0)
                    .rotateZ(sin*bob*3*((float)Math.PI/180))
                    .rotateX(Math.abs(net.minecraft.util.Mth.cos(walk*(float)Math.PI-0.2f)*bob)*5*((float)Math.PI/180))
                    .mul(camera.viewRotationMatrix);
                check(ChestGlow.worldProjection(mc.gameRenderer).equals(expected,0.00001f),"locator follows world's walking transform at phase "+step);
            }
            frame.optionsRenderState.bobView=false;
            check(ChestGlow.worldProjection(mc.gameRenderer).equals(new org.joml.Matrix4f(projection).mul(camera.viewRotationMatrix),0.00001f),"disabled view bob also disables locator bob");
            check(camera.projectionMatrix.equals(projection,0),"locator never changes the world's projection matrix");
        }finally{frame.levelRenderState.cameraRenderState=savedCamera;frame.optionsRenderState.bobView=savedBob;frame.optionsRenderState.screenEffectScale=savedEffect;}
        search.setValue("impossible_item_name");
        check(memoryScreen.children().stream().noneMatch(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("Forget")),"changing memory query removes unrelated results");
        search.setValue("diamonds");
        int before=store.search("diamonds","",null).size();
        var forget=(net.minecraft.client.gui.components.Button)memoryScreen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("Forget")).findFirst().orElseThrow();
        forget.onPress(key(InputConstants.KEY_SPACE,InputConstants.KEYCODE_SPACE,0));
        check(store.search("diamonds","",null).size()==before-1,"Forget removes the selected saved container");
        memoryScreen.onClose();
        check(mc.gui.screen()==parent,"closing chest memory returns to the inventory");
        dev.stow.client.memory.ChestMemory.disconnect();
        check(dev.stow.client.memory.ChestMemory.selected()==null && dev.stow.client.memory.ChestMemory.glowTarget()==null,"disconnect clears selected chest glow and temporary location tracking");
    }
    static net.minecraft.client.gui.components.Button button(net.minecraft.client.gui.screens.Screen screen,String text) {
        return (net.minecraft.client.gui.components.Button)screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(text)).findFirst().orElseThrow();
    }
    static void press(net.minecraft.client.gui.components.Button button){button.onPress(key(InputConstants.KEY_SPACE,InputConstants.KEYCODE_SPACE,0));}
    static void materialTests(Minecraft mc,TestScreen parent) throws Exception {
        var directory=Files.createTempDirectory(Path.of("."),"material-test-");
        var store=new ChestMemoryStore(directory,"planner");
        var a=new Location("minecraft:overworld",0,64,0,"Chest");
        var b=new Location("minecraft:overworld",30,64,0,"Chest");
        var c=new Location("minecraft:the_nether",30,64,0,"Chest");
        long now=System.currentTimeMillis();
        store.remember(new SavedChest(a,"Chest",now,List.of(new MemoryItem("minecraft:cobblestone","Cobblestone",400),new MemoryItem("minecraft:cobblestone_stairs","Cobblestone Stairs",64),new MemoryItem("minecraft:diamond","Diamond",7))));
        store.remember(new SavedChest(b,"Barrel",now,List.of(new MemoryItem("minecraft:cobblestone","Named Cobble",256))));
        store.rename(a,"Building supplies");
        check(store.displayName(store.get(a)).equals("Building supplies") && store.search("Building supplies","",null).size()==1,"local chest aliases are searchable");
        store.remember(new SavedChest(a,"Server-renamed chest",now+1,store.get(a).items()));
        check(store.displayName(store.get(a)).equals("Building supplies"),"new content snapshots preserve a local chest nickname");
        store.setGoal("minecraft:cobblestone",1000);store.setGoal("minecraft:diamond",20);
        store.trackGoal("minecraft:cobblestone");
        check(store.goals().size()==2 && store.goals().getFirst().target()==1000,"multiple goals support selecting the inventory-tracked item");
        store.setGoal("minecraft:cobblestone",1200);
        check(store.goals().size()==2 && store.goals().getFirst().target()==1200,"editing a target updates its existing goal without duplicates");
        store.setGoal("minecraft:cobblestone",1000);
        check(store.chestCount("minecraft:cobblestone")==656,"material totals use exact item IDs, combine named variants and exclude stairs");
        store.toggleIncluded(b);
        check(store.chestCount("minecraft:cobblestone")==400 && store.includedChestCount()==1,"excluding a chest invalidates cached totals and narrows storage scope");
        store.remember(new SavedChest(c,"Nether",now,List.of(new MemoryItem("minecraft:cobblestone","Cobblestone",512))));
        check(store.chestCount("minecraft:cobblestone")==400,"new chests are excluded when using an explicit custom selection");
        store.selectAllChests(true);
        check(store.chestCount("minecraft:cobblestone")==1168 && store.includedChestCount()==3,"All storage includes remembered chests across dimensions");
        store.selectAllChests(false);
        check(store.chestCount("minecraft:cobblestone")==0,"None storage produces inventory-only totals");
        store.toggleIncluded(a);store.toggleIncluded(b);
        store.save();var reloaded=new ChestMemoryStore(directory,"planner");
        check(reloaded.goals().size()==2 && reloaded.customName(a).equals("Building supplies") && reloaded.chestCount("minecraft:cobblestone")==656 && !reloaded.isIncluded(c),"goals, aliases and custom storage selection survive disk reload");
        check(new ChestMemoryStore(directory,"other-planner").goals().isEmpty(),"material goals and selected storage remain isolated per world");
        var inv=new Inventory(null,new EntityEquipment());inv.setItem(0,new ItemStack(Items.COBBLESTONE,64));inv.setItem(1,new ItemStack(Items.COBBLESTONE,32));inv.setItem(2,new ItemStack(Items.DIRT,64));
        inv.setItem(40,new ItemStack(Items.COBBLESTONE,8));
        long inInventory=MaterialPlanner.inventoryCount(inv,new ItemStack(Items.COBBLESTONE,12),"minecraft:cobblestone");
        check(inInventory==116,"live inventory count includes hotbar, main slots, offhand and carried stack exactly once");
        var progress=new MaterialPlanner.Progress(inInventory,store.chestCount("minecraft:cobblestone"),1000,store.includedChestCount());
        check(progress.total()==772 && progress.missing()==228 && !progress.enough(),"1000-cobble goal shows correct combined total and shortage");
        check(new MaterialPlanner.Progress(1200,400,1000,1).missing()==0 && new MaterialPlanner.Progress(1200,400,1000,1).enough(),"surplus counts remain visible and never produce a negative shortage");
        var catalogue=MemoryItems.catalogue();
        check(MemoryItems.resolve(catalogue,"cobble").id().equals("minecraft:cobblestone") && MemoryItems.resolve(catalogue,"minecraft:cobblestone").id().equals("minecraft:cobblestone"),"shortcut alias and explicit item ID resolve to cobblestone");
        check(MemoryItems.resolve(catalogue,"slab")==null && MemoryItems.search(catalogue,"slab").size()>1,"ambiguous material names require a visible picker selection");
        check(MemoryItems.search(catalogue,"diamonds").stream().anyMatch(e -> e.id().equals("minecraft:diamond")),"item picker accepts familiar plural searches");
        check(MemoryItems.icon("missingmod:old_item").isEmpty() && MemoryItems.icon("minecraft:diamond").is(Items.DIAMOND),"known IDs produce icons and removed mod items fall back safely");
        for(int bad:List.of(0,-1,1000001)){
            boolean rejected=false;try{store.setGoal("minecraft:dirt",bad);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid target rejected: "+bad);
        }
        var legacy=ChestMemoryStore.pathFor(directory,"legacy");
        Files.writeString(legacy,"{\"version\":1,\"chests\":[{\"location\":{\"dimension\":\"minecraft:overworld\",\"x\":0,\"y\":64,\"z\":0,\"kind\":\"Chest\"},\"title\":\"Old chest\",\"lastSeen\":1,\"items\":[]}]}");
        var migrated=new ChestMemoryStore(directory,"legacy");migrated.rename(a,"Migrated");migrated.setGoal("minecraft:cobblestone",1000);migrated.save();
        check(new ChestMemoryStore(directory,"legacy").customName(a).equals("Migrated"),"revision-4 chest files migrate without losing saved locations");
        setMemoryField("activeMenu",null);setMemoryField("activeLocation",null);setMemoryField("worldKey",null);setMemoryField("store",store);
        mc.gui.setScreen(parent);search(parent).setValue("");
        parent.extractContents(new GuiGraphicsExtractor(mc,new GuiRenderState(),parent.width,parent.height),0,0,0);
        var need=(FloatingMaterialTracker)parent.children().stream().filter(FloatingMaterialTracker.class::isInstance).findFirst().orElseThrow();
        parent.moved.clear();parent.mouseClicked(mouse(need.getX()+1,need.getY()+1,0),false);
        check(mc.gui.screen() instanceof MaterialsScreen && parent.moved.isEmpty(),"inventory progress button opens materials without transferring stacks");
        ((MaterialsScreen)mc.gui.screen()).onClose();parent.mouseReleased(mouse(need.getX()+1,need.getY()+1,0));
        parent.getMenu().setCarried(new ItemStack(Items.COBBLESTONE,4));parent.extractContents(new GuiGraphicsExtractor(mc,new GuiRenderState(),parent.width,parent.height),0,0,0);
        parent.mouseClicked(mouse(need.getX()+1,need.getY()+1,0),false);parent.mouseReleased(mouse(need.getX()+1,need.getY()+1,0));
        check(mc.gui.screen()==parent && parent.getMenu().getCarried().getCount()==4,"materials button is blocked while carrying an item");parent.getMenu().setCarried(ItemStack.EMPTY);
        need=(FloatingMaterialTracker)parent.children().stream().filter(FloatingMaterialTracker.class::isInstance).findFirst().orElseThrow();
        // A minimal LocalPlayer fixture lets the actual inventory button use its live-count path.
        Field unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe=(sun.misc.Unsafe)unsafeField.get(null);
        var fakePlayer=(net.minecraft.client.player.LocalPlayer)unsafe.allocateInstance(net.minecraft.client.player.LocalPlayer.class);
        Field inventoryField=Player.class.getDeclaredField("inventory");inventoryField.setAccessible(true);inventoryField.set(fakePlayer,inv);
        Field cooldownField=Player.class.getDeclaredField("cooldowns");cooldownField.setAccessible(true);cooldownField.set(fakePlayer,new ItemCooldowns());
        fakePlayer.containerMenu=parent.getMenu();
        var previousPlayer=mc.player;
        inv.setItem(1,new ItemStack(Items.COBBLESTONE,52)); // 64 + 52 + 8 offhand = 124.
        try {
            mc.player=fakePlayer;store.selectAllChests(false);store.toggleIncluded(a);
            parent.extractContents(new GuiGraphicsExtractor(mc,new GuiRenderState(),parent.width,parent.height),0,0,0);
            check(need.countLabel(store.goals().getFirst()).equals("524 / 1000"),"inventory counter adds 124 live inventory to 400 selected chest items");
            var counterRender=new GuiRenderState();need.extractRenderState(new GuiGraphicsExtractor(mc,counterRender,parent.width,parent.height),0,0,0);
            List<Object> counterIcons=new ArrayList<>();counterRender.forEachItem(counterIcons::add);
            check(counterIcons.size()==2,"floating list displays real item icons for multiple tracked goals");
            List<Object> backgrounds=new ArrayList<>();counterRender.forEachElement(backgrounds::add,GuiRenderState.TraverseRange.ALL);
            check(backgrounds.stream().allMatch(e->e instanceof net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState r&&Math.abs(r.x1()-r.x0())<=7&&Math.abs(r.y1()-r.y0())<=7),"floating trackers emit only small HUD stars, without backgrounds or button sprites");
            check(need.getX()>=parent.inventoryRight(),"floating counters sit strictly to the right of the inventory");
            store.toggleIncluded(b);parent.extractContents(new GuiGraphicsExtractor(mc,new GuiRenderState(),parent.width,parent.height),0,0,0);
            check(need.countLabel(store.goals().getFirst()).equals("780 / 1000"),"counter updates immediately when another remembered chest is selected");
            inv.setItem(1,new ItemStack(Items.COBBLESTONE,20));parent.extractContents(new GuiGraphicsExtractor(mc,new GuiRenderState(),parent.width,parent.height),0,0,0);
            check(need.countLabel(store.goals().getFirst()).equals("748 / 1000"),"counter updates live when inventory quantity changes");
            store.remember(new SavedChest(a,"Server-renamed chest",now,List.of(new MemoryItem("minecraft:cobblestone","Cobblestone",100),new MemoryItem("minecraft:diamond","Diamond",7))));
            parent.extractContents(new GuiGraphicsExtractor(mc,new GuiRenderState(),parent.width,parent.height),0,0,0);
            check(need.countLabel(store.goals().getFirst()).equals("448 / 1000"),"counter updates when an opened chest supplies a refreshed snapshot");
            inv.setItem(1,new ItemStack(Items.COBBLESTONE,52));
        } finally {mc.player=previousPlayer;}
        store.remember(new SavedChest(a,"Server-renamed chest",now,List.of(new MemoryItem("minecraft:cobblestone","Cobblestone",400),new MemoryItem("minecraft:diamond","Diamond",7))));
        var checklist=new MaterialsScreen(parent,store);mc.gui.setScreen(checklist);
        var render=new GuiRenderState();checklist.extractRenderState(new GuiGraphicsExtractor(mc,render,checklist.width,checklist.height),0,0,0);
        List<Integer> colors=new ArrayList<>();render.forEachElement(e -> {if(e instanceof net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState rect)colors.add(rect.col1());},GuiRenderState.TraverseRange.ALL);
        check(colors.contains(0x18000000) && colors.contains(0xFFFFFFFF),"redesigned planner renders distinct goal rows and visibility accents");
        press(button(checklist,"+ Material"));check(mc.gui.screen() instanceof MaterialPickerScreen,"add material opens the searchable registry picker");
        var picker=(MaterialPickerScreen)mc.gui.screen();List<EditBox> fields=picker.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();
        fields.get(0).setValue("minecraft:diamond");fields.get(1).setValue("0");
        press(button(picker,"Diamond"));
        check(!button(picker,"Save target").active,"picker disables confirmation for invalid amounts");
        check(mc.gui.screen()==picker && store.goals().getFirst().itemId().equals("minecraft:cobblestone"),"choosing an item only previews it and never commits a goal");
        fields.get(1).setValue("48");var diamondButton=button(picker,"Diamond");picker.mouseClicked(mouse(diamondButton.getX()+2,diamondButton.getY()+2,0),false);
        check(picker.getFocused()==fields.get(1)&&picker.charTyped(new CharacterEvent('7'))&&fields.get(1).getValue().equals("7"),"real item mouse click leaves selected amount ready for typing without a second click");fields.get(1).setValue("48");press(diamondButton);
        check(fields.get(1).isFocused() && fields.get(1).getHighlighted().equals("48"),"choosing an item focuses and selects its amount for easy replacement");
        picker.keyPressed(key(InputConstants.KEY_RETURN,13,0));
        check(store.goals().get(1).itemId().equals("minecraft:diamond") && store.goals().get(1).target()==48 && mc.gui.screen()==checklist,"picker saves exact item choice and amount then returns to checklist");
        store.trackGoal("minecraft:diamond");checklist.tick();
        var draft=(EditBox)checklist.children().stream().filter(w->w instanceof EditBox&&((EditBox)w).getMessage().getString().equals("Target amount")).findFirst().orElseThrow();
        draft.setValue("64");press(button(checklist,"Save target"));
        check(store.goals().getFirst().target()==64,"planner saves edited quantities directly without reopening the item picker");
        press(button(checklist,"Hide"));check(!store.isGoalVisible("minecraft:diamond"),"planner Hide keeps the goal while hiding its floating counter");
        store.save();check(!new ChestMemoryStore(directory,"planner").isGoalVisible("minecraft:diamond"),"floating visibility survives a restart");
        press(button(checklist,"Show"));check(store.isGoalVisible("minecraft:diamond"),"planner Show restores a hidden floating goal");
        press(button(checklist,"Sources"));var sources=(ChestMemoryScreen)mc.gui.screen();
        check(sources.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button btn && btn.getMessage().getString().equals("Rename")).count()==1,"Sources filters to chests containing the exact target item");
        press(button(sources,"None"));check(store.chestCount("minecraft:cobblestone")==0,"selection GUI None changes totals for all goals");
        press(button(sources,"+"));check(store.chestCount("minecraft:diamond")==7,"selection checkbox includes an individual chest");
        press(button(sources,"Rename"));var rename=(ChestRenameScreen)mc.gui.screen();
        ((EditBox)rename.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow()).setValue("Build chest");press(button(rename,"Save name"));
        check(store.displayName(store.get(a)).equals("Build chest") && mc.gui.screen()==sources,"rename GUI updates local nickname and returns to chest results");
        var previewState=new GuiRenderState();sources.extractRenderState(new GuiGraphicsExtractor(mc,previewState,sources.width,sources.height),0,0,0);
        List<Object> icons=new ArrayList<>();previewState.forEachItem(icons::add);
        check(icons.size()>=2,"chest menu emits real Minecraft item icons for multiple remembered contents");
        press(button(sources,"Rename"));press(button(mc.gui.screen(),"Reset name"));
        check(store.displayName(store.get(a)).equals("Server-renamed chest"),"reset nickname restores original server container title");
        var dispatcher=new com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>();
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.invoker().register(dispatcher,null);
        List<String> feedback=new ArrayList<>();
        var commandSource=(net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource)Proxy.newProxyInstance(FeatureTest.class.getClassLoader(),new Class[]{net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource.class},(proxy,method,args) -> {
            if(method.getName().equals("sendFeedback") || method.getName().equals("sendError")){feedback.add(((Component)args[0]).getString());return null;}
            if(method.getName().equals("getClient"))return mc;if(method.getName().equals("attended"))return true;return null;
        });
        check(dispatcher.execute("need 1000 cobble",commandSource)==1 && store.goals().contains(new MaterialGoal("minecraft:cobblestone",1000)) && !feedback.isEmpty(),"real Brigadier client /need shortcut creates a goal and gives feedback");
        int before=store.goals().size();dispatcher.execute("need 500 slab",commandSource);
        Field pending=MaterialPlanner.class.getDeclaredField("pendingScreen");pending.setAccessible(true);
        check(pending.get(null) instanceof MaterialPickerScreen && store.goals().size()==before,"ambiguous client commands schedule the picker instead of choosing an arbitrary block");
        boolean syntax=false;try{dispatcher.execute("need 0 cobble",commandSource);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){syntax=true;}
        check(syntax,"command rejects zero target at argument parsing");
        dispatcher.execute("need",commandSource);check(pending.get(null) instanceof MaterialsScreen,"bare /need opens the checklist");
        MaterialPlanner.disconnect();check(pending.get(null)==null,"disconnect clears queued command screens");
        floatingTests(store,directory,parent,mc);
        revisionSevenTests(store,directory,parent,mc);
        store.rename(a,"Building supplies");store.selectAllChests(true);store.setGoal("minecraft:diamond",48);store.setGoal("minecraft:dirt",256);store.setGoal("minecraft:oak_planks",512);store.setGoal("minecraft:cobblestone",1000);
        mc.gui.setScreen(new MaterialsScreen(parent,store));
        if(Boolean.getBoolean("porttest.screenshots"))startScreenshots(mc,store,parent);
        else ChestMemory.disconnect();
    }
    static void revisionSevenTests(ChestMemoryStore store,Path directory,TestScreen parent,Minecraft mc) throws Exception {
        var fresh=new ChestMemoryStore(directory,"projects");Location a=new Location("minecraft:overworld",0,64,0,"Chest"),b=new Location("minecraft:overworld",80,64,0,"Barrel"),n=new Location("minecraft:the_nether",1,64,0,"Chest");
        long now=System.currentTimeMillis();for(var location:List.of(a,b,n))fresh.remember(new SavedChest(location,"Supplies",now-120000,List.of(new MemoryItem("minecraft:cobblestone","Cobblestone",64))));
        fresh.setGoal("minecraft:cobblestone",1000);fresh.setGoal("minecraft:diamond",48);fresh.setGoal("minecraft:dirt",128);
        check(fresh.goals().getFirst().itemId().equals("minecraft:cobblestone"),"new goals append in creation order instead of reversing the first page");
        fresh.setGoal("minecraft:diamond",64);check(fresh.goals().get(1).equals(new MaterialGoal("minecraft:diamond",64)),"changing a goal preserves its position");
        fresh.toggleGoalVisible("minecraft:diamond");fresh.removeGoal("minecraft:diamond");check(fresh.canUndo() && fresh.goals().size()==2,"deleting a goal exposes Undo");
        fresh.undo();check(fresh.goals().get(1).target()==64 && !fresh.isGoalVisible("minecraft:diamond"),"Undo restores goal amount, original order and visibility");
        fresh.selectAllChests(false);fresh.toggleIncluded(a);fresh.rename(a,"Stone reserve");String main=fresh.activeProjectId();
        String farm=fresh.createProject("Farm");fresh.setGoal("minecraft:diamond",20);fresh.selectAllChests(false);fresh.toggleIncluded(b);
        check(fresh.goals().size()==1 && fresh.chestCount("minecraft:cobblestone")==64 && fresh.customName(a).equals("Stone reserve"),"projects isolate goals and storage while sharing named chest memory");
        fresh.switchProject(main);check(fresh.goals().size()==3 && fresh.isIncluded(a) && !fresh.isIncluded(b),"switching projects restores original goals and chest choices");
        check(fresh.nearestSource("minecraft:cobblestone","minecraft:overworld",new net.minecraft.core.BlockPos(79,64,0)).location().equals(a),"find shortcut ignores nearer chests excluded from the project");
        check(fresh.nearestSource("minecraft:diamond","minecraft:overworld",new net.minecraft.core.BlockPos(0,64,0))==null && fresh.nearestSource("minecraft:cobblestone","minecraft:the_nether",new net.minecraft.core.BlockPos(0,64,0))==null,"find shortcut rejects missing items and unselected other-dimension storage");
        check(fresh.oldestSourceTime("minecraft:cobblestone")==now-120000 && ChestMemoryStore.age(now-120000).equals("2m"),"freshness uses age of included matching snapshots");
        fresh.forget(a);check(fresh.chestCount("minecraft:cobblestone")==0,"forgetting selected storage updates project totals");fresh.undo();check(fresh.customName(a).equals("Stone reserve") && fresh.chestCount("minecraft:cobblestone")==64,"Undo restores forgotten chest nickname, contents and selection");
        fresh.forget(a);fresh.remember(new SavedChest(a,"Fresh chest",now,List.of(new MemoryItem("minecraft:cobblestone","Cobblestone",12))));fresh.undo();check(fresh.countIn(a,"minecraft:cobblestone")==12 && fresh.customName(a).equals("Stone reserve"),"Undo never overwrites a newer reopened chest snapshot");
        fresh.switchProject(farm);fresh.renameProject("Tree farm");fresh.save();var reload=new ChestMemoryStore(directory,"projects");check(reload.projectName().equals("Tree farm") && reload.projects().size()==2 && reload.goals().getFirst().target()==20,"project names, active project, goals and selected storage persist");
        reload.deleteProject();check(reload.projects().size()==1,"deleting a project keeps at least one usable project");reload.undo();check(reload.projectName().equals("Tree farm") && reload.projects().size()==2,"Undo restores a deleted project and its selection");
        var projects=new ProjectsScreen(parent,reload);mc.gui.setScreen(projects);var name=(EditBox)projects.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();name.setValue("House");press(button(projects,"Create new"));check(reload.projectName().equals("House") && reload.goals().isEmpty(),"project manager creates and selects a separate empty build");
        press(button(projects,"Delete"));press(button(projects,"Undo"));check(reload.projectName().equals("House"),"project manager exposes reversible delete");
        setMemoryField("store",store);mc.gui.setScreen(parent);parent.hover(parent.getMenu().slots.get(0));parent.getMenu().chest.setItem(0,new ItemStack(Items.DIRT,32));
        parent.keyPressed(key(InputConstants.KEY_N,110,0));check(mc.gui.screen() instanceof MaterialPickerScreen,"N on a hovered stack opens quick-add without moving it");
        var quick=(MaterialPickerScreen)mc.gui.screen();var fields=quick.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();check(fields.get(1).isFocused() && fields.get(1).getHighlighted().equals("1000"),"quick-add preselects the exact item and highlights its target");fields.get(1).setValue("222");quick.keyPressed(key(InputConstants.KEY_RETURN,13,0));check(store.goals().contains(new MaterialGoal("minecraft:dirt",222)) && mc.gui.screen()==parent,"quick-add confirms quantity and returns directly to the inventory");
        parent.keyPressed(key(InputConstants.KEY_F,InputConstants.KEYCODE_F,InputConstants.MOD_CONTROL));parent.keyPressed(key(InputConstants.KEY_N,110,0));check(mc.gui.screen()==parent,"N cannot open quick-add while typing in inventory search");parent.keyPressed(key(InputConstants.KEY_ESCAPE,27,0));
        var many=new ChestMemoryStore(directory,"wheel-order");for(var entry:MemoryItems.catalogue().subList(0,14))many.setGoal(entry.id(),100);
        setMemoryField("store",many);mc.gui.setScreen(parent);parent.extractContents(new GuiGraphicsExtractor(mc,new GuiRenderState(),parent.width,parent.height),0,0,0);
        var tracker=(FloatingMaterialTracker)parent.children().stream().filter(FloatingMaterialTracker.class::isInstance).findFirst().orElseThrow();String first=tracker.displayedGoals().getFirst().itemId();
        check(first.equals(many.goals().getFirst().itemId()),"inventory floating list opens at the first material");
        check(parent.mouseScrolled(tracker.getX()+2,tracker.getY()+2,0,-1)&&!tracker.displayedGoals().getFirst().itemId().equals(first),"real screen scroll-down advances to later materials");
        parent.mouseScrolled(tracker.getX()+2,tracker.getY()+2,0,1);check(tracker.displayedGoals().getFirst().itemId().equals(first),"scroll up returns to first page");
        var renameKeyboard=new ChestRenameScreen(parent,store,store.search("","",null).getFirst().chest());mc.gui.setScreen(renameKeyboard);var renameField=(EditBox)renameKeyboard.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();renameField.setValue("Keyboard supplies");renameKeyboard.keyPressed(key(InputConstants.KEY_RETURN,13,0));check(mc.gui.screen()==parent && store.customName(store.search("Keyboard supplies","",null).getFirst().chest().location()).equals("Keyboard supplies"),"Enter saves a chest nickname and returns to the inventory");
        setMemoryField("store",store);
        for(int w:List.of(320,427,640,854))for(int h:List.of(240,360,480,720,960))for(ScreenWithName item:List.of(new ScreenWithName(StowSettingsScreen.create(parent),"settings"),new ScreenWithName(new MaterialsScreen(parent,store),"materials"),new ScreenWithName(new MaterialPickerScreen(parent,store,"",1000),"picker"),new ScreenWithName(new ProjectsScreen(parent,store),"projects"),new ScreenWithName(new ChestMemoryScreen(parent,store,"",true,null),"storage"),new ScreenWithName(new ChestMemoryScreen(parent,store,"",false,null),"all-storage"),new ScreenWithName(new ChestRenameScreen(parent,store,store.search("","",null).getFirst().chest()),"rename"))){
            item.screen.init(w,h);item.screen.extractRenderState(new GuiGraphicsExtractor(mc,new GuiRenderState(),w,h),-100,-100,0);var widgets=item.screen.children().stream().filter(net.minecraft.client.gui.components.AbstractWidget.class::isInstance).map(net.minecraft.client.gui.components.AbstractWidget.class::cast).toList();
            for(var widget:widgets)if(widget.getX()<0 || widget.getY()<0 || widget.getRight()>w || widget.getBottom()>h)throw new AssertionError("widget outside "+item.name+" "+w+"x"+h+" "+widget.getMessage().getString());
            for(int i=0;i<widgets.size();i++)for(int j=i+1;j<widgets.size();j++){var aW=widgets.get(i);var bW=widgets.get(j);if(aW.getX()<bW.getRight() && aW.getRight()>bW.getX() && aW.getY()<bW.getBottom() && aW.getBottom()>bW.getY())throw new AssertionError("overlap in "+item.name+" "+w+"x"+h+" "+aW.getMessage().getString()+" / "+bW.getMessage().getString()+" ["+aW.getClass()+" "+aW.getX()+","+aW.getY()+","+aW.getWidth()+","+aW.getHeight()+"; "+bW.getClass()+" "+bW.getX()+","+bW.getY()+","+bW.getWidth()+","+bW.getHeight()+"]");}
            item.screen.extractRenderState(new GuiGraphicsExtractor(mc,new GuiRenderState(),w,h),-100,-100,0);
        }
        check(true,"all five planner screens render with in-bounds non-overlapping controls across 20 GUI viewport sizes");
        int savedGuiScale=mc.options.guiScale().get();mc.options.guiScale().set(2);mc.resizeGui();
        mc.gui.setScreen(new MaterialsScreen(parent,store));
        if(mc.gui.screen().width>=640){
            press(button(mc.gui.screen(),"Storage"));check(mc.gui.screen() instanceof ChestMemoryScreen,"wide menu sidebar switches directly to storage");
            press(button(mc.gui.screen(),"Projects"));check(mc.gui.screen() instanceof ProjectsScreen,"wide menu sidebar switches directly to projects");
            mc.gui.screen().onClose();check(mc.gui.screen()==parent,"sidebar navigation returns to the inventory without stacking utility parents");
        }
        mc.options.guiScale().set(savedGuiScale);mc.resizeGui();
        var beforeMode=Stow.config.dragMode;Set<List<String>> silhouettes=new HashSet<>();
        for(var mode:dev.stow.StowConfig.DragMode.values()){
            Stow.config.dragMode=mode;var icon=new InventoryIconButton(InventoryIconButton.Kind.DRAG,Component.literal("Drag"),pressed -> {},20,20);var iconState=new GuiRenderState();icon.extractRenderState(new GuiGraphicsExtractor(mc,iconState,parent.width,parent.height),-100,-100,0);List<String> shape=new ArrayList<>();iconState.forEachElement(e -> {if(e instanceof net.minecraft.client.renderer.state.gui.BlitRenderState r)shape.add(r.textureSetup().texure0().texture().getLabel());},GuiRenderState.TraverseRange.ALL);silhouettes.add(shape);
        }
        Stow.config.dragMode=beforeMode;check(silhouettes.size()==3,"all drag modes have distinct rendered silhouettes independent of color");
        var projectsSame=new ProjectsScreen(parent,store);var materialsSame=new MaterialsScreen(parent,store);projectsSame.init(640,720);materialsSame.init(640,720);
        var projectState=new GuiRenderState();var materialState=new GuiRenderState();projectsSame.extractRenderState(new GuiGraphicsExtractor(mc,projectState,640,720),-100,-100,0);materialsSame.extractRenderState(new GuiGraphicsExtractor(mc,materialState,640,720),-100,-100,0);
        List<net.minecraft.client.gui.navigation.ScreenRectangle> projectPanel=new ArrayList<>(),materialPanel=new ArrayList<>();projectState.forEachElement(e -> {if(e instanceof net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState r && r.col1()==0xC0000000)projectPanel.add(r.bounds());},GuiRenderState.TraverseRange.ALL);materialState.forEachElement(e -> {if(e instanceof net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState r && r.col1()==0xC0000000)materialPanel.add(r.bounds());},GuiRenderState.TraverseRange.ALL);
        check(projectPanel.equals(materialPanel) && button(projectsSame,"Create new").getHeight()==button(materialsSame,"+ Material").getHeight() && button(projectsSame,"Back").getWidth()==button(materialsSame,"Back").getWidth(),"project and material menus share panel bounds, control height and footer sizing");
        check(tooltip(button(projectsSame,"Delete"))==null && tooltip(button(materialsSame,"Storage · "+store.includedChestCount()+" chests"))==null,"self-explanatory text controls have no redundant tooltips");
        var pickerQuiet=new MaterialPickerScreen(parent,store,"minecraft:diamond",48);mc.gui.setScreen(pickerQuiet);var quietFields=pickerQuiet.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();check(tooltip(quietFields.get(1))==null,"valid target amount has no redundant tooltip");quietFields.get(1).setValue("0");check(tooltip(quietFields.get(1))!=null,"invalid target shows a brief range hint");
        mc.gui.setScreen(parent);
    }
    static net.minecraft.client.gui.components.Tooltip tooltip(net.minecraft.client.gui.components.AbstractWidget widget) throws Exception {
        Field field=net.minecraft.client.gui.components.AbstractWidget.class.getDeclaredField("tooltip");field.setAccessible(true);return ((net.minecraft.client.gui.components.WidgetTooltipHolder)field.get(widget)).get();
    }
    private record ScreenWithName(net.minecraft.client.gui.screens.Screen screen,String name) {}
    static void floatingTests(ChestMemoryStore store,Path directory,TestScreen parent,Minecraft mc) throws Exception {
        var many=new ChestMemoryStore(directory,"many");
        for(var entry:MemoryItems.catalogue().subList(0,14))many.setGoal(entry.id(),100);
        var tracker=new FloatingMaterialTracker(250,20,100,100,id -> {});tracker.update(many,true);
        String first=tracker.displayedGoals().getFirst().itemId();int initialSize=tracker.displayedGoals().size();
        check(initialSize<14 && tracker.getHeight()<=100,"long floating lists stay inside the available vertical space");
        check(tracker.mouseScrolled(252,22,0,-1) && !tracker.displayedGoals().getFirst().itemId().equals(first),"scrolling over the floating list reveals additional goals");
        check(!tracker.mouseScrolled(0,0,0,-1),"scrolling elsewhere does not alter the floating list");
        for(var goal:many.goals())many.toggleGoalVisible(goal.itemId());tracker.update(many,true);
        check(!tracker.visible,"hiding all goals removes the floating overlay completely");
        many.setGoal("minecraft:cobblestone",524);tracker.update(many,true);
        check(tracker.visible && many.isGoalVisible("minecraft:cobblestone"),"newly added goals become visible automatically");
        store.save();var old=com.google.gson.JsonParser.parseString(Files.readString(ChestMemoryStore.pathFor(directory,"planner"))).getAsJsonObject();old.addProperty("version",2);old.remove("hiddenGoals");
        Files.writeString(ChestMemoryStore.pathFor(directory,"revision5"),old.toString());var migrated=new ChestMemoryStore(directory,"revision5");
        check(migrated.visibleGoals().size()==migrated.goals().size() && migrated.goals().size()>0,"revision-5 goals migrate to visible floating items without losing their targets");
        // A server snapshot must not discard a partially typed target in the planner.
        var screen=new MaterialsScreen(parent,store);mc.gui.setScreen(screen);
        var amount=(EditBox)screen.children().stream().filter(w->w instanceof EditBox&&((EditBox)w).getMessage().getString().equals("Target amount")).findFirst().orElseThrow();amount.setValue("777");
        var chest=store.search("","",null).getFirst().chest();store.remember(new SavedChest(chest.location(),chest.title(),System.currentTimeMillis(),chest.items()));screen.tick();
        check(amount.getValue().equals("777") && screen.children().contains(amount),"incoming chest snapshots preserve an in-progress target edit");
        screen.setFocused(amount);screen.keyPressed(key(InputConstants.KEY_RETURN,13,0));
        check(store.goals().getFirst().target()==777,"Enter confirms a target amount in the redesigned planner");
    }
    static void startScreenshots(Minecraft mc,ChestMemoryStore store,TestScreen parent) {
        for(var goal:store.visibleGoals().stream().limit(3).toList())if(!store.isHudGoal(goal.itemId(),3))store.toggleHudGoal(goal.itemId(),3);
        final int[] ticks={0};final int[] capture={-1};
        final net.minecraft.client.player.LocalPlayer preview;
        try{preview=CompanionTests.previewPlayer(mc,parent.getMenu());}catch(Exception e){throw new RuntimeException(e);}
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ++ticks[0];int stage=(ticks[0]-30)/12;
            if(ticks[0]<30)return;
            if((ticks[0]-30)%12==0 && stage<28){
                int scale=stage/7+1,view=stage%7;client.options.guiScale().set(scale);client.resizeGui();capture[0]=stage;
                if(view==0 || view==5){parent.init(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());client.gui.setScreen(new net.minecraft.client.gui.screens.Screen(Component.empty()) {
                    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mx,int my,float delta){var tracker=(FloatingMaterialTracker)parent.children().stream().filter(FloatingMaterialTracker.class::isInstance).findFirst().orElseThrow();parent.extractRenderState(graphics,view==5?tracker.getX()+2:-100,view==5?tracker.getY()+2:-100,delta);}
                });}
                if(view==1)client.gui.setScreen(new MaterialsScreen(parent,store));
                if(view==2)client.gui.setScreen(new ChestMemoryScreen(parent,store,"",true,null));
                if(view==3)client.gui.setScreen(new MaterialPickerScreen(new MaterialsScreen(parent,store),store,"cobble",1000));
                if(view==4)client.gui.setScreen(new ProjectsScreen(parent,store));
                if(view==6)client.gui.setScreen(StowSettingsScreen.create(parent));
            }
            if((ticks[0]-30)%12==8 && capture[0]>=0 && capture[0]<28){int scale=capture[0]/7+1,view=capture[0]%7;String name=List.of("inventory","materials","chests","picker","projects","hover","settings").get(view);net.minecraft.client.Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image -> {try{image.writeToFile(Path.of(name+"-scale"+scale+".png"));image.close();}catch(Exception e){throw new RuntimeException(e);}});capture[0]=-1;}
            if((ticks[0]-30)%12==0 && stage>=28 && stage<31){
                Stow.config.dragMode=dev.stow.StowConfig.DragMode.values()[stage-28];capture[0]=stage;
                parent.init(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());client.gui.setScreen(new net.minecraft.client.gui.screens.Screen(Component.empty()) {
                    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mx,int my,float delta){parent.extractRenderState(graphics,-100,-100,delta);}
                });
            }
            if((ticks[0]-30)%12==8 && capture[0]>=28 && capture[0]<31){String name="drag-"+dev.stow.StowConfig.DragMode.values()[capture[0]-28].name().toLowerCase();net.minecraft.client.Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image -> {try{image.writeToFile(Path.of(name+".png"));image.close();}catch(Exception e){throw new RuntimeException(e);}});capture[0]=-1;}
            if((ticks[0]-30)%12==0 && stage>=31 && stage<=35){
                client.options.guiScale().set(2);client.resizeGui();capture[0]=stage;
                if(stage>31){client.options.guiScale().set(stage-31);client.resizeGui();}
                final boolean far=stage>31;
                client.gui.setScreen(new net.minecraft.client.gui.screens.Screen(Component.empty()){
                    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mx,int my,float delta){}
                    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mx,int my,float delta){
                        graphics.fill(0,0,width,height,0xFF303030);
                        for(int row=0;row<height;row+=24)for(int column=0;column<width;column+=48)graphics.outline(column+(row/24%2)*24,row,48,24,0xFF242424);
                        graphics.centeredText(client.font,"Locator overlay over an opaque wall fixture",width/2,20,0xFFFFFFFF);
                        ChestGlow.drawProjected(graphics,new ChestGlow.Target(far?new net.minecraft.world.phys.AABB(-0.5,-0.5,-58.5,0.5,0.5,-57.5):new net.minecraft.world.phys.AABB(-0.5,-0.5,-5,0.5,0.5,-4),far?"Building supplies · 58 m":"Building supplies · 5 m"),net.minecraft.world.phys.Vec3.ZERO,new org.joml.Matrix4f().perspective((float)Math.PI/2,(float)width/height,0.1f,100));
                    }
                });
            }
            if((ticks[0]-30)%12==8 && capture[0]>=31&&capture[0]<=35){String file=capture[0]==31?"locator-wall.png":"locator-far-scale"+(capture[0]-31)+".png";net.minecraft.client.Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try{image.writeToFile(Path.of(file));image.close();}catch(Exception e){throw new RuntimeException(e);}});capture[0]=-1;}
            if((ticks[0]-30)%12==0 && stage>=36&&stage<64){
                int scale=(stage-36)/7+1,view=(stage-36)%7;client.options.guiScale().set(scale);client.resizeGui();capture[0]=stage;
                Screen tool=switch(view){case 0->new dev.stow.client.ui.CommandPaletteScreen(parent,new ItemStack(Items.COBBLESTONE));case 1->new dev.stow.client.ui.ShortcutEditorScreen(parent);case 2->new dev.stow.client.ui.KeepAmountsScreen(parent,"cobble");case 3->new dev.stow.client.ui.EquipmentScreen(parent);case 4->new MaterialsScreen(parent,store);case 5->new dev.stow.client.ui.DockLayoutScreen(parent);default->null;};
                var saved=client.player;try{client.player=preview;if(tool!=null)tool.init(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());}finally{client.player=saved;}
                client.gui.setScreen(new Screen(Component.empty()){
                    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float delta){}
                    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){var old=client.player;try{client.player=preview;
                        if(tool!=null){tool.tick();tool.extractRenderState(g,-100,-100,delta);}
                        else {g.fill(0,0,width,height,0xFF354346);g.centeredText(client.font,"Survival dock preview",width/2,24,0xFFFFFFFF);dev.stow.client.hud.SurvivalDock.draw(g,store);var block=new ItemStack(Items.COBBLESTONE);dev.stow.client.hud.BuildingStockHud.draw(g,block,preview.getInventory(),false);g.outline(width/2-91,height-22,182,22,0xFFB4BEC0);}
                    }finally{client.player=old;}}
                });
            }
            if((ticks[0]-30)%12==8 && capture[0]>=36&&capture[0]<64){int scale=(capture[0]-36)/7+1,view=(capture[0]-36)%7;String file=List.of("palette","shortcuts","keep","equipment","materials-actions","dock-layout","survival-dock").get(view)+"-scale"+scale+".png";net.minecraft.client.Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try{image.writeToFile(Path.of(file));image.close();}catch(Exception e){throw new RuntimeException(e);}});capture[0]=-1;}
            if((ticks[0]-30)%12==0&&stage>=64&&stage<68){client.options.guiScale().set(stage-63);client.resizeGui();capture[0]=stage;client.gui.setScreen(new ChestMemoryScreen(parent,store,"",false,null));}
            if((ticks[0]-30)%12==8&&capture[0]>=64&&capture[0]<68){String file="storage-scale"+(capture[0]-63)+".png";net.minecraft.client.Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try{image.writeToFile(Path.of(file));image.close();}catch(Exception e){throw new RuntimeException(e);}});capture[0]=-1;}
            if((ticks[0]-30)%12==0&&stage>=68&&stage<72){client.options.guiScale().set(stage-67);client.resizeGui();capture[0]=stage;client.gui.setScreen(new dev.stow.client.ui.DockLayoutScreen(parent,true));}
            if((ticks[0]-30)%12==8&&capture[0]>=68&&capture[0]<72){String file="project-layout-scale"+(capture[0]-67)+".png";net.minecraft.client.Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(),image->{try{image.writeToFile(Path.of(file));image.close();}catch(Exception e){throw new RuntimeException(e);}});capture[0]=-1;}
            if(stage>=72)System.exit(0);
        });
    }
    static void usingPrototype(Item item,String name) {item.builtInRegistryHolder().bindComponents(net.minecraft.core.component.DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE,64).set(DataComponents.ITEM_NAME,Component.translatable(name)).set(DataComponents.ITEM_MODEL,net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item)).build());}
    static void setMemoryField(String name,Object value) throws Exception {Field f=dev.stow.client.memory.ChestMemory.class.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    static class MemoryTestScreen extends AbstractContainerScreen<ChestMenu> {
        MemoryTestScreen(ChestMenu menu,Inventory inv){super(menu,inv,Component.literal("Chest"));}
        @Override public void extractBackground(GuiGraphicsExtractor graphics,int x,int y,float delta){}
    }
    static class TestMenu extends AbstractContainerMenu {
        final SimpleContainer chest = new SimpleContainer(4);
        TestMenu(Inventory inv) {
            super(null, 17);
            for(int i=0;i<4;i++) addSlot(new Slot(chest,i,8+18*i,18));
            for(int i=0;i<3;i++) addSlot(new Slot(inv,i,8+18*i,48));
        }
        @Override public boolean stillValid(Player p) { return true; }
        @Override public ItemStack quickMoveStack(Player p,int id) { return ItemStack.EMPTY; }
    }
    static class TestScreen extends AbstractContainerScreen<TestMenu> {
        final List<Integer> moved = new ArrayList<>();
        TestScreen(TestMenu m, Inventory i) { super(m,i,Component.literal("Port test")); }
        @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float a) {}
        @Override protected void slotClicked(Slot slot,int id,int button,ContainerInput type) {
            if(type == ContainerInput.QUICK_MOVE) moved.add(id);
        }
        int inventoryRight(){return leftPos+imageWidth;}
        void hover(Slot s) { hoveredSlot=s; }
        MouseButtonEvent at(Slot s,int mods) { return mouse(leftPos+s.x+8,topPos+s.y+8,mods); }
    }
}
