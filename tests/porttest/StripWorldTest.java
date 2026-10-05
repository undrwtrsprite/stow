package porttest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stow.Stow;
import dev.stow.client.input.StowShortcuts;
import dev.stow.client.inventory.BulkStrip;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.*;
import net.minecraft.world.phys.*;

/** Real vanilla axe use, server confirmations/rejections, reach, occlusion and cancellation. */
public final class StripWorldTest implements ClientModInitializer {
    public static volatile boolean reject;
    public static volatile int requests;
    private static int checks;
    private boolean started;
    private volatile boolean prepared,verified;
    private volatile Throwable failure;
    private int ticks,worldTicks,phase,elapsed,index;
    private final List<String> cases=List.of("batch","same shortcut cancels","Sneak unchanged","tool change","last durability","open menu","disabled","server rejection","move out of reach","wrong tool","wrong target");
    private final BlockPos first=new BlockPos(1,65,0),second=new BlockPos(1,66,2),third=new BlockPos(1,65,-2);
    private final BlockPos hidden=new BlockPos(3,65,0),far=new BlockPos(10,65,0),birch=new BlockPos(0,65,2);
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);checks++;System.out.println("STRIP WORLD PASS: "+label);}
    private void shortcut(Minecraft mc,int action){mc.keyboardHandler.keyPress(mc.getWindow().handle(),action,new KeyEvent(InputConstants.KEY_S,115,InputConstants.MOD_ALT));}
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{try{tick(mc);}catch(Throwable error){fail(error);}});}
    private void tick(Minecraft mc)throws Exception{
        if(++ticks>3000)throw new AssertionError("Stripping world timeout");
        if(failure!=null)throw new AssertionError("Server validation",failure);
        if(!started&&mc.gui.overlay()==null&&mc.gui.screen()!=null){
            started=true;mc.options.renderDistance().set(2);mc.options.simulationDistance().set(2);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            var binding=StowShortcuts.binding(StowShortcuts.Action.BULK_STRIP);
            check(binding.code()==InputConstants.KEY_S&&binding.modifiers()==InputConstants.MOD_ALT,"default shortcut is Alt+S, without Sneak");
            mc.createWorldOpenFlows().createFreshLevel("strip-fixture",new LevelSettings("Strip fixture",GameType.SURVIVAL,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(1234,false,false),r->r.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.gui.screen());return;
        }
        if(mc.player==null||mc.level==null||mc.getSingleplayerServer()==null||mc.gui.overlay()!=null||++worldTicks<30)return;
        if(index==cases.size()){
            mc.options.keyShift.setDown(false);reject=false;BulkStrip.cancel();
            Files.writeString(Path.of("strip-world-result.txt"),"PASS "+index+" client/server oak-stripping cases, "+checks+" checks\n");System.exit(0);
        }
        if(phase==0){
            BulkStrip.cancel();mc.gui.setScreen(null);mc.gameMode.stopDestroyBlock();mc.options.keyShift.setDown(false);mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);Stow.config.bulkStrip=true;
            prepared=false;verified=false;phase=1;elapsed=0;
            mc.getSingleplayerServer().execute(()->{try{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();p.doCloseContainer();p.getInventory().clearContent();p.inventoryMenu.setCarried(ItemStack.EMPTY);p.getInventory().setSelectedSlot(0);p.teleportTo(0.5,65,0.5);
                var level=p.level();level.setBlock(new BlockPos(0,64,0),Blocks.STONE.defaultBlockState(),3);
                for(int y=64;y<=69;y++)for(int z=-1;z<=1;z++)level.setBlock(new BlockPos(2,y,z),Blocks.STONE.defaultBlockState(),3);
                for(var pos:List.of(first,third,hidden,far))level.setBlock(pos,Blocks.OAK_LOG.defaultBlockState(),3);
                level.setBlock(second,Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X),3);
                level.setBlock(birch,Blocks.BIRCH_LOG.defaultBlockState(),3);
                var axe=new ItemStack(Items.IRON_AXE);if(index==4)axe.setDamageValue(axe.getMaxDamage()-1);
                p.getInventory().setItem(0,axe);p.getInventory().setItem(1,new ItemStack(Items.COBBLESTONE,16));p.getInventory().setItem(9,new ItemStack(Items.DIAMOND_AXE));
                p.inventoryMenu.broadcastChanges();requests=0;reject=index==7;prepared=true;
            }catch(Throwable error){failure=error;}});return;
        }
        if(phase==1){
            if(!prepared||++elapsed<30)return;
            mc.gui.setScreen(null);mc.player.getInventory().setSelectedSlot(index==9?1:0);
            check(mc.player.getInventory().getItem(0).is(Items.IRON_AXE),"server supplied vanilla axe: "+cases.get(index));
            if(index==2)mc.options.keyShift.setDown(true);
            if(index==6)Stow.config.bulkStrip=false;
            var aimed=index==10?birch:first;mc.hitResult=new BlockHitResult(Vec3.atCenterOf(aimed),Direction.WEST,aimed,false);
            shortcut(mc,1);shortcut(mc,0);
            if(index==4||index==6||index==9||index==10)check(!BulkStrip.busy(),"invalid start produces no batch: "+cases.get(index));
            else{
                check(BulkStrip.busy(),"real keyboard shortcut starts batch: "+cases.get(index));
                shortcut(mc,2);check(BulkStrip.busy(),"key repeat does not cancel or restart batch: "+cases.get(index));
                if(index==1){shortcut(mc,1);shortcut(mc,0);check(!BulkStrip.busy(),"second shortcut press cancels before sending any use");}
                if(index==3)mc.player.getInventory().setSelectedSlot(1);
                if(index==5)mc.gui.setScreen(new Screen(Component.literal("Open menu")){});
                if(index==8)mc.player.setPos(25.5,65,0.5);
            }
            phase=2;elapsed=0;return;
        }
        if(phase==2){
            if(++elapsed>180)throw new AssertionError("Batch failed to stop: "+cases.get(index));
            if(elapsed<40||BulkStrip.busy())return;
            if(index==2)check(mc.options.keyShift.isDown(),"stripping never changes held Sneak state");
            phase=3;
            mc.getSingleplayerServer().execute(()->{try{
                var p=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();boolean stripped=index==0||index==2;
                for(var pos:List.of(first,second,third))check(p.level().getBlockState(pos).is(stripped?Blocks.STRIPPED_OAK_LOG:Blocks.OAK_LOG),"server confirms expected log state at "+pos+": "+cases.get(index));
                check(p.level().getBlockState(second).getValue(RotatedPillarBlock.AXIS)==Direction.Axis.X,"log orientation preserved: "+cases.get(index));
                check(p.level().getBlockState(hidden).is(Blocks.OAK_LOG),"wall protects occluded oak log: "+cases.get(index));
                check(p.level().getBlockState(far).is(Blocks.OAK_LOG),"out-of-reach oak log unchanged: "+cases.get(index));
                check(p.level().getBlockState(birch).is(Blocks.BIRCH_LOG),"birch log unchanged: "+cases.get(index));
                var axe=p.getInventory().getItem(0);check(axe.is(Items.IRON_AXE),"axe stays in its original slot: "+cases.get(index));
                check(axe.getDamageValue()==(index==4?axe.getMaxDamage()-1:stripped?3:0),"normal durability, no block-breaking or duplicate uses: "+cases.get(index));
                check(p.getInventory().getItem(1).is(Items.COBBLESTONE)&&p.getInventory().getItem(1).getCount()==16&&p.getInventory().getItem(9).is(Items.DIAMOND_AXE),"inventory and backpack axe unchanged: "+cases.get(index));
                check(requests==(stripped?3:index==7?1:0),"one request per stripped log, no retries on rejection: "+cases.get(index));verified=true;
            }catch(Throwable error){failure=error;}});return;
        }
        if(phase==3&&verified){
            if(index==7)check(mc.level.getBlockState(first).is(Blocks.OAK_LOG),"rejected prediction reconciles with authoritative server state");
            index++;phase=0;
        }
    }
    private static void fail(Throwable error){error.printStackTrace();try{var out=new java.io.StringWriter();error.printStackTrace(new java.io.PrintWriter(out));Files.writeString(Path.of("strip-world-result.txt"),"FAIL "+out);}catch(Exception ignored){}System.exit(1);}
}
