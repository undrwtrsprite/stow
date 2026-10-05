package dev.stow.stripping.protocol;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Shared wire format only: neither side loads the other's initializer. */
public final class OakHarvestProtocol {
    private static boolean registered;
    public record Request(int id, BlockPos anchor, int slot, boolean cancel) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("stow:oak_harvest_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=CustomPacketPayload.codec(
            (p,b)->{b.writeVarInt(p.id);b.writeBlockPos(p.anchor);b.writeVarInt(p.slot);b.writeBoolean(p.cancel);},
            b->new Request(b.readVarInt(),b.readBlockPos(),b.readVarInt(),b.readBoolean()));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Result(int id, String status, int stripped, int mined) implements CustomPacketPayload {
        public static final Type<Result> TYPE=new Type<>(Identifier.parse("stow:oak_harvest_result_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC=CustomPacketPayload.codec(
            (p,b)->{b.writeVarInt(p.id);b.writeUtf(p.status,32);b.writeVarInt(p.stripped);b.writeVarInt(p.mined);},
            b->new Result(b.readVarInt(),b.readUtf(32),b.readVarInt(),b.readVarInt()));
        @Override public Type<Result> type(){return TYPE;}
    }
    public static synchronized void register(){
        if(registered)return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Result.TYPE,Result.CODEC);
        registered=true;
    }
    private OakHarvestProtocol(){}
}
