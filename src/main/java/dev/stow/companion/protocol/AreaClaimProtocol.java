package dev.stow.companion.protocol;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import dev.stow.companion.protocol.StorageOwnerProtocol.Owner;
public final class AreaClaimProtocol {
 public static final int RUNNING=0,DONE=1,CANCELLED=2,REJECTED=3,MAX_VOLUME=262144,MAX_DISTANCE=64,MAX_BATCH=32;
 public record Request(int id,BlockPos first,BlockPos second,boolean cancel) implements CustomPacketPayload {
  public static final Type<Request> TYPE=new Type<>(Identifier.parse("stow:area_claim_v1"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=CustomPacketPayload.codec((p,b)->{b.writeVarInt(p.id);b.writeBlockPos(p.first);b.writeBlockPos(p.second);b.writeBoolean(p.cancel);},b->new Request(b.readVarInt(),b.readBlockPos(),b.readBlockPos(),b.readBoolean()));
  public Type<Request> type(){return TYPE;}
 }
 public record Result(int id,String dimension,int status,int scanned,int total,int claimed,int skipped,Owner owner,List<BlockPos> positions) implements CustomPacketPayload {
  public Result{positions=List.copyOf(positions);}
  public static final Type<Result> TYPE=new Type<>(Identifier.parse("stow:area_claim_result_v1"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC=CustomPacketPayload.codec((p,b)->{if(p.positions.size()>MAX_BATCH)throw new IllegalArgumentException("Area batch too large");b.writeVarInt(p.id);b.writeUtf(p.dimension,128);b.writeVarInt(p.status);b.writeVarInt(p.scanned);b.writeVarInt(p.total);b.writeVarInt(p.claimed);b.writeVarInt(p.skipped);b.writeUtf(p.owner.id(),40);b.writeUtf(p.owner.name(),48);b.writeBoolean(p.owner.shared());b.writeVarInt(p.positions.size());for(var pos:p.positions)b.writeBlockPos(pos);},b->{int id=b.readVarInt();String dim=b.readUtf(128);int status=b.readVarInt(),scanned=b.readVarInt(),total=b.readVarInt(),claimed=b.readVarInt(),skipped=b.readVarInt();var owner=new Owner(b.readUtf(40),b.readUtf(48),b.readBoolean());int n=b.readVarInt();if(n<0||n>MAX_BATCH||total<0||total>MAX_VOLUME||scanned<0||scanned>total)throw new io.netty.handler.codec.DecoderException("Invalid area result");var positions=new ArrayList<BlockPos>(n);for(int i=0;i<n;i++)positions.add(b.readBlockPos());return new Result(id,dim,status,scanned,total,claimed,skipped,owner,positions);});
  public Type<Result> type(){return TYPE;}
 }
 public static long volume(BlockPos a,BlockPos b){long x=Math.abs((long)a.getX()-b.getX())+1,y=Math.abs((long)a.getY()-b.getY())+1,z=Math.abs((long)a.getZ()-b.getZ())+1;if(x>129||y>129||z>129)return Long.MAX_VALUE;return x*y*z;}
 private static boolean registered;
 public static synchronized void register(){if(registered)return;PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);PayloadTypeRegistry.clientboundPlay().register(Result.TYPE,Result.CODEC);registered=true;}
}
