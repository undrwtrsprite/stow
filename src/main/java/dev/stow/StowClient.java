package dev.stow;

import dev.stow.client.inventory.InventorySorting;
import dev.stow.client.inventory.SmartDeposit;
import dev.stow.client.inventory.HandRefill;
import dev.stow.client.hud.SurvivalDock;
import dev.stow.client.hud.BuildingStockHud;
import dev.stow.client.memory.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class StowClient implements ClientModInitializer {
    @Override public void onInitializeClient(){
        StowConfig.initialize();
        MaterialPlanner.register();ChestGlow.register();SurvivalDock.register();BuildingStockHud.register();
        ClientTickEvents.END_CLIENT_TICK.register(mc->{InventorySorting.tick(mc);SmartDeposit.tick(mc);HandRefill.tick(mc);ChestMemory.tick(mc);});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->{InventorySorting.cancel();SmartDeposit.cancel();HandRefill.cancel();SurvivalDock.clear();ChestMemory.disconnect();});
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc->{InventorySorting.cancel();SmartDeposit.cancel();HandRefill.cancel();ChestMemory.stop();});
    }
}
