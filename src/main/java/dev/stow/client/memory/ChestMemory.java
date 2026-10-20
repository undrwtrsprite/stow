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
    private static final Set<ChestMemoryStore> QUEUED_SAVES=ConcurrentHashMap.newKeySet();
    private static String worldKey;
    private static ChestMemoryStore store;
    private static Location pending;
    private static long pendingAt;
    private static AbstractContainerMenu activeMenu;
    private static AbstractContainerMenu forgottenMenu;
    private static Location activeLocation;
    private static String activeTitle;
    private static boolean receivedContents;
    private static AbstractContainerMenu snapshottedMenu;
    private static int snapshottedTick;
    private static Location selected;
    private static String selectedMaterial;
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
            ShulkerMemory.clear();AreaClaims.clear();StorageOwners.clear();
            saveLater(store);
            pending = null; activeMenu = null; forgottenMenu=null; activeLocation = null; clearGlow();
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
        activeMenu = null;forgottenMenu=null;
        if (store != null && pending != null && System.nanoTime()-pendingAt <= TimeUnit.SECONDS.toNanos(5)) {
            activeMenu = menu; activeLocation = pending; activeTitle = screen.getTitle().getString(); receivedContents = false;
            StorageOwners.request(store,activeLocation,dev.stow.companion.protocol.StorageOwnerProtocol.QUERY);
        }
        pending = null;
    }

    public static void contentReceived(int containerId) {
        if (activeMenu == null || activeMenu.containerId != containerId) return;
        receivedContents = true;
        snapshot(true);
    }

    public static void clearPending() { pending = null; }
    static ChestMemoryStore observedStore(){return store;}
    static void shulkerRemoved(Location location){
        if(activeLocation!=null&&activeLocation.key().equals(location.key())){
            activeMenu=null;activeLocation=null;receivedContents=false;snapshottedMenu=null;forgottenMenu=null;
        }
        if(pending!=null&&pending.key().equals(location.key()))pending=null;
        if(selected!=null&&selected.key().equals(location.key()))clearGlow();
    }

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
        if(activeMenu==forgottenMenu){if(store.get(activeLocation)==null)return;forgottenMenu=null;} // Undo Forget resumes live snapshots.
        snapshottedMenu=activeMenu;snapshottedTick=Minecraft.getInstance().player==null?0:Minecraft.getInstance().player.tickCount;
        List<MemoryItem> items = collectItems(activeMenu);
        SavedChest old = store.get(activeLocation);
        if (refreshTime || old == null || !old.items().equals(items)){
            var mc=Minecraft.getInstance();var previous=store.get(activeLocation);
            String color=mc.level!=null&&mc.level.hasChunkAt(activeLocation.pos())?ShulkerMemory.color(mc.level.getBlockState(activeLocation.pos())):previous==null?null:previous.shulkerColor();
            store.remember(new SavedChest(activeLocation, activeTitle, System.currentTimeMillis(), items,color,StorageOwners.known(activeLocation)),Stow.config.autoAssignChests);
            if(old==null)dev.stow.client.ui.UiNotifications.show(store.isIncluded(activeLocation)?"stow.feedback.chest-assigned":"stow.feedback.chest-remembered",activeTitle,store.projectName());
        }
    }

    public static void refreshSnapshot() {
        var player=Minecraft.getInstance().player;
        if(activeMenu!=snapshottedMenu||player==null||player.tickCount!=snapshottedTick)snapshot(false);
    }
    public static void forget(ChestMemoryStore fromStore,Location location){
        if(fromStore==store&&Objects.equals(activeLocation,location))forgottenMenu=activeMenu;
        fromStore.forget(location);
        dev.stow.client.ui.UiNotifications.show("stow.feedback.chest-forgotten");
        if(fromStore==store&&Objects.equals(selected,location))clearGlow();
    }

    public static void screenRemoved(AbstractContainerMenu menu) {
        if (menu == activeMenu) { snapshot(true); saveLater(store); }
    }

    public static void tick(Minecraft mc) {
        currentStore();
        MaterialPlanner.tick(mc);
        if (activeMenu != null && (mc.player == null || mc.player.containerMenu != activeMenu)) {
            snapshot(true); saveLater(store); activeMenu = null;forgottenMenu=null; activeLocation = null;
        }
        if (++ticks % 20 == 0) { snapshot(false); saveLater(store); }
        updateGlow(mc);
    }

    public static void disconnect() {
        ShulkerMemory.clear();
        snapshottedMenu=null;
        MaterialPlanner.disconnect();
        snapshot(true); saveLater(store);
        pending = null; activeMenu = null;forgottenMenu=null; activeLocation = null; clearGlow();
        worldKey = null; store = null;
    }

    public static void stop() {
        disconnect(); SAVER.shutdown();
        try { SAVER.awaitTermination(2, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public static void saveLater(ChestMemoryStore toSave) {
        if (toSave == null || SAVER.isShutdown() || !toSave.needsSave() || !QUEUED_SAVES.add(toSave)) return;
        try{
            SAVER.execute(() -> {
                // Remove before taking the snapshot: changes during the write may queue a follow-up.
                QUEUED_SAVES.remove(toSave);
                try { toSave.save(); } catch (IOException e) { LOG.warn("Could not save chest memory", e); }
            });
        }catch(RejectedExecutionException stopped){QUEUED_SAVES.remove(toSave);}
    }

    public static Location selected() { return selected; }
    /** The remembered container behind the open container screen, if any. */
    public static Location activeLocation() { return activeMenu==null?null:activeLocation; }
    public static boolean glowing(){return Stow.config.chestGlow&&selected!=null;}
    /** Start (never toggle off) the glow on one remembered container; itemId may be null. */
    public static void show(ChestMemoryStore source,SavedChest chest,String itemId){
        if(source==null||chest==null)return;
        setGlowEnabled(true);selected=chest.location();selectedMaterial=itemId;updateGlow(Minecraft.getInstance());
        dev.stow.client.ui.UiNotifications.show(Component.translatable("stow.need.found-source",source.displayName(chest)));
    }
    public static void setGlowEnabled(boolean enabled){
        boolean changed=Stow.config.chestGlow!=enabled;
        Stow.config.chestGlow=enabled;Stow.config.save();updateGlow(Minecraft.getInstance());
        if(changed)dev.stow.client.ui.UiNotifications.show(enabled?"stow.feedback.glow-on":"stow.feedback.glow-off");
    }
    private static void clearGlow(){selected=null;selectedMaterial=null;glowTarget=null;}
    public static void stopGlow(){
        boolean hadSelection=selected!=null;clearGlow();
        if(hadSelection)dev.stow.client.ui.UiNotifications.show("stow.feedback.glow-off");
    }
    public static void select(Location location) {
        boolean wasEnabled=Stow.config.chestGlow;setGlowEnabled(true);
        if(wasEnabled&&Objects.equals(selected,location)){stopGlow();return;}
        selectedMaterial=null;selected=location;updateGlow(Minecraft.getInstance());
    }
    public static boolean isMaterialGlowing(String itemId){return Stow.config.chestGlow&&selected!=null&&Objects.equals(selectedMaterial,itemId);}
    public static boolean toggleMaterialGlow(ChestMemoryStore source,String itemId){
        if(isMaterialGlowing(itemId)){stopGlow();return true;}
        return findNearest(source,itemId);
    }
    public static boolean findNearest(ChestMemoryStore source,String itemId) {
        Minecraft mc=Minecraft.getInstance();if(source==null || mc.level==null || mc.player==null)return false;
        String dimension=mc.level.dimension().identifier().toString();
        SavedChest chest=source.nearestSource(itemId,dimension,mc.player.blockPosition());
        if(chest==null){dev.stow.client.ui.UiNotifications.show(Component.translatable("stow.need.no-source"));return false;}
        setGlowEnabled(true);selected=chest.location();selectedMaterial=itemId;updateGlow(mc);
        dev.stow.client.ui.UiNotifications.show(Component.translatable("stow.need.found-source",source.displayName(chest)));return true;
    }
    public static ChestGlow.Target glowTarget() { return glowTarget; }

    private static void updateGlow(Minecraft mc) {
        glowTarget = null;
        if (!Stow.config.chestGlow || selected == null || store == null || mc.level == null || mc.player == null
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
