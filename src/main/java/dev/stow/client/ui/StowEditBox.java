package dev.stow.client.ui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
/** Shared hint positioning; editing, selection, IME and blink timing remain vanilla. */
public final class StowEditBox extends EditBox {
 private final Font stowFont;private Component stowHint;
 public StowEditBox(Font font,int x,int y,int width,int height,Component label){super(font,x,y,width,height,label);stowFont=font;}
 @Override public void setHint(Component hint){super.setHint(hint);stowHint=hint;}
 @Override public void extractWidgetRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){super.extractWidgetRenderState(g,mx,my,delta);if(stowHint!=null&&isFocused()&&getValue().isEmpty()){int padding=isBordered()?4:0;g.text(stowFont,stowFont.plainSubstrByWidth(stowHint.getString(),Math.max(0,getWidth()-padding*2-2)),getX()+padding+2,getY()+(isBordered()?(getHeight()-8)/2:0),0xFF777777);}}
}
