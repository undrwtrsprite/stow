package dev.stow.companion.server;
import dev.stow.companion.protocol.StorageOwnerProtocol;
import dev.stow.companion.protocol.StorageOwnerProtocol.*;
import static dev.stow.companion.protocol.StorageOwnerProtocol.*;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.ChestType;
/** Labels are informational. They do not grant or restrict inventory access. */
public final class StorageOwnerServer implements ModInitializer {
 private final Map<UUID,int[]> budgets=new HashMap<>();
 public void onInitialize(){StorageOwnerProtocol.register();ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(r,c)->c.server().execute(()->{
  var p=c.player();var budget=budgets.computeIfAbsent(p.getUUID(),k->new int[]{p.tickCount,0});if(p.tickCount-budget[0]>=20){budget[0]=p.tickCount;budget[1]=0;}if(++budget[1]>32)return;
  var result=apply(p,r);if(ServerPlayNetworking.canSend(p,Result.TYPE))ServerPlayNetworking.send(p,result);
 }));ServerPlayConnectionEvents.DISCONNECT.register((h,s)->budgets.remove(h.player.getUUID()));ServerLifecycleEvents.SERVER_STOPPED.register(s->budgets.clear());}
 public static boolean supported(BlockEntity b){return b instanceof ChestBlockEntity||b instanceof BarrelBlockEntity||b instanceof ShulkerBoxBlockEntity;}
 public static void placed(Level level,BlockPos pos,LivingEntity placer){if(!(level instanceof ServerLevel)||!(placer instanceof ServerPlayer p))return;var b=level.getBlockEntity(pos);if(supported(b)&&b instanceof OwnedStorage owned)owned.stow$owner(new Owner(p.getUUID().toString(),p.getName().getString(),false));}
 private static List<BaseContainerBlockEntity> parts(ServerLevel level,BlockPos pos){
  if(!level.hasChunkAt(pos))return List.of();var b=level.getBlockEntity(pos);if(!supported(b))return List.of();
  var list=new ArrayList<BaseContainerBlockEntity>();list.add((BaseContainerBlockEntity)b);var state=level.getBlockState(pos);
  if(state.getBlock() instanceof ChestBlock&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){var other=ChestBlock.getConnectedBlockPos(pos,state);if(!level.hasChunkAt(other))return List.of();var partner=level.getBlockEntity(other);if(!(partner instanceof ChestBlockEntity)||!level.getBlockState(other).is(state.getBlock()))return List.of();list.add((BaseContainerBlockEntity)partner);}
  return list;
 }
 private static Owner combined(List<BaseContainerBlockEntity> parts){
  Owner found=null;boolean unknown=false;
  for(var b:parts){var owner=((OwnedStorage)b).stow$owner();if(owner==null){unknown=true;continue;}if(found!=null&&(!found.id().equals(owner.id())||found.shared()!=owner.shared()))return new Owner("mixed","",false);found=owner;}
  if(found!=null&&unknown)return new Owner("mixed","",false);return found==null?new Owner("","",false):found;
 }
 private static boolean allowed(ServerPlayer p,List<BaseContainerBlockEntity> parts){return parts.stream().allMatch(b->{var o=((OwnedStorage)b).stow$owner();return o==null||o.id().equals(p.getUUID().toString())||p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);});}
 public static Result apply(ServerPlayer p,Request request){
  var level=p.level();var pos=request.pos();String dim=level.dimension().identifier().toString();var empty=new Owner("","",false);
  if(request.action()<QUERY||request.action()>PRIVATE||p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64||!level.hasChunkAt(pos))return new Result(dim,pos,empty,false,UNAVAILABLE);
  var parts=parts(level,pos);if(parts.isEmpty()||parts.stream().anyMatch(b->!b.canOpen(p)))return new Result(dim,pos,empty,false,UNAVAILABLE);
  var menu=p.containerMenu;boolean open=menu!=p.inventoryMenu&&menu.stillValid(p)&&!menu.slots.isEmpty()&&parts.stream().allMatch(b->{var c=menu.getSlot(0).container;return c==b||c instanceof CompoundContainer both&&both.contains(b);});
  boolean edit=open&&!p.isSpectator()&&parts.stream().allMatch(b->p.mayInteract(level,b.getBlockPos()))&&allowed(p,parts);
  if(request.action()!=QUERY){if(!edit)return new Result(dim,pos,combined(parts),false,DENIED);
   for(var b:parts){var old=((OwnedStorage)b).stow$owner();String id=old==null?p.getUUID().toString():old.id(),name=old==null?p.getName().getString():old.name();if(request.action()==CLAIM){id=p.getUUID().toString();name=p.getName().getString();}((OwnedStorage)b).stow$owner(new Owner(id,name,request.action()==SHARE));}
  }
  return new Result(dim,pos,combined(parts),edit,OK);
 }
}
