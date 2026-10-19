package dev.stow.client.memory;
import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.*;
import dev.stow.companion.protocol.AreaClaimProtocol;
/** Detached local camera only. The player keeps normal server physics and sends neutral input. */
public final class AreaSelection {
 private static ArmorStand camera;private static Entity previous;private static LocalPlayer player;private static ClientInput input;private static CameraType perspective;private static AreaClaimScreen parent;private static Vec3 origin;private static final Set<Integer> keys=new HashSet<>();private static BlockPos first,second;
 public static boolean active(){return camera!=null;}
 public static void register(){HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("stow","area_selection"),(g,delta)->{
  if(!active())return;var mc=Minecraft.getInstance();g.textWithWordWrap(mc.font,Component.translatable("stow.area.fly-controls"),8,8,g.guiWidth()-16,0xFFFFFFFF);
  var hit=camera.pick(12,1,false);BlockPos hover=hit instanceof BlockHitResult b&&b.getType()!=HitResult.Type.MISS?b.getBlockPos():null;
  BlockPos end=second==null?hover:second;
  if(first!=null&&end!=null&&mc.gameRenderer.mainCamera().isInitialized()){var box=new AABB(first).minmax(new AABB(end));ChestGlow.drawProjected(g,new ChestGlow.Target(box,Component.translatable("stow.area.selection",AreaClaimProtocol.volume(first,end)).getString()),mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.pos,ChestGlow.worldProjection(mc.gameRenderer));}
  String text=first==null?Component.translatable("stow.area.first").getString():second==null?Component.translatable("stow.area.second").getString():Component.translatable("stow.area.review-hint").getString();g.text(mc.font,text,8,g.guiHeight()-28,0xFFFFD486);
 });}
 public static void start(AreaClaimScreen screen){var mc=Minecraft.getInstance();if(active()||mc.level==null||mc.player==null||!AreaClaims.available())return;parent=screen;first=null;second=null;keys.clear();player=mc.player;input=player.input;previous=mc.getCameraEntity();perspective=mc.options.getCameraType();origin=player.position();
  if(player.containerMenu!=player.inventoryMenu)player.closeContainer();mc.gui.setScreen(null);KeyMapping.releaseAll();player.input=new ClientInput();camera=new ArmorStand(mc.level,player.getX(),player.getEyeY(),player.getZ());int cameraId=-1;while(mc.level.getEntity(cameraId)!=null)cameraId--;camera.setId(cameraId);camera.setPos(player.getX(),player.getEyeY()-camera.getEyeHeight(),player.getZ());camera.setYRot(player.getYRot());camera.setXRot(player.getXRot());camera.setOldPosAndRot();mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(camera);
 }
 public static void stop(boolean back){if(!active())return;var mc=Minecraft.getInstance();camera=null;keys.clear();KeyMapping.releaseAll();if(player!=null)player.input=input;if(mc.player==player){mc.setCameraEntity(previous==null?player:previous);}else mc.setCameraEntity(mc.player);mc.options.setCameraType(perspective);var screen=parent;parent=null;previous=null;player=null;input=null;first=null;second=null;if(back&&screen!=null&&mc.level!=null)mc.gui.setScreen(screen);}
 public static void tick(Minecraft mc){if(!active())return;if(mc.level!=camera.level()||mc.player!=player||!player.isAlive()||mc.gui.screen()!=null||mc.getCameraEntity()!=camera||player.position().distanceToSqr(origin)>16){stop(false);return;}
  camera.setOldPosAndRot();double forward=(keys.contains(InputConstants.KEY_W)?1:0)-(keys.contains(InputConstants.KEY_S)?1:0),side=(keys.contains(InputConstants.KEY_D)?1:0)-(keys.contains(InputConstants.KEY_A)?1:0),up=(keys.contains(InputConstants.KEY_SPACE)?1:0)-(keys.contains(InputConstants.KEY_LSHIFT)||keys.contains(InputConstants.KEY_RSHIFT)?1:0);double yaw=Math.toRadians(camera.getYRot());var direction=new Vec3(-Math.sin(yaw)*forward+Math.cos(yaw)*side,up,Math.cos(yaw)*forward+Math.sin(yaw)*side);if(direction.lengthSqr()>0){var next=camera.position().add(direction.normalize().scale(keys.contains(InputConstants.KEY_LCONTROL)||keys.contains(InputConstants.KEY_RCONTROL)?1.5:.4));if(Math.abs(next.x-origin.x)<=64&&Math.abs(next.y-origin.y)<=64&&Math.abs(next.z-origin.z)<=64&&mc.level.hasChunkAt(BlockPos.containing(next)))camera.setPos(next);}
 }
 public static boolean key(int action,int key){if(!active())return false;if(action==0)keys.remove(key);else if(action==1){keys.add(key);if(key==InputConstants.KEY_ESCAPE){stop(true);return true;}if((key==InputConstants.KEY_RETURN||key==InputConstants.KEY_NUMPADENTER)&&first!=null&&second!=null){var a=first;var b=second;var target=parent;stop(false);target.selection(a,b);Minecraft.getInstance().gui.setScreen(target);}}return true;}
 public static boolean mouse(int action,int button){if(!active())return false;if(action==1){if(button==InputConstants.MOUSE_BUTTON_RIGHT){first=null;second=null;}else if(button==InputConstants.MOUSE_BUTTON_LEFT){var hit=camera.pick(12,1,false);if(hit instanceof BlockHitResult b&&b.getType()!=HitResult.Type.MISS){if(first==null||second!=null){first=b.getBlockPos().immutable();second=null;}else second=b.getBlockPos().immutable();}}}return true;}
 public static boolean turn(double dx,double dy){if(!active())return false;camera.turn(dx,dy);return true;}
}
