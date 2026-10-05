package porttest;

import dev.stow.*;
import dev.stow.client.inventory.*;
import dev.stow.client.hud.*;
import dev.stow.client.memory.*;
import static dev.stow.client.memory.ChestMemoryStore.*;
import java.util.*;
import java.lang.reflect.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.core.*;
import net.minecraft.core.component.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

final class BuildAidTests {
    private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;System.out.println("BUILD AID PASS: "+message);}
    static int run(Minecraft mc)throws Exception{
        var oldPlayer=mc.player;var oldLevel=mc.level;var oldMode=mc.gameMode;var oldScreen=mc.gui.screen();var oldHit=mc.hitResult;
        boolean oldRefill=Stow.config.handRefill,oldPick=Stow.config.toolPick,oldStock=Stow.config.buildingStock,oldAuto=Stow.config.autoTool,oldSneak=mc.options.keyShift.isDown();
        var changed=new LinkedHashMap<Item,DataComponentMap>();
        try{
            Field uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);var unsafe=(sun.misc.Unsafe)uf.get(null);
            var listener=(net.minecraft.client.multiplayer.ClientPacketListener)unsafe.allocateInstance(net.minecraft.client.multiplayer.ClientPacketListener.class);
            FeatureTest.setField(net.minecraft.client.multiplayer.ClientPacketListener.class,listener,"enabledFeatures",net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
            var level=(FixtureLevel)unsafe.allocateInstance(FixtureLevel.class);level.block=Blocks.STONE.defaultBlockState();FeatureTest.setField(net.minecraft.client.multiplayer.ClientLevel.class,level,"connection",listener);
            var player=(FixturePlayer)unsafe.allocateInstance(FixturePlayer.class);var equipment=new EntityEquipment();var inventory=new Inventory(player,equipment);
            FeatureTest.setField(Player.class,player,"inventory",inventory);FeatureTest.setField(Player.class,player,"abilities",new Abilities());FeatureTest.setField(LivingEntity.class,player,"equipment",equipment);FeatureTest.setField(LivingEntity.class,player,"attributes",new AttributeMap(Player.createAttributes().build()));FeatureTest.setField(net.minecraft.world.entity.Entity.class,player,"level",level);FeatureTest.setField(net.minecraft.client.player.LocalPlayer.class,player,"minecraft",mc);
            var menu=new InventoryMenu(inventory,false,player);FeatureTest.setField(Player.class,player,"inventoryMenu",menu);player.containerMenu=menu;
            mc.player=player;mc.level=level;mc.gameMode=new net.minecraft.client.multiplayer.MultiPlayerGameMode(mc,listener){@Override public void handleContainerInput(int id,int slot,int button,ContainerInput input,Player ignored){menu.clicked(slot,button,input,player);}};
            // Avoid normal screen-removal hooks on the controlled fake level.
            FeatureTest.setField(net.minecraft.client.gui.Gui.class,mc.gui,"screen",null);
            Stow.config.handRefill=Stow.config.toolPick=Stow.config.buildingStock=true;
            refill(mc,inventory,menu);tool(mc,inventory,menu,changed);autoTool(mc,inventory,menu,changed);hud(mc,inventory);projectHud(mc,inventory);
        }finally{
            changed.forEach((item,components)->item.builtInRegistryHolder().bindComponents(components));HandRefill.cancel();Stow.config.handRefill=oldRefill;Stow.config.toolPick=oldPick;Stow.config.buildingStock=oldStock;Stow.config.autoTool=oldAuto;mc.options.keyShift.setDown(oldSneak);
            mc.player=oldPlayer;mc.level=oldLevel;mc.gameMode=oldMode;mc.hitResult=oldHit;FeatureTest.setField(net.minecraft.client.gui.Gui.class,mc.gui,"screen",oldScreen);
        }
        return checks;
    }
    private static Slot slot(AbstractContainerMenu menu,int i){return menu.slots.stream().filter(s->s.container instanceof Inventory&&s.getContainerSlot()==i).findFirst().orElseThrow();}
    private static void refill(Minecraft mc,Inventory inv,InventoryMenu menu){
        inv.clearContent();inv.setSelectedSlot(0);inv.setItem(0,new ItemStack(Items.COBBLESTONE,9));inv.setItem(9,new ItemStack(Items.COBBLESTONE,64));inv.setItem(10,new ItemStack(Items.COBBLESTONE,40));
        var before=inv.getItem(0).copy();inv.getItem(0).shrink(1);var total=FeatureTest.contents(menu);HandRefill.placed(mc.player,0,before);HandRefill.tick(mc);
        check(inv.getItem(0).getCount()==64&&inv.getItem(9).getCount()==8&&FeatureTest.contents(menu).equals(total)&&menu.getCarried().isEmpty(),"low building stack tops up through vanilla clicks without losing items");
        inv.clearContent();inv.setItem(0,new ItemStack(Items.COBBLESTONE,1));inv.setItem(9,new ItemStack(Items.COBBLESTONE,12));before=inv.getItem(0).copy();inv.setItem(0,ItemStack.EMPTY);HandRefill.placed(mc.player,0,before);HandRefill.tick(mc);
        check(inv.getItem(0).getCount()==12&&inv.getItem(9).isEmpty(),"last placed block refills the same selected slot");
        inv.clearContent();inv.setItem(0,new ItemStack(Items.COBBLESTONE,1));before=inv.getItem(0).copy();inv.setItem(0,ItemStack.EMPTY);HandRefill.placed(mc.player,0,before);HandRefill.tick(mc);
        check(HandRefill.stockItem(mc.player).isEmpty(),"empty hand immediately clears building stock without scrolling the hotbar");
        inv.setItem(0,new ItemStack(Items.COBBLESTONE,8));before=inv.getItem(0).copy();inv.setItem(9,new ItemStack(Items.COBBLESTONE,64));HandRefill.placed(mc.player,0,before);HandRefill.tick(mc);check(inv.getItem(0).getCount()==8,"failed placement or unchanged count cannot trigger refill");
        before=inv.getItem(0).copy();inv.getItem(0).shrink(1);HandRefill.placed(mc.player,0,before);inv.setSelectedSlot(1);HandRefill.tick(mc);check(inv.getItem(0).getCount()==7,"switching hotbar slots cancels pending refill");inv.setSelectedSlot(0);
        var named=new ItemStack(Items.COBBLESTONE,64);named.set(DataComponents.CUSTOM_NAME,Component.literal("Special"));inv.setItem(9,named);check(HandRefill.sources(menu,inv,0,inv.getItem(0)).isEmpty(),"refill refuses a differently named/component stack");
        inv.setItem(9,new ItemStack(Items.COBBLESTONE,64));PinnedSlots.toggle(slot(menu,9));check(HandRefill.sources(menu,inv,0,inv.getItem(0)).isEmpty(),"refill leaves pinned reserve slots untouched");PinnedSlots.toggle(slot(menu,9));
        PinnedSlots.toggle(slot(menu,0));int added=HandRefill.topUp(menu,inv,0,inv.getItem(0).copy(),(id,b)->menu.clicked(id,b,ContainerInput.PICKUP,mc.player));check(added==57&&inv.getItem(0).getCount()==64,"a pinned building slot may refill its existing item type");PinnedSlots.toggle(slot(menu,0));
        inv.setItem(0,new ItemStack(Items.COBBLESTONE,8));before=inv.getItem(0).copy();inv.getItem(0).shrink(1);Stow.config.handRefill=false;HandRefill.placed(mc.player,0,before);HandRefill.tick(mc);check(inv.getItem(0).getCount()==7,"disabled refill cannot move items");Stow.config.handRefill=true;
        var random=new Random(19283);
        for(int n=0;n<30;n++){inv.clearContent();menu.setCarried(ItemStack.EMPTY);inv.setItem(0,new ItemStack(Items.COBBLESTONE,random.nextInt(9)));for(int i=1;i<36;i++)if(random.nextBoolean()){var item=new ItemStack(random.nextBoolean()?Items.COBBLESTONE:Items.DIRT,1+random.nextInt(64));if(random.nextInt(4)==0)item.set(DataComponents.CUSTOM_NAME,Component.literal("Named"));inv.setItem(i,item);}
            var contents=FeatureTest.contents(menu);HandRefill.topUp(menu,inv,0,new ItemStack(Items.COBBLESTONE),(id,b)->menu.clicked(id,b,ContainerInput.PICKUP,mc.player));check(FeatureTest.contents(menu).equals(contents)&&menu.getCarried().isEmpty()&&inv.getItem(0).getCount()<=64,"random refill "+n+" preserves components, total counts and empty cursor");}
        HandRefill.cancel();
    }
    private static ItemStack toolStack(Item item,List<Tool.Rule> rules,float speed,Map<Item,DataComponentMap> changed){
        changed.putIfAbsent(item,item.components());item.builtInRegistryHolder().bindComponents(DataComponentMap.builder().set(DataComponents.MAX_STACK_SIZE,1).set(DataComponents.MAX_DAMAGE,250).set(DataComponents.DAMAGE,0).set(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY).set(DataComponents.ITEM_NAME,Component.translatable(item.getDescriptionId())).set(DataComponents.TOOL,new Tool(rules,speed,1,true)).build());return new ItemStack(item);
    }
    private static void tool(Minecraft mc,Inventory inv,InventoryMenu menu,Map<Item,DataComponentMap> changed)throws Exception{
        var stone=Blocks.STONE.defaultBlockState();var stoneSet=HolderSet.direct(Blocks.STONE.builtInRegistryHolder());var dirtSet=HolderSet.direct(Blocks.DIRT.builtInRegistryHolder());
        var diamond=toolStack(Items.DIAMOND_PICKAXE,List.of(Tool.Rule.minesAndDrops(stoneSet,8)),1,changed);
        var gold=toolStack(Items.GOLDEN_PICKAXE,List.of(Tool.Rule.overrideSpeed(stoneSet,12),Tool.Rule.deniesDrops(stoneSet)),1,changed);
        var shovel=toolStack(Items.IRON_SHOVEL,List.of(Tool.Rule.minesAndDrops(dirtSet,6)),1,changed);
        inv.clearContent();inv.setSelectedSlot(0);inv.setItem(0,new ItemStack(Items.COBBLESTONE,32));inv.setItem(9,gold);inv.setItem(10,diamond);inv.setItem(2,shovel);
        check(ToolPicker.best(inv,menu,stone,null)==10,"valid harvest tool wins over faster wrong-tier tool");check(ToolPicker.best(inv,menu,Blocks.DIRT.defaultBlockState(),null)==2,"dirt picks the hotbar shovel");
        var enchanted=diamond.copy();var definition=Enchantment.definition(HolderSet.direct(Items.DIAMOND_PICKAXE.builtInRegistryHolder()),1,5,Enchantment.constantCost(1),Enchantment.constantCost(1),1,EquipmentSlotGroup.MAINHAND);
        var efficiency=Holder.direct(Enchantment.enchantment(definition).withEffect(EnchantmentEffectComponents.ATTRIBUTES,new EnchantmentAttributeEffect(Identifier.fromNamespaceAndPath("porttest","efficiency"),Attributes.MINING_EFFICIENCY,LevelBasedValue.constant(17),AttributeModifier.Operation.ADD_VALUE)).build(Identifier.fromNamespaceAndPath("porttest","efficiency")));enchanted.enchant(efficiency,4);inv.setItem(11,enchanted);
        check(ToolPicker.best(inv,menu,stone,null)==11&&ToolPicker.speed(null,enchanted,stone)==25,"Efficiency enchantment modifies effective mining speed");
        var attributes=mc.player.getAttribute(Attributes.MINING_EFFICIENCY);double original=attributes.getValue();check(ToolPicker.speed(mc.player,enchanted,stone)==25&&attributes.getValue()==original,"candidate scoring never mutates live player attributes");
        PinnedSlots.toggle(slot(menu,11));check(ToolPicker.best(inv,menu,stone,null)==10,"tool picking cannot pull from a pinned backpack slot");PinnedSlots.toggle(slot(menu,11));
        inv.setItem(11,ItemStack.EMPTY);mc.hitResult=new BlockHitResult(Vec3.ZERO,Direction.UP,BlockPos.ZERO,false);
        check(ToolPicker.pick(mc)&&inv.getItem(0).is(Items.DIAMOND_PICKAXE)&&inv.getItem(10).is(Items.COBBLESTONE)&&inv.getSelectedSlot()==0&&menu.getCarried().isEmpty(),"backpack tool swaps into hotbar through normal inventory packets");
        check(ToolPicker.pick(mc)&&inv.getSelectedSlot()==0,"already fastest held tool remains selected");
        inv.setItem(0,new ItemStack(Items.COBBLESTONE,32));inv.setItem(10,diamond);var pick=Minecraft.class.getDeclaredMethod("pickBlockOrEntity");pick.setAccessible(true);pick.invoke(mc);check(inv.getItem(0).is(Items.DIAMOND_PICKAXE),"actual Minecraft middle-click entrypoint invokes the tool picker mixin");
        ((FixtureLevel)mc.level).block=Blocks.DIRT.defaultBlockState();check(ToolPicker.pick(mc)&&inv.getSelectedSlot()==2,"hotbar tool selection moves no inventory items");
        inv.setSelectedSlot(0);PinnedSlots.toggle(slot(menu,0));check(ToolPicker.destination(inv,menu)!=0,"pinned hotbar slot cannot be replaced by a backpack tool");PinnedSlots.toggle(slot(menu,0));
        Stow.config.toolPick=false;check(!ToolPicker.pick(mc),"disabled tool picker falls through to vanilla pick block");Stow.config.toolPick=true;
        mc.player.getAbilities().instabuild=true;check(!ToolPicker.pick(mc),"creative mode keeps vanilla pick block behavior");mc.player.getAbilities().instabuild=false;
    }
    private static void autoTool(Minecraft mc,Inventory inv,InventoryMenu menu,Map<Item,DataComponentMap> changed)throws Exception{
        var level=(FixtureLevel)mc.level;var pos=BlockPos.ZERO;
        var stoneSet=HolderSet.direct(Blocks.STONE.builtInRegistryHolder());
        var diamond=toolStack(Items.DIAMOND_PICKAXE,List.of(Tool.Rule.minesAndDrops(stoneSet,8)),1,changed);
        var gold=toolStack(Items.GOLDEN_PICKAXE,List.of(Tool.Rule.overrideSpeed(stoneSet,12),Tool.Rule.deniesDrops(stoneSet)),1,changed);
        var shovel=toolStack(Items.IRON_SHOVEL,List.of(Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.DIRT.builtInRegistryHolder()),6)),1,changed);
        var axe=toolStack(Items.IRON_AXE,List.of(Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.OAK_LOG.builtInRegistryHolder()),6)),1,changed);
        var shears=toolStack(Items.SHEARS,List.of(Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()),15)),1,changed);
        inv.clearContent();menu.setCarried(ItemStack.EMPTY);inv.setSelectedSlot(0);inv.setItem(0,new ItemStack(Items.COBBLESTONE,32));inv.setItem(1,diamond);inv.setItem(2,shovel);inv.setItem(3,axe);inv.setItem(4,shears);inv.setItem(5,gold);level.block=Blocks.STONE.defaultBlockState();mc.options.keyShift.setDown(false);
        Stow.config.autoTool=false;check(!ToolPicker.autoSwitch(mc,pos)&&inv.getSelectedSlot()==0,"auto tool off leaves the selected item unchanged");
        Stow.config.autoTool=true;Stow.config.toolPick=false;var contents=FeatureTest.contents(menu);
        check(ToolPicker.autoSwitch(mc,pos)&&inv.getSelectedSlot()==1,"auto mining selects a harvest-capable hotbar pickaxe independently of manual tool picking");
        check(!ToolPicker.autoSwitch(mc,pos)&&inv.getSelectedSlot()==1,"repeated mining retains the best tool without selection churn");
        level.block=Blocks.DIRT.defaultBlockState();check(ToolPicker.autoSwitch(mc,pos)&&inv.getSelectedSlot()==2,"continuous mining switches from stone to the dirt shovel");
        level.block=Blocks.OAK_LOG.defaultBlockState();check(ToolPicker.autoSwitch(mc,pos)&&inv.getSelectedSlot()==3,"logs select the hotbar axe");
        level.block=Blocks.COBWEB.defaultBlockState();check(ToolPicker.autoSwitch(mc,pos)&&inv.getSelectedSlot()==4,"component-based selection supports shears");
        check(contents.equals(FeatureTest.contents(menu))&&menu.getCarried().isEmpty(),"automatic selection never rearranges inventory items");
        level.block=Blocks.STONE.defaultBlockState();inv.setItem(9,inv.getItem(1));inv.setItem(1,ItemStack.EMPTY);inv.setItem(5,ItemStack.EMPTY);inv.setSelectedSlot(0);
        check(!ToolPicker.autoSwitch(mc,pos)&&inv.getSelectedSlot()==0,"automatic mining cannot pull tools from the backpack");inv.setItem(1,inv.getItem(9));inv.setItem(9,ItemStack.EMPTY);
        PinnedSlots.toggle(slot(menu,1));check(ToolPicker.autoSwitch(mc,pos)&&inv.getSelectedSlot()==1&&PinnedSlots.isPinned(slot(menu,1)),"pinned hotbar tools may be selected without moving their slot");PinnedSlots.toggle(slot(menu,1));
        inv.setItem(6,diamond.copy());inv.setSelectedSlot(6);check(!ToolPicker.autoSwitch(mc,pos)&&inv.getSelectedSlot()==6,"equal-speed ties retain the currently held tool");
        inv.setSelectedSlot(0);mc.options.keyShift.setDown(true);check(!ToolPicker.autoSwitch(mc,pos)&&inv.getSelectedSlot()==0,"holding Sneak temporarily prevents automatic tool changes");mc.options.keyShift.setDown(false);
        level.block=Blocks.AIR.defaultBlockState();check(!ToolPicker.autoSwitch(mc,pos),"air cannot trigger tool selection");level.block=Blocks.BEDROCK.defaultBlockState();check(!ToolPicker.autoSwitch(mc,pos),"unbreakable blocks cannot trigger tool selection");level.block=Blocks.STONE.defaultBlockState();
        menu.setCarried(new ItemStack(Items.DIRT));check(!ToolPicker.autoSwitch(mc,pos),"carried inventory items prevent automatic selection");menu.setCarried(ItemStack.EMPTY);
        ((FixturePlayer)mc.player).usingItem=true;check(!ToolPicker.autoSwitch(mc,pos),"using an item prevents automatic selection");((FixturePlayer)mc.player).usingItem=false;
        mc.player.getAbilities().instabuild=true;check(!ToolPicker.autoSwitch(mc,pos),"creative pick-block behavior remains unchanged");mc.player.getAbilities().instabuild=false;
        FeatureTest.setField(net.minecraft.client.gui.Gui.class,mc.gui,"screen",new net.minecraft.client.gui.screens.Screen(Component.literal("Open menu")){});check(!ToolPicker.autoSwitch(mc,pos),"open menus prevent automatic selection");FeatureTest.setField(net.minecraft.client.gui.Gui.class,mc.gui,"screen",null);
        var action=dev.stow.client.input.StowShortcuts.Action.AUTO_TOOL;
        check(dev.stow.client.input.StowShortcuts.matches(action,new StowConfig.Shortcut(false,com.mojang.blaze3d.platform.InputConstants.KEY_T,256))&&dev.stow.client.input.StowShortcuts.matches(action,new StowConfig.Shortcut(false,com.mojang.blaze3d.platform.InputConstants.KEY_T,512)),"Alt+T accepts either Alt key");
        check(!dev.stow.client.input.StowShortcuts.matches(action,new StowConfig.Shortcut(false,com.mojang.blaze3d.platform.InputConstants.KEY_T,0)),"plain T remains vanilla chat input");
        Stow.config.autoTool=false;var event=FeatureTest.key(com.mojang.blaze3d.platform.InputConstants.KEY_T,116,256);long window=mc.getWindow().handle();mc.keyboardHandler.keyPress(window,1,event);check(Stow.config.autoTool&&FeatureTest.reloadConfig().autoTool,"actual keyboard press enables and saves auto tool");
        mc.keyboardHandler.keyPress(window,2,event);check(Stow.config.autoTool,"keyboard repeat cannot toggle the feature repeatedly");mc.keyboardHandler.keyPress(window,1,event);check(!Stow.config.autoTool&&!FeatureTest.reloadConfig().autoTool,"second hotkey press disables and saves auto tool");
        Stow.config.toolPick=true;
    }
    private static void hud(Minecraft mc,Inventory inv){
        inv.clearContent();inv.setItem(0,new ItemStack(Items.COBBLESTONE,8));inv.setItem(9,new ItemStack(Items.COBBLESTONE,64));var named=new ItemStack(Items.COBBLESTONE,5);named.set(DataComponents.CUSTOM_NAME,Component.literal("Special"));inv.setItem(10,named);
        check(BuildingStockHud.count(inv,new ItemStack(Items.COBBLESTONE))==72,"hotbar stock sums held plus matching inventory reserves, excluding other components");
        for(int width:new int[]{320,427,640,1280})for(int height:new int[]{240,480})for(float scale:new float[]{1,2,3,4})for(boolean offhand:new boolean[]{false,true}){var b=BuildingStockHud.bounds(width,height,60,scale,offhand);if(b.x()<0||b.y()<0||b.x()+60*b.scale()>width||b.y()+16*b.scale()>height)throw new AssertionError("stock HUD outside viewport");}check(true,"hotbar count stays in bounds across GUI scales and offhand layouts");
        var state=new GuiRenderState();BuildingStockHud.draw(new GuiGraphicsExtractor(mc,state,320,240),inv.getItem(0),inv,false);var icons=new ArrayList<>();state.forEachItem(icons::add);var rectangles=new ArrayList<>();state.forEachElement(rectangles::add,GuiRenderState.TraverseRange.ALL);check(icons.size()==1&&rectangles.isEmpty(),"hotbar stock renders one item icon without a background");
    }
    private static void projectHud(Minecraft mc,Inventory inv)throws Exception{
        var config=Stow.config;String snapshot=new com.google.gson.Gson().toJson(config);
        try{
            var store=new ChestMemoryStore(java.nio.file.Files.createTempDirectory("stow-hud-"),"hud");var chest=new Location("minecraft:overworld",0,64,0,"Chest");
            store.remember(new SavedChest(chest,"Chest",System.currentTimeMillis(),List.of(new MemoryItem("minecraft:cobblestone","Cobblestone",400))));store.setGoal("minecraft:cobblestone",1000);
            inv.clearContent();inv.setItem(0,new ItemStack(Items.COBBLESTONE,24));config.dockMaterials=true;config.dockRows=3;store.toggleHudGoal("minecraft:cobblestone",3);
            check(SurvivalDock.projectRows(store).getFirst().label().equals("424 / 1000"),"project HUD combines current inventory and remembered storage");
            inv.setItem(0,new ItemStack(Items.COBBLESTONE,23));check(SurvivalDock.projectRows(store).getFirst().label().equals("423 / 1000"),"project HUD updates as inventory changes");
            store.remember(new SavedChest(chest,"Chest",System.currentTimeMillis(),List.of(new MemoryItem("minecraft:cobblestone","Cobblestone",500))));check(SurvivalDock.projectRows(store).getFirst().label().equals("523 / 1000"),"project HUD updates after a new chest snapshot");
            config.dockRows=1;store.setGoal("minecraft:dirt",100);check(SurvivalDock.projectRows(store).size()==1,"project HUD only shows its selected row limit");
            config.dockEnabled=false;var state=new GuiRenderState();SurvivalDock.draw(new GuiGraphicsExtractor(mc,state,320,240),store);var icons=new ArrayList<>();state.forEachItem(icons::add);check(icons.size()==1,"project visibility is independent of equipment HUD visibility");
            config.dockMaterials=false;check(SurvivalDock.projectRows(store).isEmpty(),"project totals can be hidden separately");
            for(var corner:dev.stow.StowConfig.DockCorner.values())for(int scale:new int[]{60,100,150})for(int width:new int[]{320,427,640,1280}){var b=SurvivalDock.bounds(width,240,180,148,corner,1000,1000,scale);if(b.x()<0||b.y()<0||b.x()+b.width()>width||b.y()+b.height()>240)throw new AssertionError("project HUD outside viewport");}check(true,"project corner, scale and large offsets stay inside every GUI viewport");
            config.projectCorner=dev.stow.StowConfig.DockCorner.BOTTOM_LEFT;config.projectOffsetX=37;config.projectOffsetY=42;config.projectScale=80;config.refillThreshold=4;config.handRefill=false;config.toolPick=false;config.buildingStock=false;config.save();var reload=FeatureTest.reloadConfig();
            check(reload.projectCorner==dev.stow.StowConfig.DockCorner.BOTTOM_LEFT&&reload.projectOffsetX==37&&reload.projectOffsetY==42&&reload.projectScale==80&&!reload.dockMaterials,"project placement and visibility survive config reload");
            check(reload.refillThreshold==4&&!reload.handRefill&&!reload.toolPick&&!reload.buildingStock,"building aid options survive config reload");
        }finally{Stow.config=new com.google.gson.Gson().fromJson(snapshot,dev.stow.StowConfig.class);Stow.config.save();}
    }
    static class FixturePlayer extends net.minecraft.client.player.LocalPlayer {
        boolean usingItem;
        @Override public boolean isUsingItem(){return usingItem;}
        FixturePlayer(){super(null,null,null,null,null,null,false,null,null);}
        @Override public net.minecraft.world.level.GameType gameMode(){return net.minecraft.world.level.GameType.SURVIVAL;}
    }
    static class FixtureLevel extends net.minecraft.client.multiplayer.ClientLevel {
        BlockState block;
        FixtureLevel(){super(null,null,net.minecraft.world.level.Level.OVERWORLD,null,0,0,null,false,0,0);}
        @Override public BlockState getBlockState(BlockPos pos){return block;}
    }
}
