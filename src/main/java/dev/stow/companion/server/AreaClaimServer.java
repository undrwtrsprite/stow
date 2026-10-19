package dev.stow.companion.server;
import java.util.*;
import dev.stow.companion.protocol.AreaClaimProtocol;
import dev.stow.companion.protocol.AreaClaimProtocol.*;
import static dev.stow.companion.protocol.AreaClaimProtocol.*;
import dev.stow.companion.protocol.StorageOwnerProtocol.Owner;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.ChestType;
/** One explicit finite job; no chunk loading, world registry or background ownership polling. */
public final class AreaClaimServer implements ModInitializer {
 private final Map<UUID,Job> jobs=new LinkedHashMap<>();
 private final Map<UUID,Integer> lastStart=new HashMap<>();
 public void onInitialize(){AreaClaimProtocol.register();ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(r,c)->c.server().execute(()->{
  var p=c.player();if(r.cancel()){var j=jobs.get(p.getUUID());if(j!=null&&j.id==r.id()){jobs.remove(p.getUUID());send(p,j.result(CANCELLED));}return;}
  int last=lastStart.getOrDefault(p.getUUID(),p.tickCount-20);if(jobs.containsKey(p.getUUID())||jobs.size()>=8||p.tickCount-last<20||!valid(p,r)){send(p,new Result(r.id(),p.level().dimension().identifier().toString(),REJECTED,0,0,0,0,owner(p),List.of()));return;}
  lastStart.put(p.getUUID(),p.tickCount);var job=new Job(p,r);jobs.put(p.getUUID(),job);send(p,job.result(RUNNING));
 }));
 ServerTickEvents.END_SERVER_TICK.register(server->{if(jobs.isEmpty())return;int positions=512/jobs.size(),containers=Math.max(1,8/jobs.size());for(var entry:List.copyOf(jobs.entrySet())){var j=entry.getValue();var p=server.getPlayerList().getPlayer(entry.getKey());if(p==null){jobs.remove(entry.getKey());continue;}int status=RUNNING;if(p.level()!=j.level||p.isSpectator()||!p.isAlive()||p.position().distanceToSqr(j.origin)>64)status=CANCELLED;else{j.step(positions,containers);if(j.scanned>=j.total)status=DONE;}
  if(status!=RUNNING||j.updates.size()>=MAX_BATCH-2||++j.ticks>=20){j.ticks=0;send(p,j.result(status));}if(status!=RUNNING)jobs.remove(entry.getKey());
 }});
 ServerPlayConnectionEvents.DISCONNECT.register((h,s)->{jobs.remove(h.player.getUUID());lastStart.remove(h.player.getUUID());});ServerLifecycleEvents.SERVER_STOPPED.register(s->{jobs.clear();lastStart.clear();});}
 private static void send(ServerPlayer p,Result result){if(ServerPlayNetworking.canSend(p,Result.TYPE))ServerPlayNetworking.send(p,result);}
 private static Owner owner(ServerPlayer p){return new Owner(p.getUUID().toString(),p.getName().getString(),false);}
 public static boolean valid(ServerPlayer p,Request r){if(p.isSpectator()||!p.isAlive()||volume(r.first(),r.second())>MAX_VOLUME)return false;var center=p.blockPosition();for(var pos:List.of(r.first(),r.second()))if(Math.abs((long)pos.getX()-center.getX())>MAX_DISTANCE||Math.abs((long)pos.getY()-center.getY())>MAX_DISTANCE||Math.abs((long)pos.getZ()-center.getZ())>MAX_DISTANCE)return false;return true;}
 public static final class Job {
  public final int id,total;public int scanned,claimed,skipped;private int ticks;
  private final ServerPlayer player;private final ServerLevel level;private final net.minecraft.world.phys.Vec3 origin;private final Owner owner;
  private final BlockPos min,max;private final int sx,sz;private final List<BlockPos> updates=new ArrayList<>();
  public Job(ServerPlayer p,Request r){if(!valid(p,r))throw new IllegalArgumentException("Invalid claim area");player=p;level=p.level();origin=p.position();owner=owner(p);id=r.id();min=new BlockPos(Math.min(r.first().getX(),r.second().getX()),Math.min(r.first().getY(),r.second().getY()),Math.min(r.first().getZ(),r.second().getZ()));max=new BlockPos(Math.max(r.first().getX(),r.second().getX()),Math.max(r.first().getY(),r.second().getY()),Math.max(r.first().getZ(),r.second().getZ()));sx=max.getX()-min.getX()+1;sz=max.getZ()-min.getZ()+1;total=(int)volume(min,max);}
  private boolean inside(BlockPos pos){return pos.getX()>=min.getX()&&pos.getX()<=max.getX()&&pos.getY()>=min.getY()&&pos.getY()<=max.getY()&&pos.getZ()>=min.getZ()&&pos.getZ()<=max.getZ();}
  /** Stops before a bounded reply buffer fills; caller can flush it and continue next tick. */
  public void step(int positions,int containers){int end=Math.min(total,scanned+Math.min(512,Math.max(0,positions))),changed=0;while(scanned<end&&changed<containers&&updates.size()<MAX_BATCH-2){int i=scanned++;var pos=min.offset(i%sx,i/(sx*sz),(i/sx)%sz);var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);if(chunk==null)continue;var be=chunk.getBlockEntity(pos);if(!StorageOwnerServer.supported(be))continue;var state=level.getBlockState(pos);var parts=new ArrayList<BaseContainerBlockEntity>();parts.add((BaseContainerBlockEntity)be);
   if(state.getBlock() instanceof ChestBlock&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){if(state.getValue(ChestBlock.TYPE)==ChestType.RIGHT)continue;var other=ChestBlock.getConnectedBlockPos(pos,state);var otherChunk=level.getChunkSource().getChunkNow(other.getX()>>4,other.getZ()>>4);var partner=otherChunk==null?null:otherChunk.getBlockEntity(other);if(!inside(other)||!(partner instanceof ChestBlockEntity)||!level.getBlockState(other).is(state.getBlock())){skipped++;continue;}parts.add((BaseContainerBlockEntity)partner);}
   if(parts.stream().anyMatch(b->((OwnedStorage)b).stow$owner()!=null||!b.canOpen(player)||!player.mayInteract(level,b.getBlockPos()))){skipped++;continue;}
   for(var b:parts){((OwnedStorage)b).stow$owner(owner);updates.add(b.getBlockPos().immutable());}claimed++;changed++;
  }}
  public Result result(int status){var result=new Result(id,level.dimension().identifier().toString(),status,scanned,total,claimed,skipped,owner,List.copyOf(updates));updates.clear();return result;}
 }
}
