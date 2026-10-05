package porttest;

import dev.stow.*;
import dev.stow.client.inventory.*;
import dev.stow.client.memory.*;
import dev.stow.client.hud.*;
import java.util.*;
import java.nio.file.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.SimpleContainer;
import static dev.stow.client.memory.ChestMemoryStore.*;

final class PolishTests {
    private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;System.out.println("POLISH PASS: "+message);}
    static int run(Minecraft mc,FeatureTest.TestScreen parent)throws Exception{
        var oldScreen=mc.gui.screen();var oldPlayer=mc.player;String config=new com.google.gson.Gson().toJson(Stow.config);
        var f=ChestMemory.class.getDeclaredField("store");f.setAccessible(true);var oldStore=f.get(null);
        int left=(int)field(AbstractContainerScreen.class,parent,"leftPos");
        try{
            // Recipe book changes leftPos after the base container's init, without rebuilding our widgets.
            mc.gui.setScreen(parent);left=(int)field(AbstractContainerScreen.class,parent,"leftPos");
            var dir=Files.createTempDirectory("stow-polish-");var store=new ChestMemoryStore(dir,"polish");store.setGoal("minecraft:cobblestone",1000);FeatureTest.setMemoryField("store",store);
            for(int shift:new int[]{0,77,0,77}){
                FeatureTest.setField(AbstractContainerScreen.class,parent,"leftPos",left+shift);
                parent.extractContents(new GuiGraphicsExtractor(mc,new GuiRenderState(),parent.width,parent.height),-100,-100,0);
                var material=(FloatingMaterialTracker)parent.children().stream().filter(FloatingMaterialTracker.class::isInstance).findFirst().orElseThrow();
                var eye=FeatureTest.button(parent,"Toggle chest glow");var palette=FeatureTest.button(parent,"Quick actions");
                check(material.getX()==parent.inventoryRight()+8&&eye.getX()+eye.getWidth()==parent.inventoryRight()&&palette.getX()==left+shift+88,"recipe-book position change moves counters and buttons; eye stays at inventory right edge");
            }
            FeatureTest.setField(AbstractContainerScreen.class,parent,"leftPos",left);
            for(int i=0;i<16;i++){var location=new Location("minecraft:overworld",i*3,64,0,"Chest");store.remember(new SavedChest(location,"Chest "+i,0,List.of(new MemoryItem("minecraft:cobblestone","Cobblestone",64))));}
            store.selectAllChests(false);mc.gui.setScreen(new ChestMemoryScreen(parent,store,"",true,null));var choice=(ChestMemoryScreen)mc.gui.screen();choice.init(320,240);
            check((int)field(ChestMemoryScreen.class,choice,"pageSize")>=4,"compact project selection shows at least four chest cards at the smallest GUI viewport");
            var before=new ArrayList<>((List<SearchResult>)field(ChestMemoryScreen.class,choice,"results"));var plus=FeatureTest.button(choice,"+");int x=plus.getX(),y=plus.getY();FeatureTest.press(plus);
            check(((List<SearchResult>)field(ChestMemoryScreen.class,choice,"results")).equals(before),"selecting a project chest preserves the entire result order");var selected=FeatureTest.button(choice,"x");check(selected.getX()==x&&selected.getY()==y,"selected chest stays in the same visible card position");
            // Ordinary pickup transactions exercise the protected transfer instead of sending server QUICK_MOVE.
            var uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);var unsafe=(sun.misc.Unsafe)uf.get(null);
            var player=(net.minecraft.client.player.RemotePlayer)unsafe.allocateInstance(net.minecraft.client.player.RemotePlayer.class);
            var level=(net.minecraft.client.multiplayer.ClientLevel)unsafe.allocateInstance(net.minecraft.client.multiplayer.ClientLevel.class);var listener=(net.minecraft.client.multiplayer.ClientPacketListener)unsafe.allocateInstance(net.minecraft.client.multiplayer.ClientPacketListener.class);
            FeatureTest.setField(net.minecraft.client.multiplayer.ClientPacketListener.class,listener,"enabledFeatures",net.minecraft.world.flag.FeatureFlags.VANILLA_SET);FeatureTest.setField(net.minecraft.client.multiplayer.ClientLevel.class,level,"connection",listener);FeatureTest.setField(net.minecraft.world.entity.Entity.class,player,"level",level);
            var inventory=new Inventory(player,new EntityEquipment());FeatureTest.setField(Player.class,player,"inventory",inventory);FeatureTest.setField(Player.class,player,"abilities",new Abilities());var chest=new SimpleContainer(27);var menu=ChestMenu.threeRows(41,inventory,chest);player.containerMenu=menu;
            var pinned=menu.slots.stream().filter(slot->slot.container==inventory&&slot.getContainerSlot()==8).findFirst().orElseThrow();PinnedSlots.toggle(pinned);
            try{
                Stow.config.pinProtectTransfers=true;chest.setItem(0,new ItemStack(Items.COBBLESTONE,64));var totals=FeatureTest.contents(menu);
                check(PinnedTransfer.move(menu,menu.slots.getFirst(),player,(id,b)->menu.clicked(id,b,ContainerInput.PICKUP,player))&&pinned.getItem().isEmpty()&&inventory.getItem(7).getCount()==64&&FeatureTest.contents(menu).equals(totals)&&menu.getCarried().isEmpty(),"Shift transfer skips empty pinned destinations and preserves all item counts");
                pinned.set(new ItemStack(Items.COBBLESTONE,32));chest.setItem(0,new ItemStack(Items.COBBLESTONE,12));PinnedTransfer.move(menu,menu.slots.getFirst(),player,(id,b)->menu.clicked(id,b,ContainerInput.PICKUP,player));check(pinned.getItem().getCount()==32,"Shift transfer does not merge into an occupied pinned destination");
                inventory.clearContent();for(int i=0;i<36;i++)if(i!=8)inventory.setItem(i,new ItemStack(Items.DIRT,64));chest.setItem(0,new ItemStack(Items.COBBLESTONE,12));check(PinnedTransfer.move(menu,menu.slots.getFirst(),player,(id,b)->menu.clicked(id,b,ContainerInput.PICKUP,player))&&chest.getItem(0).getCount()==12&&pinned.getItem().isEmpty(),"only pinned free space leaves the Shift-clicked stack in its source");
                Stow.config.pinProtectTransfers=false;check(!PinnedTransfer.move(menu,menu.slots.getFirst(),player,(id,b)->{throw new AssertionError("disabled transfer moved items");}),"turning protection off preserves vanilla quick transfer handling");
            }finally{PinnedSlots.toggle(pinned);}
            var progress=new MaterialPlanner.Progress(200,300,1000,1);Stow.config.materialsIncludeInventory=true;check(progress.counted()==500&&progress.label().equals("500 / 1000")&&progress.missing()==500,"inventory inclusion counts carried items plus storage");
            Stow.config.materialsIncludeInventory=false;check(progress.counted()==300&&progress.label().equals("300 / 1000")&&progress.missing()==700,"disabling inventory inclusion uses only chest totals with the same display");
            var stocked=new MaterialPlanner.Progress(800,200,1000,1);check(!stocked.enough()&&stocked.color()==0xFFFF967D,"chest-only completion and colors ignore carried inventory");Stow.config.materialsIncludeInventory=true;check(stocked.enough()&&stocked.color()==0xFF8DE8B2,"re-enabling inventory inclusion updates completion and colors");Stow.config.materialsIncludeInventory=false;
            Stow.config.pinStyle=StowConfig.PinStyle.HIGHLIGHT;Stow.config.pinProtectTransfers=false;Stow.config.equipmentAll=false;Stow.config.save();var loaded=FeatureTest.reloadConfig();check(!loaded.materialsIncludeInventory&&loaded.pinStyle==StowConfig.PinStyle.HIGHLIGHT&&!loaded.pinProtectTransfers&&!loaded.equipmentAll,"inventory inclusion, pin appearance, transfer protection and all-equipment HUD options survive reload");
        }finally{FeatureTest.setField(AbstractContainerScreen.class,parent,"leftPos",left);FeatureTest.setMemoryField("store",oldStore);Stow.config=new com.google.gson.Gson().fromJson(config,StowConfig.class);Stow.config.save();mc.player=oldPlayer;mc.gui.setScreen(oldScreen);}
        return checks;
    }
    private static Object field(Class<?> type,Object object,String name)throws Exception{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}
}
