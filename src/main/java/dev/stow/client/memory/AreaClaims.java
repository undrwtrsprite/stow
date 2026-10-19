package dev.stow.client.memory;
import java.util.*;
import dev.stow.companion.protocol.AreaClaimProtocol;
import dev.stow.companion.protocol.AreaClaimProtocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
public final class AreaClaims {
 private static int next;private static ChestMemoryStore store;private static String dimension;private static Result result;private static int requestId;
 public static void register(){AreaClaimProtocol.register();ClientPlayNetworking.registerGlobalReceiver(Result.TYPE,(r,c)->c.client().execute(()->{
  if(r.id()!=requestId||store==null||store!=ChestMemory.currentStore()||!r.dimension().equals(dimension))return;result=r;
  StorageOwners.areaClaimed(store,r.dimension(),r.positions(),r.owner());
  if(r.status()!=AreaClaimProtocol.RUNNING)dev.stow.client.ui.UiNotifications.show(r.status()==AreaClaimProtocol.DONE?"stow.area.done":r.status()==AreaClaimProtocol.CANCELLED?"stow.area.cancelled":"stow.area.rejected",r.claimed());
 }));}
 public static boolean available(){var mc=Minecraft.getInstance();return mc.getConnection()!=null&&ClientPlayNetworking.canSend(Request.TYPE);}
 public static Result result(){return result;}
 public static boolean running(){return result!=null&&result.status()==AreaClaimProtocol.RUNNING;}
 public static boolean validSelection(BlockPos a,BlockPos b){var p=Minecraft.getInstance().player;if(p==null||a==null||b==null||AreaClaimProtocol.volume(a,b)>AreaClaimProtocol.MAX_VOLUME)return false;var center=p.blockPosition();for(var pos:List.of(a,b))if(Math.abs((long)pos.getX()-center.getX())>64||Math.abs((long)pos.getY()-center.getY())>64||Math.abs((long)pos.getZ()-center.getZ())>64)return false;return true;}
 public static void start(ChestMemoryStore target,BlockPos a,BlockPos b){var mc=Minecraft.getInstance();if(!available()||running()||mc.level==null||!validSelection(a,b))return;store=target;dimension=mc.level.dimension().identifier().toString();requestId=++next;result=new Result(requestId,dimension,AreaClaimProtocol.RUNNING,0,(int)AreaClaimProtocol.volume(a,b),0,0,new dev.stow.companion.protocol.StorageOwnerProtocol.Owner("","",false),List.of());ClientPlayNetworking.send(new Request(requestId,a,b,false));}
 public static void cancel(){if(running()&&available())ClientPlayNetworking.send(new Request(requestId,BlockPos.ZERO,BlockPos.ZERO,true));}
 public static void clear(){store=null;dimension=null;result=null;AreaSelection.stop(false);}
}
