package dev.stow.client.memory;
import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.*;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.*;
import dev.stow.companion.protocol.AreaClaimProtocol;
/**
 * Detached local camera only. The player keeps normal server physics and sends neutral input.
 * Step 1 picks the footprint (left-click corner 1, right-click corner 2, flat at the lower clicked Y);
 * step 2 sets the bottom/top with the wheel or arrow keys, then left-click/Enter confirms.
 */
public final class AreaSelection {
 private static final double REACH=32;
 private static final int GOLD=0xFFFFD486,RED=0xFFFF6B6B,WHITE=0xFFFFFFFF,FIRST=0xFF7CFF8A,SECOND=0xFF6FC3FF,MUTED=0xFFB8C6CC;
 private static ArmorStand camera;private static Entity previous;private static LocalPlayer player;private static ClientInput input;private static CameraType perspective;private static AreaClaimScreen parent;private static Vec3 origin;private static final Set<Integer> keys=new HashSet<>();private static BlockPos first,second;
 private static boolean heightStep;private static int bottom,top;private static BlockPos heightFirst,heightSecond;private static double scrollRemainder;
 public static boolean active(){return camera!=null;}
 public static void register(){HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("stow","area_selection"),(g,delta)->{
  if(!active())return;var mc=Minecraft.getInstance();if(mc.gui.hud.isHidden())return;hints(mc,g);
 });}
 /** Hint overlay; part of the HUD, so F1 hides it. */
 private static void hints(Minecraft mc,GuiGraphicsExtractor g){
  int x=8,y=8,w=g.guiWidth()-16;var font=mc.font;
  g.text(font,Component.translatable(heightStep?"stow.area.step.height":"stow.area.step.footprint"),x,y,GOLD);y+=12;
  var help=Component.translatable(heightStep?"stow.area.height-help":"stow.area.footprint-help");g.textWithWordWrap(font,help,x,y,w,WHITE);y+=font.wordWrapHeight(help,w)+3;
  var controls=Component.translatable("stow.area.fly-controls");g.textWithWordWrap(font,controls,x,y,w,MUTED);y+=font.wordWrapHeight(controls,w)+6;
  List<Component> lines=new ArrayList<>();List<Integer> colors=new ArrayList<>();
  if(heightStep){BlockPos[] box=heightBox();
   lines.add(Component.translatable("stow.area.height-size",top-bottom+1,bottom,top,size(box,0),size(box,1),size(box,2),AreaClaimProtocol.volume(box[0],box[1])));colors.add(WHITE);
   Component problem=problem(box);lines.add(problem==null?Component.translatable("stow.area.confirm-hint"):problem);colors.add(problem==null?GOLD:RED);
  }else{
   lines.add(Component.translatable("stow.area.corners",corner(first),corner(second)));colors.add(WHITE);
   BlockPos hover=hover();BlockPos a=first!=null?first:hover,b=second!=null?second:hover;
   if((first!=null||second!=null)&&a!=null&&b!=null){lines.add(Component.translatable("stow.area.footprint-size",Math.abs(a.getX()-b.getX())+1,Math.abs(a.getZ()-b.getZ())+1));colors.add(WHITE);}
   Component problem=first!=null&&second!=null?problem(footprint(first,second)):null;
   if(problem!=null){lines.add(problem);colors.add(RED);}
   else if(first!=null&&second!=null){lines.add(Component.translatable("stow.area.next-hint"));colors.add(GOLD);}
   else if(hover==null){lines.add(Component.translatable("stow.area.aim"));colors.add(MUTED);}
  }
  // Status sits under the instructions, clear of the hotbar, hearts and chat.
  for(int i=0;i<lines.size();i++){g.textWithWordWrap(font,lines.get(i),x,y,w,colors.get(i));y+=font.wordWrapHeight(lines.get(i),w)+2;}
 }
 /** World-space outlines, drawn from the HUD pass even while F1 hides the HUD. */
 public static void extractOutline(GuiGraphicsExtractor g){
  if(!active())return;var mc=Minecraft.getInstance();if(mc.level==null||!mc.gameRenderer.mainCamera().isInitialized())return;
  var state=mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;if(!state.initialized)return;
  var projection=ChestGlow.worldProjection(mc.gameRenderer);Vec3 eye=state.pos;
  if(heightStep){BlockPos[] box=heightBox();
   ChestGlow.drawBox(g,area(box[0],box[1]),Component.translatable("stow.area.selection",AreaClaimProtocol.volume(box[0],box[1])).getString(),eye,projection,problem(box)==null?GOLD:RED);
   return;
  }
  BlockPos hover=hover();BlockPos a=first!=null?first:hover,b=second!=null?second:hover;
  if((first!=null||second!=null)&&a!=null&&b!=null){BlockPos[] flat=footprint(a,b);
   ChestGlow.drawBox(g,area(flat[0],flat[1]),Component.translatable("stow.area.footprint-label",Math.abs(a.getX()-b.getX())+1,Math.abs(a.getZ()-b.getZ())+1).getString(),eye,projection,first!=null&&second!=null&&problem(flat)!=null?RED:GOLD);}
  if(first!=null)ChestGlow.drawBox(g,new AABB(first).inflate(.004),null,eye,projection,FIRST);
  if(second!=null)ChestGlow.drawBox(g,new AABB(second).inflate(.004),null,eye,projection,SECOND);
  if(hover!=null)ChestGlow.drawBox(g,new AABB(hover).inflate(.008),null,eye,projection,WHITE);
 }
 public static void start(AreaClaimScreen screen){var mc=Minecraft.getInstance();if(active()||mc.level==null||mc.player==null||!AreaClaims.available())return;parent=screen;first=null;second=null;heightStep=false;heightFirst=null;heightSecond=null;scrollRemainder=0;keys.clear();player=mc.player;input=player.input;previous=mc.getCameraEntity();perspective=mc.options.getCameraType();origin=player.position();
  if(player.containerMenu!=player.inventoryMenu)player.closeContainer();mc.gui.setScreen(null);KeyMapping.releaseAll();player.input=new ClientInput();camera=new ArmorStand(mc.level,player.getX(),player.getEyeY(),player.getZ());int cameraId=-1;while(mc.level.getEntity(cameraId)!=null)cameraId--;camera.setId(cameraId);camera.setPos(player.getX(),player.getEyeY()-camera.getEyeHeight(),player.getZ());camera.setYRot(player.getYRot());camera.setXRot(player.getXRot());camera.setYHeadRot(player.getYRot());camera.setOldPosAndRot();mc.options.setCameraType(CameraType.FIRST_PERSON);mc.setCameraEntity(camera);
 }
 public static void stop(boolean back){if(!active())return;var mc=Minecraft.getInstance();camera=null;keys.clear();KeyMapping.releaseAll();if(player!=null)player.input=input;if(mc.player==player){mc.setCameraEntity(previous==null?player:previous);}else mc.setCameraEntity(mc.player);mc.options.setCameraType(perspective);var screen=parent;parent=null;previous=null;player=null;input=null;first=null;second=null;heightStep=false;heightFirst=null;heightSecond=null;scrollRemainder=0;if(back&&screen!=null&&mc.level!=null)mc.gui.setScreen(screen);}
 public static void tick(Minecraft mc){if(!active())return;if(mc.level!=camera.level()||mc.player!=player||!player.isAlive()||mc.gui.screen()!=null||mc.getCameraEntity()!=camera||player.position().distanceToSqr(origin)>16){stop(false);return;}
  camera.setOldPosAndRot();double forward=(keys.contains(InputConstants.KEY_W)?1:0)-(keys.contains(InputConstants.KEY_S)?1:0),side=(keys.contains(InputConstants.KEY_D)?1:0)-(keys.contains(InputConstants.KEY_A)?1:0),up=(keys.contains(InputConstants.KEY_SPACE)?1:0)-(keys.contains(InputConstants.KEY_LSHIFT)||keys.contains(InputConstants.KEY_RSHIFT)?1:0);double yaw=Math.toRadians(camera.getYRot());var direction=Entity.calculateViewVector(camera.getXRot(),camera.getYRot()).scale(forward).add(-Math.cos(yaw)*side,up,-Math.sin(yaw)*side);if(direction.lengthSqr()>0){var next=camera.position().add(direction.normalize().scale(ctrl()?1.5:.4));if(Math.abs(next.x-origin.x)<=64&&Math.abs(next.y-origin.y)<=64&&Math.abs(next.z-origin.z)<=64&&mc.level.hasChunkAt(BlockPos.containing(next)))camera.setPos(next);}
 }
 public static boolean key(int action,KeyEvent event){if(!active())return false;var mc=Minecraft.getInstance();int key=event.key();
  // Toggle the HUD like vanilla F1 (respects rebinding); vanilla keybind handling is paused while the camera is active.
  if(mc.options.keyToggleGui.matches(event)){if(action==1)mc.gui.hud.toggle();return true;}
  if(action==0){keys.remove(key);return true;}
  boolean upKey=key==InputConstants.KEY_UP||key==InputConstants.KEY_PAGEUP,downKey=key==InputConstants.KEY_DOWN||key==InputConstants.KEY_PAGEDOWN;
  if(action==2){if(heightStep&&(upKey||downKey))adjust(upKey?1:-1);return true;}
  if(action!=1)return true;
  keys.add(key);
  if(key==InputConstants.KEY_ESCAPE){stop(true);return true;}
  if(key==InputConstants.KEY_RETURN||key==InputConstants.KEY_NUMPADENTER){if(heightStep)confirm();else nextStep();}
  else if(key==InputConstants.KEY_BACKSPACE){if(heightStep)backStep();else undo();}
  else if(heightStep&&(upKey||downKey))adjust(upKey?1:-1);
  return true;}
 public static boolean mouse(int action,int button){if(!active())return false;if(action!=1)return true;
  if(heightStep){if(button==InputConstants.MOUSE_BUTTON_LEFT)confirm();else if(button==InputConstants.MOUSE_BUTTON_RIGHT)backStep();return true;}
  if(button==InputConstants.MOUSE_BUTTON_LEFT||button==InputConstants.MOUSE_BUTTON_RIGHT){BlockPos hit=hover();
   if(hit==null){sound(false);return true;}
   if(button==InputConstants.MOUSE_BUTTON_LEFT)first=hit.immutable();else second=hit.immutable();sound(true);}
  return true;}
 /** Wheel adjusts the top (Alt: bottom) in the height step; otherwise swallowed so hotbar selection does not change. */
 public static boolean scroll(double amount){if(!active())return false;if(!heightStep)return true;var options=Minecraft.getInstance().options;
  double scaled=(options.discreteMouseScroll().get()?Math.signum(amount):amount)*options.mouseWheelSensitivity().get();scrollRemainder+=scaled;int notches=(int)scrollRemainder;scrollRemainder-=notches;if(notches!=0)adjust(notches);return true;}
 public static boolean turn(double dx,double dy){if(!active())return false;camera.turn(dx,dy);camera.setYHeadRot(camera.getYRot());return true;}
 private static void nextStep(){if(first==null||second==null){sound(false);return;}
  if(!first.equals(heightFirst)||!second.equals(heightSecond)){bottom=Math.min(first.getY(),second.getY());top=Math.max(first.getY(),second.getY());heightFirst=first;heightSecond=second;}
  heightStep=true;scrollRemainder=0;sound(true);}
 private static void backStep(){heightStep=false;scrollRemainder=0;sound(true);}
 private static void undo(){if(second!=null)second=null;else if(first!=null)first=null;else{sound(false);return;}sound(true);}
 private static void adjust(int notches){var mc=Minecraft.getInstance();if(mc.level==null)return;int delta=notches*(ctrl()?5:1);
  int low=Math.max(mc.level.getMinY(),(int)Math.floor(origin.y)-AreaClaimProtocol.MAX_DISTANCE),high=Math.min(mc.level.getMaxY(),(int)Math.floor(origin.y)+AreaClaimProtocol.MAX_DISTANCE);
  if(alt())bottom=Math.max(low,Math.min(top,bottom+delta));else top=Math.min(high,Math.max(bottom,top+delta));}
 private static void confirm(){BlockPos[] box=heightBox();if(problem(box)!=null){sound(false);return;}
  var target=parent;stop(false);sound(true);if(target!=null){target.selection(box[0],box[1]);Minecraft.getInstance().gui.setScreen(target);}}
 private static BlockPos hover(){if(camera==null)return null;var hit=camera.pick(REACH,1,false);return hit instanceof BlockHitResult b&&b.getType()!=HitResult.Type.MISS?b.getBlockPos():null;}
 private static BlockPos[] footprint(BlockPos a,BlockPos b){int y=Math.min(a.getY(),b.getY());return new BlockPos[]{new BlockPos(Math.min(a.getX(),b.getX()),y,Math.min(a.getZ(),b.getZ())),new BlockPos(Math.max(a.getX(),b.getX()),y,Math.max(a.getZ(),b.getZ()))};}
 private static BlockPos[] heightBox(){BlockPos[] flat=footprint(first,second);return new BlockPos[]{flat[0].atY(bottom),flat[1].atY(top)};}
 private static AABB area(BlockPos a,BlockPos b){return new AABB(a).minmax(new AABB(b)).inflate(.002);}
 private static int size(BlockPos[] box,int axis){return axis==0?box[1].getX()-box[0].getX()+1:axis==1?box[1].getY()-box[0].getY()+1:box[1].getZ()-box[0].getZ()+1;}
 private static Component problem(BlockPos[] box){if(AreaClaimProtocol.volume(box[0],box[1])>AreaClaimProtocol.MAX_VOLUME)return Component.translatable("stow.area.too-large");if(!AreaClaims.validSelection(box[0],box[1]))return Component.translatable("stow.area.out-of-range");return null;}
 private static Component corner(BlockPos pos){return pos==null?Component.translatable("stow.area.unset"):Component.literal(pos.toShortString());}
 private static boolean ctrl(){return keys.contains(InputConstants.KEY_LCONTROL)||keys.contains(InputConstants.KEY_RCONTROL);}
 private static boolean alt(){return keys.contains(InputConstants.KEY_LALT)||keys.contains(InputConstants.KEY_RALT);}
 private static void sound(boolean ok){Minecraft.getInstance().getSoundManager().play(ok?SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,1.2f):SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS,0.7f));}
}
