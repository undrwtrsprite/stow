package dev.stow.client.mixin;

import dev.stow.client.inventory.StowDragHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mouse Tweaks runs before the screen's drag handler through Fabric's allow event. */
@Pseudo
@Mixin(targets = "yalter.mousetweaks.Main", remap = false)
public abstract class MixinMouseTweaks {
    @Inject(method = "onMouseDrag", at = @At("HEAD"), cancellable = true, require = 0)
    private static void stow_yieldDrag(Screen screen, double x, double y, @Coerce Object button,
                                     CallbackInfoReturnable<Boolean> cir) {
        if (screen instanceof StowDragHandler handler && handler.stow$ownsDrag()) {
            // False means Mouse Tweaks did not handle it; Fabric can deliver it to Stow.
            cir.setReturnValue(false);
        }
    }
}
