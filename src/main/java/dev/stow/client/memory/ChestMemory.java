package dev.stow.client.memory;

import dev.stow.Stow;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import static dev.stow.client.memory.ChestMemoryStore.*;

/** Associates a real block interaction with its server-supplied inventory menu. */
public final class ChestMemory {
    private static final org.slf4j.Logger LOG = Stow.createLogger(ChestMemory.class);
    private static final ExecutorService SAVER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "stow chest memory"); thread.setDaemon(true); return thread;
    });
    private static String worldKey;
    private static ChestMemoryStore store;
    private static Location pending;
    private static long pendingAt;
    private static AbstractContainerMenu activeMenu;
    private static Location activeLocation;
    private static String activeTitle;
    private static boolean receivedContents;
    private static Location selected;
    private static int ticks;
    private static volatile ChestGlow.Target glowTarget;

    private ChestMemory() {}

    public static String currentWorldKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        if (mc.getSingleplayerServer() != null) {
            return "local:" + mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        }
        if (mc.getCurrentServer() != null) return "server:" + mc.getCurrentServer().ip.strip().toLowerCase(Locale.ROOT);
        if (mc.getConnection() != null && mc.getConnection().getServerData() != null)
            return "server:" + mc.getConnection().getServerData().ip.strip().toLowerCase(Locale.ROOT);
        return null;
    }

    public static ChestMemoryStore currentStore() {
        String current = currentWorldKey();
        if (!Objects.equals(worldKey, current)) {
            saveLater(store);
            pending = null; activeMenu = null; activeLocation = null; selected = null; glowTarget = null;
            worldKey = current; store = null;
            if (current != null) {
                try { store = new ChestMemoryStore(Stow.dataDirectory().resolve("chests"), current); }
                catch (IOException e) { LOG.warn("Could not load this world's chest memory; keeping the existing file", e); }
            }
        }
        return store;
    }

    public static Location locationFor(BlockPos pos, BlockState state, String dimension) {
        String kind;
        if (state.getBlock() instanceof ChestBlock) {
            kind = state.getValue(ChestBlock.TYPE) == ChestType.SINGLE ? "Chest" : "Double chest";
            if (state.getValue(ChestBlock.TYPE) == ChestType.RIGHT) pos = ChestBlock.getConnectedBlockPos(pos, state);
        } else if (state.getBlock() instanceof BarrelBlock) kind = "Barrel";
        else if (state.getBlock() instanceof ShulkerBoxBlock) kind = "Shulker box";
        else return null; // Ender chests have shared contents; minecarts and custom virtual menus have no block location.
        return new Location(dimension, pos.getX(), pos.getY(), pos.getZ(), kind);
    }

    public static void clicked(BlockHitResult hit) {
        Minecraft mc = Minecraft.getInstance();
        currentStore();
        pending = null;
        if (store == null || mc.level == null) return;
        pending = locationFor(hit.getBlockPos(), mc.level.getBlockState(hit.getBlockPos()), mc.level.dimension().identifier().toString());
        pendingAt = System.nanoTime();
    }

    public static void opened(AbstractContainerScreen<?> screen) {
        currentStore();
        AbstractContainerMenu menu = screen.getMenu();
        if (activeMenu == menu) return; // Returning from the chest-search screen.
        if (!(menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu)) { pending = null; return; }
        snapshot(true);
        activeMenu = null;
        if (store != null && pending != null && System.nanoTime()-pendingAt <= TimeUnit.SECONDS.toNanos(5)) {
            activeMenu = menu; activeLocation = pending; activeTitle = screen.getTitle().getString(); receivedContents = false;
        }
        pending = null;
    }

    public static void contentReceived(int containerId) {
        if (activeMenu == null || activeMenu.containerId != containerId) return;
        receivedContents = true;
        snapshot(true);
    }

    public static void clearPending() { pending = null; }

    public static void slotReceived(int containerId) {
        if (activeMenu != null && activeMenu.containerId == containerId && receivedContents) snapshot(false);
    }

    public static List<MemoryItem> collectItems(AbstractContainerMenu menu) {
        Map<String, MemoryItem> items = new LinkedHashMap<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory || !slot.hasItem()) continue;
            ItemStack stack = slot.getItem();
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            String name = stack.getHoverName().getString();
            String key = id + "\u0000" + name;
            MemoryItem old = items.get(key);
            items.put(key, new MemoryItem(id, name, stack.getCount() + (old == null ? 0 : old.count())));
        }
        return List.copyOf(items.values());
    }

    private static void snapshot(boolean refreshTime) {
        if (store == null || activeMenu == null || activeLocation == null || !receivedContents) return;
        List<MemoryItem> items = collectItems(activeMenu);
        SavedChest old = store.get(activeLocation);
        if (refreshTime || old == null || !old.items().equals(items))
            store.remember(new SavedChest(activeLocation, activeTitle, System.currentTimeMillis(), items));
    }

    public static void refreshSnapshot() { snapshot(false); }

    public static void screenRemoved(AbstractContainerMenu menu) {
        if (menu == activeMenu) { snapshot(true); saveLater(store); }
    }

    public static void tick(Minecraft mc) {
        currentStore();
        MaterialPlanner.tick(mc);
        if (activeMenu != null && (mc.player == null || mc.player.containerMenu != activeMenu)) {
            snapshot(true); saveLater(store); activeMenu = null; activeLocation = null;
        }
        if (++ticks % 20 == 0) { snapshot(false); saveLater(store); }
        updateGlow(mc);
    }

    public static void disconnect() {
        MaterialPlanner.disconnect();
        snapshot(true); saveLater(store);
        pending = null; activeMenu = null; activeLocation = null; selected = null; glowTarget = null;
        worldKey = null; store = null;
    }

    public static void stop() {
        disconnect(); SAVER.shutdown();
        try { SAVER.awaitTermination(2, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void saveLater(ChestMemoryStore toSave) {
        if (toSave == null || SAVER.isShutdown()) return;
        SAVER.submit(() -> { try { toSave.save(); } catch (IOException e) { LOG.warn("Could not save chest memory", e); } });
    }

    public static Location selected() { return selected; }
    public static void select(Location location) { selected = Objects.equals(selected, location) ? null : location; updateGlow(Minecraft.getInstance()); }
    public static boolean findNearest(ChestMemoryStore source,String itemId) {
        Minecraft mc=Minecraft.getInstance();if(source==null || mc.level==null || mc.player==null)return false;
        String dimension=mc.level.dimension().identifier().toString();
        SavedChest chest=source.nearestSource(itemId,dimension,mc.player.blockPosition());
        if(chest==null){mc.gui.hud.setOverlayMessage(Component.translatable("stow.need.no-source"),false);return false;}
        selected=chest.location();updateGlow(mc);
        mc.gui.hud.setOverlayMessage(Component.translatable("stow.need.found-source",source.displayName(chest)),false);return true;
    }
    public static ChestGlow.Target glowTarget() { return glowTarget; }

    private static void updateGlow(Minecraft mc) {
        glowTarget = null;
        if (selected == null || store == null || mc.level == null || mc.player == null
                || !selected.dimension().equals(mc.level.dimension().identifier().toString())) return;
        SavedChest remembered = store.get(selected);
        if (remembered == null) { selected = null; return; }
        BlockPos pos = selected.pos();
        AABB box = new AABB(pos);
        if (mc.level.hasChunkAt(pos)) {
            BlockState state = mc.level.getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE)
                box = box.minmax(new AABB(ChestBlock.getConnectedBlockPos(pos, state)));
        }
        glowTarget = new ChestGlow.Target(box.inflate(0.035), store.displayName(remembered));
    }
}
