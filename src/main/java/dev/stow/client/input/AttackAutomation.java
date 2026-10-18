package dev.stow.client.input;

import dev.stow.Stow;
import dev.stow.StowConfig.AttackMode;
import dev.stow.client.mixin.KeyMappingAccessor;
import dev.stow.client.mixin.MinecraftAttackAccessor;
import dev.stow.client.ui.UiNotifications;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Supplies the normal Attack binding; vanilla still owns mining, combat and cooldowns. */
public final class AttackAutomation {
    private static AttackMode active;
    private static long nextClick;
    private static boolean first;
    private AttackAutomation(){}
    public static boolean active(){return active!=null;}
    public static boolean toggle(Minecraft mc){
        if(active()){stop(mc);UiNotifications.show("stow.attack.off");return true;}
        if(!available(mc))return false;
        active=Stow.config.attackMode;first=true;nextClick=0;
        UiNotifications.show(active==AttackMode.HOLD?Component.translatable("stow.attack.hold-on"):
                Component.translatable("stow.attack.interval-on",Math.clamp(Stow.config.attackIntervalMs,50,60_000)));
        return true;
    }
    public static void stop(Minecraft mc){
        if(active==AttackMode.HOLD&&mc.options!=null)mc.options.keyAttack.setDown(false);
        active=null;first=false;nextClick=0;
    }
    private static boolean available(Minecraft mc){
        return mc.player!=null&&mc.level!=null&&mc.gameMode!=null&&mc.player.isAlive()&&!mc.player.isSpectator()
                &&mc.gui.screen()==null&&mc.gui.overlay()==null&&!mc.isPaused()&&mc.isWindowActive();
    }
    public static void tick(Minecraft mc){if(active()&&!available(mc))stop(mc);}
    /** Called immediately before vanilla consumes key presses, never from a timer thread. */
    public static void prepare(Minecraft mc){
        if(!active())return;
        if(!available(mc)){stop(mc);return;}
        // A menu leaves vanilla's click suppression armed until Attack is released.
        // Begin with the same release/press cycle, including when started from the palette.
        if(first)((MinecraftAttackAccessor)mc).stow_continueAttack(false);
        if(active==AttackMode.HOLD)mc.options.keyAttack.setDown(true);
        if(mc.player.isUsingItem())return;
        long now=System.nanoTime();
        if(first||active==AttackMode.INTERVAL&&now>=nextClick){
            var attack=(KeyMappingAccessor)mc.options.keyAttack;
            attack.stow_setClicks(attack.stow_clicks()+1);
            first=false;nextClick=now+Math.clamp(Stow.config.attackIntervalMs,50,60_000)*1_000_000L;
        }
    }
}
