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
import dev.stow.client.ui.UIIcons;
/**
 * Detached local camera only. Movement follows the player's bound movement/jump/sneak/sprint keys (keyboard bindings; defaults otherwise).
 * The player keeps normal server physics and sends neutral input.
 * Step 1 picks the footprint (left-click corner 1, right-click corner 2, flat at the lower clicked Y); right-click with corner 1 set,
 * or Enter, advances when the footprint is valid. Step 2 sets the bottom/top with the wheel or arrow keys; left-click/Enter confirms,
 * Backspace goes back and right-click does nothing.
 */
public final class AreaSelection {
 private static final double REACH=32;
 private static final int GOLD=0xFFFFD486,RED=0xFFFFAA91,WHITE=0xFFFFFFFF,FIRST=0xFF9EE6C4,SECOND=0xFF8FC8FF,MUTED=0xFFB8C6CC,OK=0xFF9EE6C4;
 private static ArmorStand camera;private static Entity previous;private static LocalPlayer player;private static ClientInput input;private static CameraType perspective;private static AreaClaimScreen parent;private static Vec3 origin;private static final Set<Integer> keys=new HashSet<>();private static BlockPos first,second;
 private static boolean heightStep;private static int bottom,top;private static BlockPos heightFirst,heightSecond;private static double scrollRemainder;
 public static boolean active(){return camera!=null;}
 public static void register(){HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("stow","area_selection"),(g,delta)->{
  if(!active())return;var mc=Minecraft.getInstance();if(mc.gui.hud.isHidden())return;hints(mc,g);
 });}
 /**
  * Controls panel; part of the HUD, so F1 hides it. Styled like Stow's command palette and notifications:
  * tinted menu-list background with a grey outline, white action names, grey key names right-aligned
  * (as the palette shows shortcuts) and UIIcons pixel symbols for the mouse.
  */
 private record Cell(Component action,int color,UIIcons.Kind icon,Component key){}
 private record Status(Component text,int color){}
 private static final int PAD=8,ROW=11,COLUMN_GAP=14,KEY=0xFFAAAAAA,DISABLED=0xFF999999,ICON=0xFFE7ECEF;
 private static void hints(Minecraft mc,GuiGraphicsExtractor g){
  var font=mc.font;var o=mc.options;
  List<Cell> cells=new ArrayList<>();List<Status> status=new ArrayList<>();
  Component wasd=Component.empty().append(keyName(o.keyUp,InputConstants.KEY_W)).append(" ").append(keyName(o.keyLeft,InputConstants.KEY_A)).append(" ").append(keyName(o.keyDown,InputConstants.KEY_S)).append(" ").append(keyName(o.keyRight,InputConstants.KEY_D));
  cells.add(new Cell(Component.translatable("stow.area.act.move"),WHITE,null,wasd));
  cells.add(new Cell(Component.translatable("stow.area.act.faster"),WHITE,null,keyName(o.keySprint,InputConstants.KEY_LCONTROL)));
  cells.add(new Cell(Component.translatable("stow.area.act.up"),WHITE,null,keyName(o.keyJump,InputConstants.KEY_SPACE)));
  cells.add(new Cell(Component.translatable("stow.area.act.down"),WHITE,null,keyName(o.keyShift,InputConstants.KEY_LSHIFT)));
  boolean both=first!=null&&second!=null;
  if(heightStep){BlockPos[] box=heightBox();Component problem=problem(box);
   cells.add(new Cell(Component.translatable("stow.area.act.top"),WHITE,UIIcons.Kind.MOUSE_WHEEL,Component.translatable("stow.area.key.wheel-arrows")));
   cells.add(new Cell(Component.translatable("stow.area.act.bottom"),WHITE,UIIcons.Kind.MOUSE_WHEEL,Component.translatable("stow.area.key.with",Component.translatable("stow.area.key.alt"))));
   cells.add(new Cell(Component.translatable("stow.area.act.fine"),WHITE,UIIcons.Kind.MOUSE_WHEEL,Component.translatable("stow.area.key.with",keyName(o.keySprint,InputConstants.KEY_LCONTROL))));
   cells.add(new Cell(Component.translatable("stow.area.act.confirm"),problem==null?GOLD:DISABLED,UIIcons.Kind.MOUSE_LEFT,Component.translatable("stow.area.key.or",Component.translatable("stow.area.key.left-click"),Component.translatable("stow.area.key.enter"))));
   cells.add(new Cell(Component.translatable("stow.area.act.full-height"),WHITE,null,Component.translatable("stow.area.key.v")));
   cells.add(new Cell(Component.translatable("stow.area.act.back"),WHITE,null,Component.translatable("stow.area.key.backspace")));
   status.add(new Status(Component.translatable("stow.area.height-range",top-bottom+1,bottom,top),WHITE));
   status.add(new Status(Component.translatable("stow.area.box-size",size(box,0),size(box,1),size(box,2),AreaClaimProtocol.volume(box[0],box[1])),problem==null?OK:RED));
   status.add(new Status(Component.translatable("stow.area.containers",remembered(box)),MUTED));
   if(problem!=null)status.add(new Status(problem,RED));
  }else{BlockPos hover=hover();Component problem=both?problem(footprint(first,second)):null;
   cells.add(new Cell(Component.translatable("stow.area.act.corner1"),FIRST,UIIcons.Kind.MOUSE_LEFT,Component.translatable("stow.area.key.left-click")));
   cells.add(new Cell(Component.translatable("stow.area.act.corner2"),SECOND,UIIcons.Kind.MOUSE_RIGHT,Component.translatable("stow.area.key.right-click")));
   cells.add(new Cell(Component.translatable("stow.area.act.next"),both&&problem==null?GOLD:DISABLED,null,Component.translatable("stow.area.key.enter")));
   cells.add(new Cell(Component.translatable("stow.area.act.undo"),WHITE,null,Component.translatable("stow.area.key.backspace")));
   status.add(new Status(Component.translatable("stow.area.corner1",corner(first)),first==null?KEY:FIRST));
   status.add(new Status(Component.translatable("stow.area.corner2",corner(second)),second==null?KEY:SECOND));
   BlockPos a=first!=null?first:hover,b=second!=null?second:hover;
   if((first!=null||second!=null)&&a!=null&&b!=null)status.add(new Status(Component.translatable("stow.area.footprint-size",Math.abs(a.getX()-b.getX())+1,Math.abs(a.getZ()-b.getZ())+1),WHITE));
   if(problem!=null)status.add(new Status(problem,RED));
   else if(both)status.add(new Status(Component.translatable("stow.area.next-hint"),GOLD));
   else if(hover==null)status.add(new Status(Component.translatable("stow.area.aim"),KEY));
  }
  cells.add(new Cell(Component.translatable("stow.area.act.cancel"),WHITE,null,Component.translatable("stow.area.key.esc")));
  cells.add(new Cell(Component.translatable("stow.area.act.hide"),WHITE,null,keyName(o.keyToggleGui,InputConstants.KEY_F1)));
  int cell=0;for(var c:cells)cell=Math.max(cell,font.width(c.action())+10+(c.icon()==null?0:10)+font.width(c.key()));
  int maxInner=Math.max(140,Math.min(g.guiWidth()-12,380)-2*PAD);
  int columns=2*cell+COLUMN_GAP<=maxInner?2:1;int inner=columns==2?2*cell+COLUMN_GAP:Math.min(maxInner,cell);
  Component header=Component.translatable("stow.area.step",heightStep?2:1,2,Component.translatable(heightStep?"stow.area.step-name.height":"stow.area.step-name.footprint"));
  String progress=(heightStep?2:1)+" / 2";
  inner=Math.max(inner,font.width(header)+12+font.width(progress));
  List<List<net.minecraft.util.FormattedCharSequence>> wrapped=new ArrayList<>();int statusH=0;
  for(var line:status){var split=font.split(line.text(),inner);wrapped.add(split);statusH+=10*split.size();}
  int rows=(cells.size()+columns-1)/columns;
  int w=inner+2*PAD,h=PAD+10+6+rows*ROW+5+statusH+PAD-2;
  // Bottom-left beside the hotbar when it fits, otherwise top-left; both stay clear of the centred crosshair.
  int x=6,y=6+w<=g.guiWidth()/2-91-4?g.guiHeight()-6-h:6;
  g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,Identifier.withDefaultNamespace("textures/gui/menu_list_background.png"),x,y,0,0,w,h,w,h,32,32,0xFF444444);
  g.outline(x,y,w,h,0xFF666666);
  int cx=x+PAD,cy=y+PAD;
  g.text(font,header,cx,cy,WHITE);g.text(font,progress,x+w-PAD-font.width(progress),cy,KEY);
  cy+=10;g.fill(cx,cy+2,x+w-PAD,cy+3,0xFF666666);cy+=6;
  for(int i=0;i<cells.size();i++){var c=cells.get(i);int col=i%columns,row=i/columns;int left=cx+col*(cell+COLUMN_GAP),top=cy+row*ROW,right=columns==2?left+cell:x+w-PAD;
   g.text(font,c.action(),left,top,c.color());
   int keyX=right-font.width(c.key());g.text(font,c.key(),keyX,top,KEY);
   if(c.icon()!=null)UIIcons.symbol(g,c.icon(),keyX-10,top,c.color()==WHITE||c.color()==DISABLED?ICON:c.color());}
  cy+=rows*ROW+5;
  for(int i=0;i<wrapped.size();i++)for(var part:wrapped.get(i)){g.text(font,part,cx,cy,status.get(i).color());cy+=10;}
 }
 private static boolean held(KeyMapping binding,int... defaults){var key=((dev.stow.client.mixin.KeyMappingAccessor)binding).stow_key();
  if(!binding.isUnbound()&&key.getType()==InputConstants.Type.KEYBOARD)return keys.contains(key.getValue());
  for(int d:defaults)if(keys.contains(d))return true;return false;}
 private static Component keyName(KeyMapping binding,int fallback){var key=((dev.stow.client.mixin.KeyMappingAccessor)binding).stow_key();
  return !binding.isUnbound()&&key.getType()==InputConstants.Type.KEYBOARD?key.getDisplayName():InputConstants.Type.KEYBOARD.getOrCreate(fallback).getDisplayName();}
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
  camera.setOldPosAndRot();var o=mc.options;double forward=(held(o.keyUp,InputConstants.KEY_W)?1:0)-(held(o.keyDown,InputConstants.KEY_S)?1:0),side=(held(o.keyRight,InputConstants.KEY_D)?1:0)-(held(o.keyLeft,InputConstants.KEY_A)?1:0),up=(held(o.keyJump,InputConstants.KEY_SPACE)?1:0)-(held(o.keyShift,InputConstants.KEY_LSHIFT,InputConstants.KEY_RSHIFT)?1:0);double yaw=Math.toRadians(camera.getYRot());var direction=Entity.calculateViewVector(camera.getXRot(),camera.getYRot()).scale(forward).add(-Math.cos(yaw)*side,up,-Math.sin(yaw)*side);if(direction.lengthSqr()>0){var next=camera.position().add(direction.normalize().scale(ctrl()?1.5:.4));if(Math.abs(next.x-origin.x)<=64&&Math.abs(next.y-origin.y)<=64&&Math.abs(next.z-origin.z)<=64&&mc.level.hasChunkAt(BlockPos.containing(next)))camera.setPos(next);}
 }
 public static boolean key(int action,KeyEvent event){if(!active())return false;var mc=Minecraft.getInstance();int key=event.key();
  // Toggle the HUD like vanilla F1 (respects rebinding); vanilla keybind handling is paused while the camera is active.
  if(mc.options.keyToggleGui.matches(event)||mc.options.keyToggleGui.isUnbound()&&key==InputConstants.KEY_F1){if(action==1)mc.gui.hud.toggle();return true;}
  if(action==0){keys.remove(key);return true;}
  boolean upKey=key==InputConstants.KEY_UP||key==InputConstants.KEY_PAGEUP,downKey=key==InputConstants.KEY_DOWN||key==InputConstants.KEY_PAGEDOWN;
  if(action==2){if(heightStep&&(upKey||downKey))adjust(upKey?1:-1);return true;}
  if(action!=1)return true;
  keys.add(key);
  if(key==InputConstants.KEY_ESCAPE){stop(true);return true;}
  if(key==InputConstants.KEY_RETURN||key==InputConstants.KEY_NUMPADENTER){if(heightStep)confirm();else nextStep();}
  else if(key==InputConstants.KEY_BACKSPACE){if(heightStep)backStep();else undo();}
  else if(heightStep&&(upKey||downKey))adjust(upKey?1:-1);
  else if(heightStep&&key==InputConstants.KEY_V)fullHeight();
  return true;}
 /** V (adapted from WorldEdit's //expand vert): the tallest box the Companion allows over this footprint, centred on the player's height. */
 private static void fullHeight(){var mc=Minecraft.getInstance();if(mc.level==null||first==null||second==null)return;
  int low=Math.max(mc.level.getMinY(),(int)Math.floor(origin.y)-AreaClaimProtocol.MAX_DISTANCE),high=Math.min(mc.level.getMaxY(),(int)Math.floor(origin.y)+AreaClaimProtocol.MAX_DISTANCE);
  BlockPos[] flat=footprint(first,second);long area=(long)size(flat,0)*size(flat,2);
  int span=(int)Math.max(1,Math.min(high-low+1,AreaClaimProtocol.MAX_VOLUME/Math.max(1,area)));
  int start=Math.clamp((int)Math.floor(origin.y)-span/2,low,Math.max(low,high-span+1));
  bottom=start;top=Math.min(high,start+span-1);
  // the server checks reach per block; shrink until the local check agrees, as a guard
  while(top>bottom&&problem(heightBox())!=null){top--;if(top>bottom&&problem(heightBox())!=null)bottom++;}
  sound(problem(heightBox())==null);}
 /** Remembered containers inside the box (this dimension), for the live readout. */
 private static int remembered(BlockPos[] box){var store=parent==null?null:parent.store();var mc=Minecraft.getInstance();if(store==null||mc.level==null)return 0;
  String dim=mc.level.dimension().identifier().toString();int n=0;
  for(var chest:store.chests()){var l=chest.location();if(l.dimension().equals(dim)&&l.x()>=box[0].getX()&&l.x()<=box[1].getX()&&l.y()>=box[0].getY()&&l.y()<=box[1].getY()&&l.z()>=box[0].getZ()&&l.z()<=box[1].getZ())n++;}
  return n;}
 public static boolean mouse(int action,int button){if(!active())return false;if(action!=1)return true;
  // Height step: right-click is deliberately ignored, so a quick second right-click after advancing cannot go back or confirm by accident.
  if(heightStep){if(button==InputConstants.MOUSE_BUTTON_LEFT)confirm();return true;}
  if(button==InputConstants.MOUSE_BUTTON_LEFT||button==InputConstants.MOUSE_BUTTON_RIGHT){BlockPos hit=hover();
   if(hit==null){sound(false);return true;}
   if(button==InputConstants.MOUSE_BUTTON_LEFT){first=hit.immutable();sound(true);}
   else{second=hit.immutable();if(first==null)sound(true);else nextStep();}}
  return true;}
 /** Wheel adjusts the top (Alt: bottom) in the height step; otherwise swallowed so hotbar selection does not change. */
 public static boolean scroll(double amount){if(!active())return false;if(!heightStep)return true;var options=Minecraft.getInstance().options;
  double scaled=(options.discreteMouseScroll().get()?Math.signum(amount):amount)*options.mouseWheelSensitivity().get();scrollRemainder+=scaled;int notches=(int)scrollRemainder;scrollRemainder-=notches;if(notches!=0)adjust(notches);return true;}
 public static boolean turn(double dx,double dy){if(!active())return false;camera.turn(dx,dy);camera.setYHeadRot(camera.getYRot());return true;}
 private static void nextStep(){if(first==null||second==null||problem(footprint(first,second))!=null){sound(false);return;}
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
 private static boolean ctrl(){return held(Minecraft.getInstance().options.keySprint,InputConstants.KEY_LCONTROL,InputConstants.KEY_RCONTROL);}
 private static boolean alt(){return keys.contains(InputConstants.KEY_LALT)||keys.contains(InputConstants.KEY_RALT);}
 private static void sound(boolean ok){Minecraft.getInstance().getSoundManager().play(ok?SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,1.2f):SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS,0.7f));}
}
