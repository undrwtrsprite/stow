package dev.stow.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAttackAccessor {
    @Invoker("continueAttack") void stow_continueAttack(boolean held);
}
