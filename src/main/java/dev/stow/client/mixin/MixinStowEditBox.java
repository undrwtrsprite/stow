package dev.stow.client.mixin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(EditBox.class)
public abstract class MixinStowEditBox {
 @Redirect(method="extractWidgetRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/components/TextCursorUtils;extractAppendCursor(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIIZ)V"))
 private void stow_appendCaret(GuiGraphicsExtractor g,Font font,int x,int y,int color,boolean shadow){if((Object)this instanceof dev.stow.client.ui.StowEditBox)TextCursorUtils.extractInsertCursor(g,x,y,color,font.lineHeight+1);else TextCursorUtils.extractAppendCursor(g,font,x,y,color,shadow);}
}
