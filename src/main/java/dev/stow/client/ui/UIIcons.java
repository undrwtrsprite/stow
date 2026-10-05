package dev.stow.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Downloaded Material Design Icons, Apache 2.0. Sources and provenance: design/icons/mdi. */
public final class UIIcons {
    public static final int SIZE=16;
    private static final int TEXTURE_SIZE=64;
    public enum Kind {
        STORAGE("treasure-chest"), MATERIALS("clipboard-list"), SORT("sort-descending"),
        COMMAND("lightning-bolt"), DEPOSIT("tray-arrow-down"), EYE("eye"), EYE_OFF("eye-off"),
        STAR("star"), COPY("layers-triple"), GRID("select-all"), BOTH("gesture-swipe-horizontal"),
        CHECK("check"), CLOSE("close"), PLUS("plus"), MINUS("minus"),
        LEFT("chevron-left"), RIGHT("chevron-right"), PROJECT("folder-multiple"),
        KEEP("shield-check"), KEYBOARD("keyboard"), SETTINGS("cog"), EQUIPMENT("pickaxe"),
        TRACK("clipboard-plus"), FIND("magnify");
        private final Identifier texture;
        Kind(String name){texture=Identifier.fromNamespaceAndPath("stow","textures/gui/icons/"+name+".png");}
        public Identifier texture(){return texture;}
    }
    private UIIcons(){}
    public static void draw(GuiGraphicsExtractor g,Kind kind,int x,int y,int color){
        g.blit(RenderPipelines.GUI_TEXTURED,kind.texture(),x,y,0,0,SIZE,SIZE,TEXTURE_SIZE,TEXTURE_SIZE,TEXTURE_SIZE,TEXTURE_SIZE,color);
    }
}
