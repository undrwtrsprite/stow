package porttest;

import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Loaded only by the stripping test mod, never by the shipped stow JAR. */
@Mixin(ServerPlayerGameMode.class)
public abstract class StripServerDenyMixin {
    @Inject(method="useItemOn",at=@At("HEAD"),cancellable=true)
    private void strip_test_reject(ServerPlayer player,Level level,ItemStack item,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){
        StripWorldTest.requests++;
        if(StripWorldTest.reject)ci.setReturnValue(InteractionResult.FAIL);
    }
}
