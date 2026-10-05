package dev.stow.client.mixin;
import dev.stow.client.memory.ChestMemory;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    @Inject(method="handleContainerSetSlot",at=@At("TAIL"))
    private void stow_slot(ClientboundContainerSetSlotPacket packet,CallbackInfo ci){ChestMemory.slotReceived(packet.getContainerId());}
    @Inject(method="handleContainerContent",at=@At("TAIL"))
    private void stow_content(ClientboundContainerSetContentPacket packet,CallbackInfo ci){ChestMemory.contentReceived(packet.containerId());}
    @Inject(method="handleBlockChangedAck",at=@At("TAIL"))
    private void stow_stripped(ClientboundBlockChangedAckPacket packet,CallbackInfo ci){
        dev.stow.client.inventory.BulkStrip.acknowledged(net.minecraft.client.Minecraft.getInstance(),packet.sequence());
    }
}
