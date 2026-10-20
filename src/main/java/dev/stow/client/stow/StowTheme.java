package dev.stow.client.stow;

import java.text.NumberFormat;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Redesign v2 palette (docs/design/redesign-v2.md §6) and small drawing helpers. */
public final class StowTheme {
    public static final int PANEL=0xC02E3033,RAISED=0xFF3E4144,RAISED_SOFT=0x803E4144,LINE=0xFF4A4C4F;
    public static final int CREAM=0xFFEFE6CF,MUTED=0xFFA8A39A,DIM=0xFF78756E;
    public static final int GOLD=0xFFC9A548,SAGE=0xFF8FB58A,OCHRE=0xFFD9A441,CLAY=0xFFC7705F;
    public static final int SELECTED=0xFF45412F,HOVER=0x40FFFFFF;
    private StowTheme(){}
    public static Font font(){return Minecraft.getInstance().font;}
    /** Locale grouping (3,352 / 3.352), following the game language rather than the OS. */
    public static String number(long value){
        String code=Minecraft.getInstance().getLanguageManager().getSelected();
        Locale locale=code!=null&&code.startsWith("de")?Locale.GERMANY:Locale.US;
        return NumberFormat.getIntegerInstance(locale).format(value);
    }
    public static String fit(String text,int width){
        var font=font();if(font.width(text)<=width)return text;
        return font.plainSubstrByWidth(text,Math.max(0,width-font.width("…")))+"…";
    }
    public static void text(GuiGraphicsExtractor g,String text,int x,int y,int width,int color){g.text(font(),fit(text,width),x,y,color);}
    public static void right(GuiGraphicsExtractor g,String text,int right,int y,int color){g.text(font(),text,right-font().width(text),y,color);}
    /** Selection uses three cues: raised fill, a 2-px gold bar and (keyboard) a focus ring. */
    public static void row(GuiGraphicsExtractor g,int x,int y,int w,int h,boolean selected,boolean hovered,boolean focusRing){
        if(selected){g.fill(x,y,x+w,y+h,SELECTED);g.fill(x,y,x+2,y+h,GOLD);if(focusRing)g.outline(x,y,w,h,GOLD);}
        else if(hovered)g.fill(x,y,x+w,y+h,HOVER);
    }
    public static void panel(GuiGraphicsExtractor g,int x,int y,int w,int h){g.fill(x,y,x+w,y+h,PANEL);g.outline(x,y,w,h,LINE);}
    public static void card(GuiGraphicsExtractor g,int x,int y,int w,int h){g.fill(x,y,x+w,y+h,RAISED_SOFT);g.outline(x,y,w,h,LINE);}
    /** Section labels are small caps in gold. */
    public static void section(GuiGraphicsExtractor g,String label,int x,int y){
        var font=font();g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(.75f,.75f);g.text(font,label.toUpperCase(Locale.ROOT),0,0,GOLD);g.pose().popMatrix();
    }
    public static void progress(GuiGraphicsExtractor g,int x,int y,int w,long have,long need){
        g.fill(x,y,x+w,y+2,0xFF2A2B2D);long clamped=need<=0?w:Math.min(w,have*w/Math.max(1,need));
        g.fill(x,y,x+(int)clamped,y+2,have>=need?SAGE:GOLD);
    }
    public static String distance(double blocks){return blocks<0?"—":blocks<1000?Math.round(blocks)+" m":String.format(Locale.ROOT,"%.1f km",blocks/1000);}
    public static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
}
