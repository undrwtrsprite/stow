package porttest;

import dev.stow.Stow;
import dev.stow.client.inventory.PinnedSlots;
import dev.stow.client.memory.*;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.input.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.*;

/** Real inventory gestures with an integrated survival server, plus material glow toggling. */
public final class BundleWorldTest implements ClientModInitializer {
    private boolean started,prepared,verified;private int ticks,worldTicks,phase,elapsed,index;
    private volatile Throwable failure;
    private final List<String> cases=List.of("placed bundle fast drag","held bundle fast filling","held bundle fast unpacking","full held bundle","held filling skips pins","held unpacking skips pins","material list glow toggle");
    private static ItemStack bundle(ItemStack... items){var b=new ItemStack(Items.BUNDLE);b.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(Arrays.stream(items).map(ItemStackTemplate::fromNonEmptyStack).toList()));return b;}
    private static ItemStack named(){var s=new ItemStack(Items.COBBLESTONE,10);s.set(DataComponents.CUSTOM_NAME,Component.literal("Named stack"));return s;}
    private static int inside(ItemStack b,Item i){var c=b.get(DataComponents.BUNDLE_CONTENTS);return c==null?0:c.itemCopies().filter(s->s.is(i)).mapToInt(ItemStack::getCount).sum();}
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("BUNDLE WORLD PASS: "+s);}
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{try{tick(mc);}catch(Throwable e){fail(e);}});}
    private void tick(Minecraft mc)throws Exception{
        if(++ticks>3000)throw new AssertionError("Bundle world timeout");if(failure!=null)throw new AssertionError("Server validation",failure);
        if(!started&&mc.gui.overlay()==null&&mc.gui.screen()!=null){started=true;mc.options.renderDistance().set(2);mc.options.simulationDistance().set(2);mc.options.guiScale().set(2);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.createWorldOpenFlows().createFreshLevel("bundle-fixture",new LevelSettings("Bundle fixture",GameType.SURVIVAL,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(1234,false,false),r->r.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.gui.screen());return;}
        if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.gui.overlay()!=null||++worldTicks<30)return;
        if(index==cases.size()){Files.writeString(Path.of("bundle-world-result.txt"),"PASS "+index+" client/server bundle and glow cases\n");System.exit(0);}
        if(phase==0){if(mc.gui.screen() instanceof AbstractContainerScreen<?>)mc.gui.screen().onClose();else mc.gui.setScreen(null);phase=10;elapsed=0;return;}
        if(phase==10){
            if(++elapsed<20)return;
            for(int i=0;i<36;i++){var s=new Slot(mc.player.getInventory(),i,0,0);if(PinnedSlots.isPinned(s))PinnedSlots.toggle(s);}
            if(index==4||index==5)PinnedSlots.toggle(new Slot(mc.player.getInventory(),10,0,0));
            Stow.config.bundleDrag=true;prepared=false;verified=false;phase=1;elapsed=0;
            mc.getSingleplayerServer().execute(()->{try{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.doCloseContainer();p.getInventory().clearContent();p.inventoryMenu.getCraftSlots().clearContent();p.inventoryMenu.setCarried(ItemStack.EMPTY);p.teleportTo(0.5,65,0.5);p.level().setBlock(new BlockPos(0,64,0),Blocks.STONE.defaultBlockState(),3);
                var inv=p.getInventory();
                if(index==0){var b=bundle(new ItemStack(Items.DIRT,12),named());BundleItem.toggleSelectedItem(b,1);inv.setItem(9,b);}
                if(index==1||index==4){inv.setItem(0,bundle());inv.setItem(9,new ItemStack(Items.DIRT,12));inv.setItem(10,named());inv.setItem(11,new ItemStack(Items.DIAMOND,6));}
                if(index==2||index==5)inv.setItem(0,bundle(new ItemStack(Items.DIRT,12),named(),new ItemStack(Items.DIAMOND,6)));
                if(index==3){inv.setItem(0,bundle(new ItemStack(Items.DIRT,64)));inv.setItem(9,named());}
                if(index==6)p.level().setBlock(new BlockPos(2,65,0),Blocks.CHEST.defaultBlockState(),3);
                p.inventoryMenu.broadcastChanges();prepared=true;
            }catch(Throwable e){failure=e;}});return;
        }
        if(phase==1){
            if(!prepared||++elapsed<20)return;var menu=mc.player.inventoryMenu;var inv=mc.player.getInventory();mc.gui.setScreen(new InventoryScreen(mc.player));var screen=(AbstractContainerScreen<?>)mc.gui.screen();
            if(index==6){
                var store=ChestMemory.currentStore();var loc=new ChestMemoryStore.Location(mc.level.dimension().identifier().toString(),2,65,0,"Chest");
                store.remember(new ChestMemoryStore.SavedChest(loc,"Fixture chest",System.currentTimeMillis(),List.of(new ChestMemoryStore.MemoryItem("minecraft:cobblestone","Cobblestone",10))));store.setGoal("minecraft:cobblestone",100);
                var tracker=new FloatingMaterialTracker(250,20,100,100,id->{});tracker.update(store,true);var right=new MouseButtonEvent(254,24,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT,0));
                tracker.onClick(right,false);check(ChestMemory.isMaterialGlowing("minecraft:cobblestone")&&ChestMemory.glowTarget()!=null,"material right-click enables native chest glow");
                tracker.onClick(right,false);check(ChestMemory.selected()==null&&ChestMemory.glowTarget()==null&&!ChestMemory.isMaterialGlowing("minecraft:cobblestone"),"second right-click disables the material glow");
                tracker.onClick(right,false);check(ChestMemory.glowTarget()!=null,"material glow can be enabled again");ChestMemory.setGlowEnabled(false);tracker.onClick(right,false);check(Stow.config.chestGlow&&ChestMemory.glowTarget()!=null,"material glow re-enables after the inventory eye turns it off");ChestMemory.stopGlow();index++;phase=0;return;
            }
            if(index!=0){var s=slot(menu,inv,0);mc.gameMode.handleContainerInput(menu.containerId,s.index,0,ContainerInput.PICKUP,mc.player);check(menu.getCarried().is(Items.BUNDLE),"bundle picked up onto cursor");}
            var from=event(screen,slot(menu,inv,9),index==2||index==5?InputConstants.MOUSE_BUTTON_RIGHT:InputConstants.MOUSE_BUTTON_LEFT);var to=event(screen,slot(menu,inv,index==5?12:11),from.button());
            screen.mouseClicked(from,false);screen.mouseDragged(to,to.x()-from.x(),to.y()-from.y());screen.mouseReleased(to);phase=2;elapsed=0;return;
        }
        if(phase==2){
            if(++elapsed<25)return;
            validate(mc.player.getInventory(),mc.player.inventoryMenu.getCarried(),"client");
            List<ItemStack> stacks=new ArrayList<>();for(int i=0;i<36;i++)stacks.add(mc.player.getInventory().getItem(i).copy());var cursor=mc.player.inventoryMenu.getCarried().copy();phase=3;
            mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();validate(p.getInventory(),p.inventoryMenu.getCarried(),"server");for(int i=0;i<36;i++)check(ItemStack.matches(stacks.get(i),p.getInventory().getItem(i)),"client/server slot "+i+" matches");check(ItemStack.matches(cursor,p.inventoryMenu.getCarried()),"client/server bundle components match");verified=true;}catch(Throwable e){failure=e;}});return;
        }
        if(phase==3&&verified){index++;phase=0;}
    }
    private void validate(net.minecraft.world.entity.player.Inventory inv,ItemStack cursor,String side){
        var source=inv.getItem(9);var middle=inv.getItem(10);var end=inv.getItem(11);
        System.out.println("BUNDLE STATE: "+side+" case="+index+" source="+source+" contents="+source.get(DataComponents.BUNDLE_CONTENTS)+" middle="+middle+" end="+end+" cursor="+cursor+" cursor contents="+cursor.get(DataComponents.BUNDLE_CONTENTS));
        if(index==0)check(source.is(Items.BUNDLE)&&source.get(DataComponents.BUNDLE_CONTENTS).isEmpty()&&ItemStack.matches(middle,named())&&end.is(Items.DIRT)&&end.getCount()==12&&cursor.isEmpty(),side+" placed bundle keeps selection, names and contents");
        if(index==1)check(source.isEmpty()&&middle.isEmpty()&&end.isEmpty()&&inside(cursor,Items.DIRT)==12&&inside(cursor,Items.COBBLESTONE)==10&&inside(cursor,Items.DIAMOND)==6,side+" held bundle fills all crossed stacks");
        if(index==2)check(!source.isEmpty()&&!middle.isEmpty()&&!end.isEmpty()&&cursor.is(Items.BUNDLE)&&cursor.get(DataComponents.BUNDLE_CONTENTS).isEmpty(),side+" held bundle unpacks all entries");
        if(index==3)check(ItemStack.matches(source,named())&&inside(cursor,Items.DIRT)==64,side+" full bundle leaves source items intact");
        if(index==4)check(source.isEmpty()&&ItemStack.matches(middle,named())&&end.isEmpty()&&inside(cursor,Items.COBBLESTONE)==0&&inside(cursor,Items.DIRT)==12&&inside(cursor,Items.DIAMOND)==6,side+" filling skips pinned stack");
        if(index==5)check(!source.isEmpty()&&middle.isEmpty()&&!end.isEmpty()&&!inv.getItem(12).isEmpty()&&cursor.get(DataComponents.BUNDLE_CONTENTS).isEmpty(),side+" unpacking skips pinned destination");
    }
    private static Slot slot(AbstractContainerMenu menu,net.minecraft.world.entity.player.Inventory inv,int id){return menu.slots.stream().filter(s->s.container==inv&&s.getContainerSlot()==id).findFirst().orElseThrow();}
    private static MouseButtonEvent event(AbstractContainerScreen<?> screen,Slot s,int button)throws Exception{var left=AbstractContainerScreen.class.getDeclaredField("leftPos");var top=AbstractContainerScreen.class.getDeclaredField("topPos");left.setAccessible(true);top.setAccessible(true);return new MouseButtonEvent(left.getInt(screen)+s.x+8,top.getInt(screen)+s.y+8,new MouseButtonInfo(button,InputConstants.MOD_SHIFT));}
    private static void fail(Throwable e){e.printStackTrace();try{var out=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(out));Files.writeString(Path.of("bundle-world-result.txt"),"FAIL "+out);}catch(Exception ignored){}System.exit(1);}
}
