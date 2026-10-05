package porttest;

import dev.stow.Stow;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.*;

/** Actual mining mixins against an integrated survival server and synced vanilla tool components. */
public final class AutoToolWorldTest implements ClientModInitializer {
    private boolean started;private volatile boolean prepared,verified;private volatile Throwable failure;
    private int ticks,worldTicks,phase,elapsed,index;
    private final List<String> cases=List.of("default off","stone pickaxe","held mining stone to dirt","log axe","cobweb shears","Sneak override","hotkey off");
    private final int[] selected={0,0,1,2,3,0,0};
    private final BlockPos target=new BlockPos(1,65,0),stone=new BlockPos(2,65,0);
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("AUTO TOOL WORLD PASS: "+s);}
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{try{tick(mc);}catch(Throwable e){fail(e);}});}
    private void toggle(Minecraft mc){mc.keyboardHandler.keyPress(mc.getWindow().handle(),1,new KeyEvent(InputConstants.KEY_T,116,InputConstants.MOD_ALT));}
    private void tick(Minecraft mc)throws Exception{
        if(++ticks>3000)throw new AssertionError("Auto tool world timeout");if(failure!=null)throw new AssertionError("Server validation",failure);
        if(!started&&mc.gui.overlay()==null&&mc.gui.screen()!=null){started=true;check(!Stow.config.autoTool,"default off on first real world startup");mc.options.renderDistance().set(2);mc.options.simulationDistance().set(2);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.createWorldOpenFlows().createFreshLevel("auto-tool-fixture",new LevelSettings("Auto tool fixture",GameType.SURVIVAL,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(1234,false,false),r->r.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.gui.screen());return;}
        if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.gui.overlay()!=null||++worldTicks<30)return;
        if(index==cases.size()){mc.options.keyShift.setDown(false);Files.writeString(Path.of("auto-tool-world-result.txt"),"PASS "+index+" client/server automatic mining cases\n");System.exit(0);}
        if(phase==0){mc.gui.setScreen(null);mc.gameMode.stopDestroyBlock();mc.options.keyShift.setDown(false);prepared=false;verified=false;phase=1;elapsed=0;
            mc.getSingleplayerServer().execute(()->{try{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.doCloseContainer();p.getInventory().clearContent();p.inventoryMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);p.teleportTo(0.5,65,0.5);p.level().setBlock(new BlockPos(0,64,0),Blocks.STONE.defaultBlockState(),3);
                var inv=p.getInventory();inv.setItem(0,new ItemStack(Items.DIAMOND_PICKAXE));inv.setItem(1,new ItemStack(Items.IRON_SHOVEL));inv.setItem(2,new ItemStack(Items.IRON_AXE));inv.setItem(3,new ItemStack(Items.SHEARS));inv.setItem(4,new ItemStack(Items.DIRT,32));inv.setItem(9,new ItemStack(Items.NETHERITE_PICKAXE));
                p.level().setBlock(target,(index==1?Blocks.STONE:index==3?Blocks.OAK_LOG:index==4?Blocks.COBWEB:Blocks.DIRT).defaultBlockState(),3);p.level().setBlock(stone,Blocks.STONE.defaultBlockState(),3);p.inventoryMenu.broadcastChanges();prepared=true;
            }catch(Throwable e){failure=e;}});return;}
        if(phase==1){if(!prepared||++elapsed<30)return;mc.gui.setScreen(null);
            check(mc.player.getInventory().getItem(1).is(Items.IRON_SHOVEL),"server supplied vanilla tools: "+cases.get(index));
            if(index==1){toggle(mc);check(Stow.config.autoTool,"Alt+T enables automatic mining in game");}
            if(index==6){toggle(mc);check(!Stow.config.autoTool,"Alt+T disables automatic mining in game");}
            mc.player.getInventory().setSelectedSlot(index==0||index>=5?0:4);if(index==5)mc.options.keyShift.setDown(true);
            if(index==2){mc.gameMode.startDestroyBlock(stone,Direction.UP);check(mc.player.getInventory().getSelectedSlot()==0,"startDestroyBlock selects stone pickaxe");mc.gameMode.continueDestroyBlock(target,Direction.UP);}
            else mc.gameMode.startDestroyBlock(target,Direction.UP);
            check(mc.player.getInventory().getSelectedSlot()==selected[index],"actual mining entrypoint selects expected tool: "+cases.get(index));
            phase=2;elapsed=0;return;}
        if(phase==2){if(index>=1&&index<=4&& !mc.level.getBlockState(target).isAir())mc.gameMode.continueDestroyBlock(target,Direction.UP);
            if(++elapsed<40)return;phase=3;
            mc.getSingleplayerServer().execute(()->{try{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();check(p.getInventory().getSelectedSlot()==selected[index],"server received selected tool: "+cases.get(index));
                if(index>=1&&index<=4){check(p.level().getBlockState(target).isAir(),"server confirms block broken: "+cases.get(index));check(p.getInventory().getItem(selected[index]).getDamageValue()>0,"server damaged the correct tool: "+cases.get(index));}
                for(int i=0;i<5;i++)check(p.getInventory().getItem(i).is(new Item[]{Items.DIAMOND_PICKAXE,Items.IRON_SHOVEL,Items.IRON_AXE,Items.SHEARS,Items.DIRT}[i]),"server tool slot preserved "+i);
                check(p.getInventory().getItem(9).is(Items.NETHERITE_PICKAXE),"backpack tool remains untouched");verified=true;
            }catch(Throwable e){failure=e;}});return;}
        if(phase==3&&verified){index++;phase=0;}
    }
    private static void fail(Throwable e){e.printStackTrace();try{var out=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(out));Files.writeString(Path.of("auto-tool-world-result.txt"),"FAIL "+out);}catch(Exception ignored){}System.exit(1);}
}
