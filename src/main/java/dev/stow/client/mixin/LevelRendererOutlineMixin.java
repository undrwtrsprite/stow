package dev.stow.client.mixin;
import dev.stow.client.memory.NativeChestOutline;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LevelRenderer.class)
public abstract class LevelRendererOutlineMixin {
    @Shadow @Final private BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    @Inject(method="submitFeatures",at=@At("HEAD"))
    private void stow$submitOutline(LevelRenderState frame,SubmitNodeCollector collector,boolean blockOutline,CallbackInfo ci){
        if(collector instanceof SubmitNodeStorage storage)NativeChestOutline.submit(frame,storage,blockEntityRenderDispatcher);
    }
}
