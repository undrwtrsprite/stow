package dev.stow.client.mixin;
import dev.stow.client.input.StowShortcuts;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class MixinKeyboardHandler {
    @Inject(method="keyPress",at=@At("HEAD"),cancellable=true)
    private void stow_shortcut(long window,int action,KeyEvent event,CallbackInfo ci){
        if(action==1&&window==Minecraft.getInstance().getWindow().handle()&&StowShortcuts.global(StowShortcuts.chord(event)))ci.cancel();
    }
}
