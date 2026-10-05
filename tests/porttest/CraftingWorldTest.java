package porttest;

import dev.stow.Stow;
import dev.stow.client.inventory.PinnedSlots;
import dev.stow.client.inventory.PinnedTransfer;
import java.nio.file.*;
import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.input.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.*;

/** Actual client clicks, server recipes and synchronization, with no synthetic result slots. */
public final class CraftingWorldTest implements ClientModInitializer {
    private record Case(String name,boolean table,boolean protect,boolean pins,int space,int expected,int remaining,boolean close,boolean interrupt){}
    private final List<Case> cases=List.of(
        new Case("inventory uses vanilla bulk crafting with pin protection on",false,true,true,-1,256,0,false,false),
        new Case("table uses vanilla bulk crafting with pin protection on",true,true,true,-1,256,0,false,false),
        new Case("inventory with protection off matches the vanilla destination slots",false,false,true,-1,256,0,false,false),
        new Case("table with protection off matches the vanilla destination slots",true,false,true,-1,256,0,false,false),
        new Case("no pins retains vanilla bulk crafting",false,true,false,-1,256,0,false,false),
        new Case("nearly full inventory lets vanilla use pinned crafting destinations",false,true,true,4,40,0,false,false),
        new Case("small partial stack retains vanilla crafting allocation",false,true,true,2,40,0,false,false),
        new Case("multiple partial stacks retain vanilla crafting allocation",false,true,true,3,40,0,false,false),
        new Case("a completely full inventory leaves crafting ingredients intact",false,true,true,0,0,10,false,false)
    );
    private List<ItemStack> inventoryReference,tableReference;
    private boolean started,initialized;private int ticks,worldTicks,index,phase,elapsed,checks;
    private volatile boolean prepared,verified;private volatile Throwable serverFailure;
    private AbstractCraftingMenu clickedMenu;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);System.out.println("CRAFTING PASS: "+message);}
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{try{tick(mc);}catch(Throwable e){fail(e);}});}
    private void tick(Minecraft mc)throws Exception{
        if(++ticks>4000)throw new AssertionError("Crafting world timed out");
        if(serverFailure!=null)throw new AssertionError("Server validation",serverFailure);
        if(!started&&mc.gui.overlay()==null&&mc.gui.screen()!=null){started=true;mc.options.renderDistance().set(2);mc.options.simulationDistance().set(2);mc.options.guiScale().set(2);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.createWorldOpenFlows().createFreshLevel("crafting-fixture",new LevelSettings("Crafting fixture",GameType.SURVIVAL,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(1234,false,false),r->r.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.gui.screen());return;}
        if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.gui.overlay()!=null)return;
        if(++worldTicks<30)return;
        if(!initialized){initialized=true;mc.gui.setScreen(null);mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.teleportTo(0.5,65,0.5);p.level().setBlock(new BlockPos(0,64,0),Blocks.STONE.defaultBlockState(),3);p.level().setBlock(new BlockPos(1,65,0),Blocks.CRAFTING_TABLE.defaultBlockState(),3);});return;}
        if(index==cases.size()){Files.writeString(Path.of("crafting-world-result.txt"),"PASS "+checks+" client/server crafting cases\n");System.exit(0);}
        Case c=cases.get(index);
        if(phase==0){
            if(mc.gui.screen() instanceof AbstractContainerScreen<?>)mc.gui.screen().onClose();else mc.gui.setScreen(null);phase=10;elapsed=0;return;
        }
        if(phase==10){
            if(++elapsed<20)return; // Let closing the previous menu return its inputs before resetting the fixture.
            for(int i=0;i<36;i++){var slot=new Slot(mc.player.getInventory(),i,0,0);if(PinnedSlots.isPinned(slot))PinnedSlots.toggle(slot);}
            if(c.pins){PinnedSlots.toggle(new Slot(mc.player.getInventory(),8,0,0));PinnedSlots.toggle(new Slot(mc.player.getInventory(),7,0,0));}
            Stow.config.pinProtectTransfers=c.protect;prepared=false;verified=false;elapsed=0;phase=1;
            mc.getSingleplayerServer().execute(()->{try{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.doCloseContainer();p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.inventoryMenu.setCarried(ItemStack.EMPTY);
                if(c.space>=0){for(int i=0;i<36;i++)if(i!=7&&i!=8)p.getInventory().setItem(i,new ItemStack(Items.DIRT,64));p.getInventory().setItem(6,new ItemStack(Items.OAK_PLANKS,64-(c.space==3?1:c.space)));if(c.space==3)p.getInventory().setItem(5,new ItemStack(Items.OAK_PLANKS,61));}
                if(c.pins)p.getInventory().setItem(7,new ItemStack(c.table?Items.STICK:Items.OAK_PLANKS,c.space==0?64:17));
                if(c.space==0)p.getInventory().setItem(8,new ItemStack(Items.DIRT,64));
                if(c.table)p.openMenu(new SimpleMenuProvider((id,inv,player)->new CraftingMenu(id,inv,ContainerLevelAccess.create(p.level(),new BlockPos(1,65,0))),Component.literal("Crafting")));
                var menu=(AbstractCraftingMenu)p.containerMenu;menu.getInputGridSlots().forEach(s->s.set(ItemStack.EMPTY));
                menu.getInputGridSlots().get(0).set(new ItemStack(c.table?Items.OAK_PLANKS:Items.OAK_LOG,c.space>=0?10:64));
                if(c.table)menu.getInputGridSlots().get(3).set(new ItemStack(Items.OAK_PLANKS,64));
                menu.broadcastChanges();p.inventoryMenu.broadcastChanges();prepared=true;
            }catch(Throwable e){serverFailure=e;}});return;
        }
        if(phase==1){
            if(!prepared||++elapsed<20)return;
            if(!(mc.player.containerMenu instanceof AbstractCraftingMenu menu)||c.table!=(menu instanceof CraftingMenu)||!menu.getResultSlot().hasItem())return;
            if(!c.table)mc.gui.setScreen(new InventoryScreen(mc.player));
            if(!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen))return;
            clickedMenu=menu;var slot=menu.getResultSlot();int x=(int)field(screen,"leftPos")+slot.x+8,y=(int)field(screen,"topPos")+slot.y+8;
            check(!PinnedTransfer.move(menu,slot,mc.player,(id,b)->{throw new AssertionError("Crafting was intercepted with a pickup");}),c.name+" (protection bypass)");
            var event=new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,InputConstants.MOD_SHIFT));screen.mouseClicked(event,false);screen.mouseReleased(event);
            System.out.println("CRAFTING CLICK: "+c.name+" at "+x+","+y+" menu="+menu.containerId+" screen="+screen.getClass().getSimpleName()+" result="+slot.getItem()+" inputs="+menu.getInputGridSlots().stream().map(Slot::getItem).toList()+" pins="+mc.player.getInventory().getItem(7)+", "+mc.player.getInventory().getItem(8));
            if(c.close){check(mc.player.getInventory().getItem(8).isEmpty()&&mc.player.getInventory().getItem(7).getCount()==17,c.name+" (pins before vanilla returns inputs)");screen.onClose();}
            if(c.interrupt)mc.gameMode.handleContainerInput(menu.containerId,menu.getInputGridSlots().getFirst().index,0,ContainerInput.PICKUP,mc.player);
            phase=2;elapsed=0;return;
        }
        if(phase==2){
            if(++elapsed<30)return;
            Item output=c.table?Items.STICK:Items.OAK_PLANKS;int baseline=c.pins?(c.space==0?64:17):0;if(c.space>=0)baseline+=c.space==3?124:64-c.space;
            System.out.println("CRAFTING STATE: actual="+count(mc.player.getInventory(),output)+" expected="+(baseline+c.expected)+" player menu="+mc.player.containerMenu.containerId+" screen="+mc.gui.screen()+" result="+clickedMenu.getResultSlot().getItem()+" inputs="+clickedMenu.getInputGridSlots().stream().map(Slot::getItem).toList()+" pins="+mc.player.getInventory().getItem(7)+", "+mc.player.getInventory().getItem(8));
            check(count(mc.player.getInventory(),output)==baseline+c.expected,c.name+" (client count "+count(mc.player.getInventory(),output)+" / "+(baseline+c.expected)+")");
            check(c.interrupt||clickedMenu.getCarried().isEmpty(),c.name+" (empty cursor)");
            var contents=new ArrayList<ItemStack>();for(int i=0;i<36;i++)contents.add(mc.player.getInventory().getItem(i).copy());
            if(c.space<0&&c.pins){
                if(c.protect){if(c.table)tableReference=contents;else inventoryReference=contents;}
                else{var reference=c.table?tableReference:inventoryReference;for(int i=0;i<36;i++)check(ItemStack.matches(reference.get(i),contents.get(i)),c.name+" (slot "+i+")");}
            }
            if(c.space==0)check(mc.player.getInventory().getItem(7).getCount()==64&&mc.player.getInventory().getItem(8).is(Items.DIRT),c.name+" (full slots unchanged)");
            if(!c.close)check(clickedMenu.getInputGridSlots().getFirst().getItem().getCount()==c.remaining,c.name+" (ingredients)");
            int expectedCount=baseline+c.expected;
            phase=3;mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();check(count(p.getInventory(),output)==expectedCount,c.name+" (authoritative server count)");check(c.interrupt||p.containerMenu.getCarried().isEmpty(),c.name+" (server cursor)");verified=true;}catch(Throwable e){serverFailure=e;}});return;
        }
        if(phase==3&&verified){checks++;index++;phase=0;}
    }
    private static int count(net.minecraft.world.entity.player.Inventory inventory,Item item){int count=0;for(int i=0;i<36;i++)if(inventory.getItem(i).is(item))count+=inventory.getItem(i).getCount();return count;}
    private static Object field(Object screen,String name)throws Exception{var f=AbstractContainerScreen.class.getDeclaredField(name);f.setAccessible(true);return f.get(screen);}
    private static void fail(Throwable e){e.printStackTrace();try{var out=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(out));Files.writeString(Path.of("crafting-world-result.txt"),"FAIL "+out);}catch(Exception ignored){}System.exit(1);}
}
