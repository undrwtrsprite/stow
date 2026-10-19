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
        if(window==Minecraft.getInstance().getWindow().handle()&&dev.stow.client.memory.AreaSelection.mouse(action,event.button())){ci.cancel();return;}
        if(action==1&&window==Minecraft.getInstance().getWindow().handle()&&StowShortcuts.global(StowShortcuts.chord(event)))ci.cancel();
    }
    @Inject(method="onScroll",at=@At("HEAD"),cancellable=true)
    private void stow_cameraScroll(long window,double xOffset,double yOffset,CallbackInfo ci){if(window==Minecraft.getInstance().getWindow().handle()&&dev.stow.client.memory.AreaSelection.scroll(yOffset!=0?yOffset:-xOffset))ci.cancel();}
    @Redirect(method="turnPlayer",at=@At(value="INVOKE",target="Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void stow_cameraLook(net.minecraft.client.player.LocalPlayer player,double dx,double dy){if(!dev.stow.client.memory.AreaSelection.turn(dx,dy))player.turn(dx,dy);}
}
