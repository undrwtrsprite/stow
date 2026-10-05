package dev.stow.client.mixin;
import dev.stow.client.memory.NativeChestOutline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorOutlineMixin {
    @Shadow @Final private LevelRenderState levelRenderState;
    @Shadow @Final private Minecraft minecraft;
    @Inject(method="extract",at=@At("TAIL"))
    private void stow$extractOutline(DeltaTracker delta,Camera camera,float partial,CallbackInfo ci){NativeChestOutline.extract(minecraft,levelRenderState);}
}
