package dev.stow.client.ui;

import dev.stow.Stow;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Action feedback is extracted after screens, so their blur never covers it. */
public final class UiNotifications {
    private record Notice(Component message,long since,int duration) {}
    private static Notice current;
    private UiNotifications(){}
    public static void register(){
        ScreenEvents.AFTER_INIT.register((client,screen,width,height)->ScreenEvents.afterExtract(screen).register((s,g,x,y,delta)->draw(g)));
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("stow","action_feedback"),(g,delta)->{var mc=Minecraft.getInstance();if(mc.player!=null&&mc.gui.screen()==null&&!mc.gui.hud.isHidden())draw(g);});
    }
    public static void show(String key,Object... values){show(Component.translatable(key,values));}
    public static void show(Component message){
        if(!Stow.config.notifications)return;
        long now=System.currentTimeMillis();
        if(current!=null&&now-current.since<current.duration&&current.message.getString().equals(message.getString()))return;
        current=new Notice(message.copy(),now,duration(message));
    }
    private static int duration(Component message){
        String key=message.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translated?translated.getKey():"";
        if(key.contains("glow")||key.equals("stow.need.found-source"))return 1100;
        if(key.equals("stow.feedback.sort-done"))return 1200;
        if(key.contains("no-source")||key.contains("no-spare")||key.contains("protected")||key.contains("timeout")||key.contains("stopped")||key.contains("failed"))return 2800;
        return 1800;
    }
    public static void clear(){current=null;}
    public static void draw(GuiGraphicsExtractor g){
        if(!Stow.config.notifications){clear();return;}
        long now=System.currentTimeMillis();if(current!=null&&now-current.since>=current.duration)current=null;
        if(current==null)return;
        var font=Minecraft.getInstance().font;int width=Math.min(Math.min(260,g.guiWidth()-8),Math.max(100,font.width(current.message)+38)),textWidth=width-34;
        var lines=font.split(current.message,textWidth);int height=Math.max(24,Math.min(3,lines.size())*9+12);
        float entering=Math.clamp((now-current.since)/140f,0,1);int slide=Math.round(5*(1-entering)*(1-entering));
        int x=g.guiWidth()-width-4,y=4-slide;
        g.fill(x,y,x+width,y+height,0xF0202020);g.outline(x,y,width,height,0xFF888888);
        UIIcons.draw(g,UIIcons.Kind.COPY,x+5,y+(height-16)/2,0xFFFFFFFF);
        for(int i=0;i<Math.min(3,lines.size());i++)g.text(font,lines.get(i),x+27,y+6+i*9,0xFFFFFFFF);
    }
}
