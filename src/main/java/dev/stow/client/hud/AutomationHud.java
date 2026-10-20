package dev.stow.client.hud;

import dev.stow.Stow;
import dev.stow.client.input.AttackAutomation;
import dev.stow.client.inventory.AutoMending;
import dev.stow.client.inventory.BulkStrip;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Redesign v2 "Automation status": one small line per running automation (auto tool, held
 * attack, auto mending, bulk stripping) so a running toggle is never invisible. Read-only.
 */
public final class AutomationHud {
    private AutomationHud(){}
    public static void register(){HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("stow","automation_status"),(g,delta)->{
        var mc=Minecraft.getInstance();if(mc.player==null||mc.gui.screen()!=null||mc.gui.hud.isHidden()||!Stow.config.automationHud)return;
        var rows=rows();if(rows.isEmpty())return;
        var c=Stow.config;
        SurvivalDock.drawRows(g,rows,null,c.automationCorner,c.automationOffsetX,c.automationOffsetY,c.automationScale,c.automationAlignment);
    });}
    public static List<SurvivalDock.Row> rows(){
        var rows=new ArrayList<SurvivalDock.Row>();
        if(Stow.config.autoTool)rows.add(new SurvivalDock.Row(new ItemStack(Items.IRON_PICKAXE),Component.translatable("stow.hud.automation.tool").getString(),0xFFEFE6CF));
        if(AttackAutomation.active())rows.add(new SurvivalDock.Row(new ItemStack(Items.IRON_SWORD),Component.translatable("stow.hud.automation.attack").getString(),0xFFD9A441));
        if(AutoMending.active())rows.add(new SurvivalDock.Row(new ItemStack(Items.EXPERIENCE_BOTTLE),Component.translatable("stow.hud.automation.mend").getString(),0xFF8FB58A));
        if(BulkStrip.busy())rows.add(new SurvivalDock.Row(new ItemStack(Items.IRON_AXE),Component.translatable("stow.hud.automation.strip").getString(),0xFFD9A441));
        return rows;
    }
    /** Preview rows for the Arrange HUD editor when nothing is running. */
    public static List<SurvivalDock.Row> previewRows(){
        var live=rows();if(!live.isEmpty())return live;
        return List.of(new SurvivalDock.Row(new ItemStack(Items.IRON_PICKAXE),Component.translatable("stow.hud.automation.tool").getString(),0xFFEFE6CF),
            new SurvivalDock.Row(new ItemStack(Items.IRON_SWORD),Component.translatable("stow.hud.automation.attack").getString(),0xFFD9A441));
    }
}
