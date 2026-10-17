package dev.stow.companion.protocol;

import io.netty.handler.codec.DecoderException;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Bounded, versioned sort proposal. The server verifies the original contents and item conservation. */
public final class ChestSortProtocol {
    public static final int MAX_SLOTS=54,MAX_BYTES=24*1024;
    public static final int DONE=0,FALLBACK=1,CHANGED=2,BUSY=3;
    private static boolean registered;
    public record Change(int slot,ItemStack before,ItemStack after) {
        public Change { before=before.copy();after=after.copy(); }
    }
    public record Request(int id,int menuId,int stateId,List<Change> changes) implements CustomPacketPayload {
        public Request { changes=List.copyOf(changes); }
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("stow:chest_sort_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=CustomPacketPayload.codec(
            (p,b)->{
                if(p.changes.isEmpty()||p.changes.size()>MAX_SLOTS)throw new IllegalArgumentException("Invalid sort size");
                b.writeVarInt(p.id);b.writeVarInt(p.menuId);b.writeVarInt(p.stateId);b.writeVarInt(p.changes.size());
                for(var c:p.changes){b.writeVarInt(c.slot);ItemStack.OPTIONAL_STREAM_CODEC.encode(b,c.before);ItemStack.OPTIONAL_STREAM_CODEC.encode(b,c.after);}
            },b->{
                if(b.readableBytes()>MAX_BYTES)throw new DecoderException("Sort request too large");
                int id=b.readVarInt(),menu=b.readVarInt(),state=b.readVarInt(),size=b.readVarInt();
                if(size<1||size>MAX_SLOTS)throw new DecoderException("Invalid sort size");
                var changes=new ArrayList<Change>(size);
                for(int i=0;i<size;i++)changes.add(new Change(b.readVarInt(),ItemStack.OPTIONAL_STREAM_CODEC.decode(b),ItemStack.OPTIONAL_STREAM_CODEC.decode(b)));
                return new Request(id,menu,state,changes);
            });
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Result(int id,int status) implements CustomPacketPayload {
        public static final Type<Result> TYPE=new Type<>(Identifier.parse("stow:chest_sort_result_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Result> CODEC=CustomPacketPayload.codec(
            (p,b)->{b.writeVarInt(p.id);b.writeVarInt(p.status);},b->new Result(b.readVarInt(),b.readVarInt()));
        @Override public Type<Result> type(){return TYPE;}
    }
    public static synchronized void register(){
        if(registered)return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Result.TYPE,Result.CODEC);registered=true;
    }
    private ChestSortProtocol(){}
}
