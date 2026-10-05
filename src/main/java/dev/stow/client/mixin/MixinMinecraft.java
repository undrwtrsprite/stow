package dev.stow.client.mixin;
import dev.stow.client.inventory.ToolPicker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Inject(method="pickBlockOrEntity",at=@At("HEAD"),cancellable=true)
    private void stow_pickTool(CallbackInfo ci){if(ToolPicker.pick((Minecraft)(Object)this))ci.cancel();}
}
