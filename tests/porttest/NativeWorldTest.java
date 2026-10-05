package porttest;

import java.nio.file.*;
import java.util.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.*;
import net.minecraft.world.level.block.*;
import dev.stow.client.memory.*;
import static dev.stow.client.memory.ChestMemoryStore.*;

/** Optional real integrated-world fixture for spectral outlines above an opaque wall. */
public final class NativeWorldTest implements ClientModInitializer {
    private boolean started,placed;private int ticks,worldTicks;private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;System.out.println("NATIVE WORLD PASS: "+message);}
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(mc->{try{
        if(++ticks>2400)throw new AssertionError("World fixture timed out");
        if(!started&&mc.gui.overlay()==null&&mc.gui.screen()!=null){started=true;mc.options.renderDistance().set(2);mc.options.simulationDistance().set(2);mc.options.guiScale().set(2);mc.options.entityShadows().set(false);
            mc.createWorldOpenFlows().createFreshLevel("outline-fixture",new LevelSettings("Outline fixture",GameType.CREATIVE,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(1234,false,false),registries->registries.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),mc.gui.screen());return;
        }
        if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null||mc.gui.overlay()!=null)return;
        if(!placed){placed=true;mc.gui.setScreen(null);mc.getSingleplayerServer().execute(()->{var level=mc.getSingleplayerServer().overworld();for(int x=-5;x<=5;x++)for(int z=-4;z<=8;z++)level.setBlock(new BlockPos(x,65,z),Blocks.STONE.defaultBlockState(),3);for(int x=-2;x<=2;x++)for(int y=66;y<=70;y++)level.setBlock(new BlockPos(x,y,2),Blocks.STONE.defaultBlockState(),3);level.setBlock(new BlockPos(0,66,5),Blocks.CHEST.defaultBlockState(),3);});mc.player.connection.sendCommand("tp @s 0.5 66 -2 0 0");return;}
        ++worldTicks;if(worldTicks<30)return;
        if(worldTicks==30){mc.gui.setScreen(null);mc.player.connection.sendCommand("tp @s 0.5 66 -2 0 0");}
        if(worldTicks==60)capture(mc,"native-wall-before.png");
        if(worldTicks==75){var store=ChestMemory.currentStore();var chest=new Location("minecraft:overworld",0,66,5,"Chest");store.remember(new SavedChest(chest,"Spectral chest",System.currentTimeMillis(),List.of()));ChestMemory.select(chest);}
        if(worldTicks==100){var frame=mc.gameRenderer.gameRenderState().levelRenderState;check(NativeChestOutline.ready(frame),"selected chest is extracted behind the solid wall");var f=net.minecraft.client.renderer.LevelRenderer.class.getDeclaredField("currentFrameRendersEntityOutline");f.setAccessible(true);check(f.getBoolean(mc.levelRenderer),"vanilla spectral/entity outline pass rendered the selected chest");capture(mc,"native-wall-glow.png");}
        if(worldTicks==125)ChestMemory.setGlowEnabled(false);
        if(worldTicks==140){check(!NativeChestOutline.ready(mc.gameRenderer.gameRenderState().levelRenderState),"quick-off removes native outline submissions");capture(mc,"native-wall-off.png");}
        if(worldTicks==165){Files.writeString(Path.of("native-world-result.txt"),"PASS "+checks+" integrated-world checks\n");System.exit(0);}
    }catch(Throwable e){e.printStackTrace();try{var writer=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(writer));Files.writeString(Path.of("native-world-result.txt"),"FAIL "+writer);}catch(Exception ignored){}System.exit(1);}});}
    private static void capture(Minecraft mc,String name){net.minecraft.client.Screenshot.takeScreenshot(mc.gameRenderer.mainRenderTarget(),image->{try{image.writeToFile(Path.of(name));image.close();}catch(Exception e){throw new RuntimeException(e);}});}
}
