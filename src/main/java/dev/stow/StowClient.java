package dev.stow;

import dev.stow.client.inventory.InventorySorting;
import dev.stow.client.inventory.SmartDeposit;
import dev.stow.client.inventory.HandRefill;
import dev.stow.client.inventory.BulkStrip;
import dev.stow.client.inventory.CompanionHarvest;
import dev.stow.client.inventory.MerchantTrading;
import dev.stow.client.inventory.AutoMending;
import dev.stow.client.hud.SurvivalDock;
import dev.stow.client.hud.BuildingStockHud;
import dev.stow.client.version.VersionChecker;
import dev.stow.client.memory.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class StowClient implements ClientModInitializer {
    @Override public void onInitializeClient(){
        StowConfig.initialize();
        CompanionHarvest.register();
        InventorySorting.register();
        MerchantTrading.register();
        dev.stow.client.ui.ShulkerPreview.register();
        ShulkerMemory.register();StorageOwners.register();AreaClaims.register();AreaSelection.register();
        MaterialPlanner.register();ChestGlow.register();SurvivalDock.register();BuildingStockHud.register();dev.stow.client.hud.AutomationHud.register();dev.stow.client.ui.UiNotifications.register();
        ClientTickEvents.START_CLIENT_TICK.register(mc->{if(!AreaSelection.active())dev.stow.client.input.AttackAutomation.tick(mc);AreaSelection.tick(mc);ShulkerMemory.tick(mc);});
        ClientTickEvents.END_CLIENT_TICK.register(mc->{InventorySorting.tick(mc);SmartDeposit.tick(mc);MerchantTrading.tick(mc);AutoMending.tick(mc);HandRefill.tick(mc);dev.stow.client.inventory.ToolRefill.tick(mc);BulkStrip.tick(mc);ChestMemory.tick(mc);VersionChecker.tick(mc);dev.stow.client.ui.WhatsNew.tick(mc);});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->{dev.stow.client.input.AttackAutomation.stop(mc);AutoMending.clear();InventorySorting.cancel();SmartDeposit.cancel();MerchantTrading.cancel();HandRefill.cancel();dev.stow.client.inventory.ToolRefill.clear();BulkStrip.cancel();SurvivalDock.clear();AreaClaims.clear();StorageOwners.clear();ChestMemory.disconnect();dev.stow.client.stow.StowSession.clear();});
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc->{dev.stow.client.input.AttackAutomation.stop(mc);AutoMending.stop(mc);AutoMending.clear();InventorySorting.cancel();SmartDeposit.cancel();MerchantTrading.cancel();HandRefill.cancel();BulkStrip.cancel();ChestMemory.stop();});
    }
}
