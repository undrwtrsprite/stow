package dev.stow.companion.protocol;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Only identifies the open offer; prices, stacks and stock always come from the server. */
public final class MerchantTradeProtocol {
    public static final int MAX_BATCHES=128;
    private static boolean registered;
    public record Request(int id,int menuId,int stateId,int offer) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("stow:merchant_trade_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=CustomPacketPayload.codec(
            (p,b)->{b.writeVarInt(p.id);b.writeVarInt(p.menuId);b.writeVarInt(p.stateId);b.writeVarInt(p.offer);},
            b->new Request(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt()));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Result(int id,int batches) implements CustomPacketPayload {
        public static final Type<Result> TYPE=new Type<>(Identifier.parse("stow:merchant_trade_result_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC=CustomPacketPayload.codec(
            (p,b)->{b.writeVarInt(p.id);b.writeVarInt(p.batches);},b->new Result(b.readVarInt(),b.readVarInt()));
        @Override public Type<Result> type(){return TYPE;}
    }
    public static synchronized void register(){
        if(registered)return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Result.TYPE,Result.CODEC);
        registered=true;
    }
    private MerchantTradeProtocol(){}
}
