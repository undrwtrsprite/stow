package dev.stow.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reuse vanilla camera effects without changing world rendering or depth. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("bobHurt") void stow$bobHurt(CameraRenderState camera, PoseStack pose);
    @Invoker("bobView") void stow$bobView(CameraRenderState camera, PoseStack pose);
}
