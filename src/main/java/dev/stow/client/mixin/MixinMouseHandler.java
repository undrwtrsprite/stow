package dev.stow.client.mixin;
import dev.stow.client.input.StowShortcuts;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler {
    @Inject(method="onButton",at=@At("HEAD"),cancellable=true)
    private void stow_shortcut(long window,MouseButtonInfo event,int action,CallbackInfo ci){
        if(action==1&&window==Minecraft.getInstance().getWindow().handle()&&StowShortcuts.global(StowShortcuts.chord(event)))ci.cancel();
    }
}
