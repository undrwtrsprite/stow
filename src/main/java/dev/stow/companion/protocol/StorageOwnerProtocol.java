package dev.stow.companion.protocol;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** Small, versioned label requests; never transports inventory contents. */
public final class StorageOwnerProtocol {
 public static final int QUERY=0,CLAIM=1,SHARE=2,PRIVATE=3,OK=0,UNAVAILABLE=1,DENIED=2;
 public record Owner(String id,String name,boolean shared) {}
 public record Request(BlockPos pos,int action) implements CustomPacketPayload {
  public static final Type<Request> TYPE=new Type<>(Identifier.parse("stow:storage_owner_v1"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=CustomPacketPayload.codec((p,b)->{b.writeBlockPos(p.pos);b.writeVarInt(p.action);},b->new Request(b.readBlockPos(),b.readVarInt()));
  public Type<Request> type(){return TYPE;}
 }
 public record Result(String dimension,BlockPos pos,Owner owner,boolean editable,int status) implements CustomPacketPayload {
  public static final Type<Result> TYPE=new Type<>(Identifier.parse("stow:storage_owner_result_v1"));
  public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC=CustomPacketPayload.codec((p,b)->{b.writeUtf(p.dimension,128);b.writeBlockPos(p.pos);b.writeUtf(p.owner.id,40);b.writeUtf(p.owner.name,48);b.writeBoolean(p.owner.shared);b.writeBoolean(p.editable);b.writeVarInt(p.status);},b->new Result(b.readUtf(128),b.readBlockPos(),new Owner(b.readUtf(40),b.readUtf(48),b.readBoolean()),b.readBoolean(),b.readVarInt()));
  public Type<Result> type(){return TYPE;}
 }
 private static boolean registered;
 public static synchronized void register(){if(registered)return;PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);PayloadTypeRegistry.clientboundPlay().register(Result.TYPE,Result.CODEC);registered=true;}
 private StorageOwnerProtocol(){}
}
