package dev.stow.client.mixin;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Area-selection outlines are world markers: draw them even when F1 hides the HUD, beneath HUD elements. */
@Mixin(Hud.class)
public abstract class MixinHud {
    @Inject(method="extractRenderState",at=@At("HEAD"))
    private void stow_areaOutline(GuiGraphicsExtractor graphics,DeltaTracker delta,CallbackInfo ci){dev.stow.client.memory.AreaSelection.extractOutline(graphics);}
}
