package dev.stow.client.mixin;

import dev.stow.client.memory.MaterialImportScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.MultiLineEditBox;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Only Stow's paste editor eases wheel scrolling; cursor and scrollbar moves stay immediate. */
@Mixin(AbstractScrollArea.class)
public abstract class MixinAbstractScrollArea {
    @Shadow private double scrollAmount;
    @Shadow protected abstract double scrollRate();
    @Unique private double stow_scrollFrom;
    @Unique private long stow_scrollStarted;
    @Unique private boolean stow_wheel;
    @Unique private boolean stow_pasteEditor(){return (Object)this instanceof MultiLineEditBox&&Minecraft.getInstance().gui.screen() instanceof MaterialImportScreen;}
    @Inject(method="mouseScrolled",at=@At("HEAD"),cancellable=true)
    private void stow_smoothPaste(double x,double y,double horizontal,double vertical,CallbackInfoReturnable<Boolean> ci){
        var area=(AbstractScrollArea)(Object)this;
        if(!stow_pasteEditor()||!area.isMouseOver(x,y)||vertical==0)return;
        double visible=area.scrollAmount();stow_wheel=true;
        try{area.setScrollAmount(scrollAmount-vertical*scrollRate());}finally{stow_wheel=false;}
        stow_scrollFrom=visible;stow_scrollStarted=System.nanoTime();ci.setReturnValue(true);
    }
    @Inject(method="setScrollAmount",at=@At("HEAD"))
    private void stow_directScroll(double amount,CallbackInfo ci){if(!stow_wheel)stow_scrollStarted=0;}
    @Inject(method="scrollAmount",at=@At("RETURN"),cancellable=true)
    private void stow_visibleScroll(CallbackInfoReturnable<Double> ci){
        if(stow_scrollStarted==0||!stow_pasteEditor())return;
        double t=Math.clamp((System.nanoTime()-stow_scrollStarted)/160_000_000d,0,1);
        ci.setReturnValue(scrollAmount+(stow_scrollFrom-scrollAmount)*(1-t)*(1-t)*(1-t));
    }
}
