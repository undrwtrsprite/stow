package dev.stow.client.inventory;

import dev.stow.Stow;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.BitSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Player inventory positions, independent of the currently open menu's slot IDs. */
public final class PinnedSlots {
    private static final org.slf4j.Logger LOGGER = Stow.createLogger(PinnedSlots.class);
    private static final BitSet PINS = new BitSet(36);
    private static boolean loaded;

    private PinnedSlots() {}

    private static Path file() {
        return Stow.dataDirectory().resolve("pins.txt");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(file())) return;
        try {
            for (String line : Files.readAllLines(file())) {
                try {
                    int index = Integer.parseInt(line.trim());
                    if (index >= 0 && index < 36) PINS.set(index);
                } catch (NumberFormatException ignored) {}
            }
        } catch (IOException e) {
            LOGGER.warn("Could not load pinned inventory slots", e);
        }
    }

    public static boolean canPin(Slot slot) {
        return slot != null && slot.container instanceof Inventory
                && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 36;
    }

    public static boolean isPinned(Slot slot) {
        if (!canPin(slot)) return false;
        load();
        return PINS.get(slot.getContainerSlot());
    }

    public static boolean toggle(Slot slot) {
        if (!canPin(slot)) return false;
        load();
        PINS.flip(slot.getContainerSlot());
        Path target = file();
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            Files.write(temporary, PINS.stream().mapToObj(Integer::toString).toList());
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not save pinned inventory slots", e);
        }
        dev.stow.client.ui.UiNotifications.show(PINS.get(slot.getContainerSlot())?"stow.feedback.pinned":"stow.feedback.unpinned");
        return true;
    }
}
