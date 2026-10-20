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
 * Detached local camera only. Movement follows the player's bound movement/jump/sneak/sprint keys (keyboard bindings; defaults otherwise).
 * The player keeps normal server physics and sends neutral input.
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
 /** Controls panel; part of the HUD, so F1 hides it. Key caps and mouse icons are GUI-pixel fills, so they stay crisp at every GUI scale. */
 private record Cap(Component label,boolean down){}
 private enum Mouse{LEFT,RIGHT,WHEEL}
 private record Sep(String text){}
 private record Chip(List<Object> parts,Component action,int color){}
 private record Status(Component text,int color){}
 private static final int PAD=5,ROW=14,CAP_H=12,CHIP_GAP=10,TEXT=0xFFE6EAEE,OK=0xFF7CFF8A,PANEL=0xB0101418,PANEL_EDGE=0x50FFFFFF,CAP_EDGE=0xFFC8D0D8,CAP_BODY=0xD0181C20,CAP_DOWN=0xE0485058;
 private static void hints(Minecraft mc,GuiGraphicsExtractor g){
  var font=mc.font;var o=mc.options;
  List<List<Chip>> groups=new ArrayList<>();
  groups.add(List.of(
   new Chip(List.of(cap(o.keyUp,InputConstants.KEY_W),cap(o.keyLeft,InputConstants.KEY_A),cap(o.keyDown,InputConstants.KEY_S),cap(o.keyRight,InputConstants.KEY_D)),Component.translatable("stow.area.act.move"),TEXT),
   new Chip(List.of(cap(o.keyJump,InputConstants.KEY_SPACE)),Component.translatable("stow.area.act.up"),TEXT),
   new Chip(List.of(cap(o.keyShift,InputConstants.KEY_LSHIFT)),Component.translatable("stow.area.act.down"),TEXT),
   new Chip(List.of(cap(o.keySprint,InputConstants.KEY_LCONTROL)),Component.translatable("stow.area.act.faster"),TEXT)));
  boolean both=first!=null&&second!=null;
  List<Status> status=new ArrayList<>();
  if(heightStep){BlockPos[] box=heightBox();Component problem=problem(box);
   groups.add(List.of(
    new Chip(List.of(Mouse.WHEEL,new Sep("/"),fixed("stow.area.key.up",InputConstants.KEY_UP),fixed("stow.area.key.down",InputConstants.KEY_DOWN)),Component.translatable("stow.area.act.top"),TEXT),
    new Chip(List.of(fixed("stow.area.key.alt",InputConstants.KEY_LALT),new Sep("+"),Mouse.WHEEL),Component.translatable("stow.area.act.bottom"),TEXT),
    new Chip(List.of(cap(o.keySprint,InputConstants.KEY_LCONTROL),new Sep("+"),Mouse.WHEEL),Component.translatable("stow.area.act.fine"),TEXT)));
   groups.add(List.of(
    new Chip(List.of(Mouse.LEFT,new Sep("/"),fixed("stow.area.key.enter",InputConstants.KEY_RETURN)),Component.translatable("stow.area.act.confirm"),problem==null?OK:MUTED),
    new Chip(List.of(Mouse.RIGHT,new Sep("/"),fixed("stow.area.key.backspace",InputConstants.KEY_BACKSPACE)),Component.translatable("stow.area.act.back"),TEXT)));
   status.add(new Status(Component.translatable("stow.area.height-range",top-bottom+1,bottom,top),TEXT));
   status.add(new Status(Component.translatable("stow.area.box-size",size(box,0),size(box,1),size(box,2),AreaClaimProtocol.volume(box[0],box[1])),problem==null?OK:RED));
   if(problem!=null)status.add(new Status(problem,RED));
  }else{BlockPos hover=hover();
   groups.add(List.of(
    new Chip(List.of(Mouse.LEFT),Component.translatable("stow.area.act.corner1"),FIRST),
    new Chip(List.of(Mouse.RIGHT),Component.translatable("stow.area.act.corner2"),SECOND)));
   groups.add(List.of(
    new Chip(List.of(fixed("stow.area.key.backspace",InputConstants.KEY_BACKSPACE)),Component.translatable("stow.area.act.undo"),TEXT),
    new Chip(List.of(fixed("stow.area.key.enter",InputConstants.KEY_RETURN)),Component.translatable("stow.area.act.next"),both?GOLD:MUTED)));
   status.add(new Status(Component.translatable("stow.area.corner1",corner(first)),first==null?MUTED:FIRST));
   status.add(new Status(Component.translatable("stow.area.corner2",corner(second)),second==null?MUTED:SECOND));
   BlockPos a=first!=null?first:hover,b=second!=null?second:hover;
   if((first!=null||second!=null)&&a!=null&&b!=null)status.add(new Status(Component.translatable("stow.area.footprint-size",Math.abs(a.getX()-b.getX())+1,Math.abs(a.getZ()-b.getZ())+1),TEXT));
   Component problem=both?problem(footprint(first,second)):null;
   if(problem!=null)status.add(new Status(problem,RED));
   else if(both)status.add(new Status(Component.translatable("stow.area.next-hint"),GOLD));
   else if(hover==null)status.add(new Status(Component.translatable("stow.area.aim"),MUTED));
  }
  groups.add(List.of(
   new Chip(List.of(fixed("stow.area.key.esc",InputConstants.KEY_ESCAPE)),Component.translatable("stow.area.act.cancel"),TEXT),
   new Chip(List.of(cap(o.keyToggleGui,InputConstants.KEY_F1)),Component.translatable("stow.area.act.hide"),TEXT)));
  // Lay out: each logical group wraps on its own; the panel shrinks to its widest row.
  int maxInner=Math.max(120,Math.min(g.guiWidth()-12,330)-2*PAD);
  List<List<Chip>> rows=new ArrayList<>();int inner=0;
  for(var group:groups){List<Chip> row=new ArrayList<>();int w=0;
   for(var chip:group){int cw=chipWidth(font,chip);if(!row.isEmpty()&&w+CHIP_GAP+cw>maxInner){rows.add(row);inner=Math.max(inner,w);row=new ArrayList<>();w=0;}w+=(row.isEmpty()?0:CHIP_GAP)+cw;row.add(chip);}
   rows.add(row);inner=Math.max(inner,w);}
  Component header=Component.translatable("stow.area.step",heightStep?2:1,2,Component.translatable(heightStep?"stow.area.step-name.height":"stow.area.step-name.footprint"));
  inner=Math.max(inner,font.width(header)+20);
  for(var line:status)inner=Math.max(inner,Math.min(maxInner,font.width(line.text())));
  List<List<net.minecraft.util.FormattedCharSequence>> wrapped=new ArrayList<>();int statusH=0;
  for(var line:status){var split=font.split(line.text(),inner);wrapped.add(split);statusH+=10*split.size();}
  int w=inner+2*PAD,h=PAD+12+3+rows.size()*ROW+4+statusH+PAD-2;
  // Bottom-left beside the hotbar when it fits, otherwise top-left; both stay clear of the centred crosshair.
  int x=6,y=6+w<=g.guiWidth()/2-91-4?g.guiHeight()-6-h:6;
  g.fill(x+1,y,x+w-1,y+h,PANEL);g.fill(x,y+1,x+1,y+h-1,PANEL);g.fill(x+w-1,y+1,x+w,y+h-1,PANEL);
  g.fill(x+1,y,x+w-1,y+1,PANEL_EDGE);g.fill(x+1,y+h-1,x+w-1,y+h,PANEL_EDGE);g.fill(x,y+1,x+1,y+h-1,PANEL_EDGE);g.fill(x+w-1,y+1,x+w,y+h-1,PANEL_EDGE);
  int cx=x+PAD,cy=y+PAD;
  g.text(font,header,cx,cy+1,GOLD,false);
  for(int i=0;i<2;i++){int dx=x+w-PAD-16+i*9,color=i<=(heightStep?1:0)?GOLD:0x60FFFFFF;g.fill(dx+1,cy+2,dx+5,cy+7,color);g.fill(dx,cy+3,dx+6,cy+6,color);}
  cy+=12;g.fill(cx,cy+1,x+w-PAD,cy+2,0x30FFFFFF);cy+=3;
  for(var row:rows){int rx=cx;for(var chip:row){rx=drawChip(g,font,rx,cy+1,chip)+CHIP_GAP;}cy+=ROW;}
  cy+=4;
  for(int i=0;i<wrapped.size();i++)for(var part:wrapped.get(i)){g.text(font,part,cx,cy,status.get(i).color(),false);cy+=10;}
 }
 private static Cap cap(KeyMapping binding,int fallback){return new Cap(keyName(binding,fallback),held(binding,fallback));}
 private static Cap fixed(String label,int code){return new Cap(Component.translatable(label),keys.contains(code));}
 private static int partWidth(net.minecraft.client.gui.Font font,Object part){return part instanceof Cap c?Math.max(CAP_H,font.width(c.label())+6):part instanceof Sep s?font.width(s.text()):9;}
 private static int chipWidth(net.minecraft.client.gui.Font font,Chip chip){int w=0;for(var part:chip.parts())w+=partWidth(font,part)+2;return w+2+font.width(chip.action());}
 private static int drawChip(GuiGraphicsExtractor g,net.minecraft.client.gui.Font font,int x,int y,Chip chip){
  for(var part:chip.parts()){int pw=partWidth(font,part);
   if(part instanceof Cap c){
    g.fill(x+1,y,x+pw-1,y+1,CAP_EDGE);g.fill(x+1,y+CAP_H-1,x+pw-1,y+CAP_H,CAP_EDGE);g.fill(x,y+1,x+1,y+CAP_H-1,CAP_EDGE);g.fill(x+pw-1,y+1,x+pw,y+CAP_H-1,CAP_EDGE);
    g.fill(x+1,y+1,x+pw-1,y+CAP_H-1,c.down()?CAP_DOWN:CAP_BODY);g.fill(x+1,y+CAP_H-2,x+pw-1,y+CAP_H-1,0x50000000);
    g.text(font,c.label(),x+(pw-font.width(c.label())+1)/2,y+2,WHITE,false);
   }else if(part instanceof Sep s)g.text(font,s.text(),x,y+2,MUTED,false);
   else if(part instanceof Mouse m)mouseIcon(g,x+0,y,m,chip.color()==TEXT||chip.color()==MUTED?GOLD:chip.color());
   x+=pw+2;}
  g.text(font,chip.action(),x+2,y+2,chip.color(),false);return x+2+font.width(chip.action());
 }
 /** 9x12 GUI-pixel mouse: rounded body, split buttons and wheel; the used part is filled with the accent colour. */
 private static void mouseIcon(GuiGraphicsExtractor g,int x,int y,Mouse m,int accent){
  int edge=CAP_EDGE,body=CAP_BODY;
  g.fill(x+2,y,x+7,y+1,edge);g.fill(x+2,y+11,x+7,y+12,edge);g.fill(x,y+2,x+1,y+10,edge);g.fill(x+8,y+2,x+9,y+10,edge);
  g.fill(x+1,y+1,x+2,y+2,edge);g.fill(x+7,y+1,x+8,y+2,edge);g.fill(x+1,y+10,x+2,y+11,edge);g.fill(x+7,y+10,x+8,y+11,edge);
  g.fill(x+2,y+1,x+7,y+11,body);g.fill(x+1,y+2,x+8,y+10,body);
  if(m==Mouse.LEFT){g.fill(x+1,y+2,x+4,y+5,accent);g.fill(x+2,y+1,x+4,y+2,accent);}
  if(m==Mouse.RIGHT){g.fill(x+5,y+2,x+8,y+5,accent);g.fill(x+5,y+1,x+7,y+2,accent);}
  g.fill(x+1,y+5,x+8,y+6,edge);
  if(m==Mouse.WHEEL)g.fill(x+4,y+1,x+5,y+5,accent);else g.fill(x+4,y+1,x+5,y+5,edge);
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
 private static boolean ctrl(){return held(Minecraft.getInstance().options.keySprint,InputConstants.KEY_LCONTROL,InputConstants.KEY_RCONTROL);}
 private static boolean alt(){return keys.contains(InputConstants.KEY_LALT)||keys.contains(InputConstants.KEY_RALT);}
 private static void sound(boolean ok){Minecraft.getInstance().getSoundManager().play(ok?SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,1.2f):SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS,0.7f));}
}
