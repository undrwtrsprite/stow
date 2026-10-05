package dev.stow.client.mixin;
import dev.stow.client.memory.ChestMemory;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.*;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {
    @Inject(method={"interact","useItem"},at=@At("HEAD"))
    private void stow_clear(CallbackInfoReturnable<InteractionResult> ci){ChestMemory.clearPending();}
    @Inject(method="useItemOn",at=@At("HEAD"))
    private void stow_clicked(LocalPlayer player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){ChestMemory.clicked(hit);}
}
