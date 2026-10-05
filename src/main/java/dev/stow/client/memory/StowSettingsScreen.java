package dev.stow.client.memory;

import dev.stow.Stow;
import dev.stow.StowConfig.*;
import me.shedaniel.clothconfig2.api.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** Uses the same Cloth Config builder and category layout as BetterGrassify. */
public final class StowSettingsScreen {
    private StowSettingsScreen(){}
    private static Component text(String key){return Component.translatable("stow.settings."+key);}
    private static Component sortName(Enum<?> value){return Component.translatable("stow.sort."+value.name().toLowerCase(Locale.ROOT));}
    private static <T extends Enum<?>> ResponsiveEnumEntry<T> option(ConfigEntryBuilder builder,String key,Class<T> type,T value,T defaults,java.util.function.Function<Enum,Component> names,java.util.function.Consumer<T> save,String tip){
        return new ResponsiveEnumEntry<>(text(key),type,value,builder.getResetButtonKey(),defaults,save,names,tip==null?null:text(tip));
    }
    public static Screen create(Screen parent){
        var config=Stow.config;
        ConfigBuilder builder=ConfigBuilder.create().setParentScreen(parent).setTitle(text("title"));
        builder.setGlobalized(true);builder.setGlobalizedExpanded(true);
        builder.setShouldListSmoothScroll(true);builder.setShouldTabsSmoothScroll(true);
        ConfigEntryBuilder entry=builder.entryBuilder();
        var appearance=builder.getOrCreateCategory(text("appearance"));
        appearance.addEntry(new ResponsiveConfigEntries.Slider(text("stock.scale"),config.buildingStockScale,100,200,entry.getResetButtonKey(),135,v->config.buildingStockScale=v));
        ConfigCategory sorting=builder.getOrCreateCategory(text("sorting"));
        sorting.addEntry(option(entry,"normal",SortOrder.class,config.sortOrder,SortOrder.CREATIVE,
            StowSettingsScreen::sortName,value->config.sortOrder=value,"normal.tip"));
        sorting.addEntry(option(entry,"shift",SortOrder.class,config.shiftSortOrder,SortOrder.AMOUNT,
            StowSettingsScreen::sortName,value->config.shiftSortOrder=value,null));
        sorting.addEntry(option(entry,"control",SortOrder.class,config.controlSortOrder,SortOrder.NAME,
            StowSettingsScreen::sortName,value->config.controlSortOrder=value,null));
        sorting.addEntry(option(entry,"hotbar",HotbarScoping.class,config.hotbarScoping,HotbarScoping.SOFT,
            value->text("hotbar."+value.name().toLowerCase(Locale.ROOT)),value->config.hotbarScoping=value,"hotbar.tip"));
        ConfigCategory inventory=builder.getOrCreateCategory(text("inventory"));
        inventory.addEntry(option(entry,"drag",DragMode.class,config.dragMode,DragMode.BOTH,
            value->Component.translatable("stow.inventory.drag."+value.name().toLowerCase(Locale.ROOT)),value->config.dragMode=value,"drag.tip"));
        inventory.addEntry(new ResponsiveConfigEntries.Toggle(text("materials.include-inventory"),config.materialsIncludeInventory,entry.getResetButtonKey(),true,v->config.materialsIncludeInventory=v));
        inventory.addEntry(new ResponsiveConfigEntries.Toggle(text("materials.page"),config.rememberMaterialPage,entry.getResetButtonKey(),true,v->config.rememberMaterialPage=v));
        inventory.addEntry(option(entry,"pin.style",PinStyle.class,config.pinStyle,PinStyle.LOCK,value->text("pin."+value.name().toLowerCase(Locale.ROOT)),v->config.pinStyle=v,null));
        var pinTransfers=new ResponsiveConfigEntries.Toggle(text("pin.transfers"),config.pinProtectTransfers,entry.getResetButtonKey(),true,v->config.pinProtectTransfers=v);
        pinTransfers.setTooltipSupplier(()->java.util.Optional.of(new Component[]{text("pin.transfers.tip")}));
        inventory.addEntry(pinTransfers);
        var bundleDrag=new ResponsiveConfigEntries.Toggle(text("bundle.drag"),config.bundleDrag,entry.getResetButtonKey(),true,v->config.bundleDrag=v);
        bundleDrag.setTooltipSupplier(()->java.util.Optional.of(new Component[]{text("bundle.drag.tip")}));
        inventory.addEntry(bundleDrag);
        inventory.addEntry(new ResponsiveConfigEntries.Toggle(text("glow"),config.chestGlow,entry.getResetButtonKey(),true,v->ChestMemory.setGlowEnabled(v)));
        inventory.addEntry(new ResponsiveConfigEntries.Toggle(text("stock"),config.buildingStock,entry.getResetButtonKey(),true,v->config.buildingStock=v));
        inventory.addEntry(new ResponsiveConfigEntries.Toggle(text("refill"),config.handRefill,entry.getResetButtonKey(),true,v->config.handRefill=v));
        inventory.addEntry(new ResponsiveConfigEntries.Slider(text("refill.threshold"),config.refillThreshold,1,16,entry.getResetButtonKey(),8,v->config.refillThreshold=v));
        inventory.addEntry(new ResponsiveConfigEntries.Toggle(text("tool-pick"),config.toolPick,entry.getResetButtonKey(),true,v->config.toolPick=v));
        var autoTool=new ResponsiveConfigEntries.Toggle(text("auto-tool"),config.autoTool,entry.getResetButtonKey(),false,v->config.autoTool=v);
        autoTool.setTooltipSupplier(()->java.util.Optional.of(new Component[]{text("auto-tool.tip")}));
        inventory.addEntry(autoTool);
        var bulkStrip=new ResponsiveConfigEntries.Toggle(text("bulk-strip"),config.bulkStrip,entry.getResetButtonKey(),true,v->config.bulkStrip=v);
        bulkStrip.setTooltipSupplier(()->java.util.Optional.of(new Component[]{text("bulk-strip.tip")}));
        inventory.addEntry(bulkStrip);
        inventory.addEntry(new ScreenLinkEntry(Component.translatable("stow.shortcuts.title"),dev.stow.client.ui.ShortcutEditorScreen::new));
        var deposit=builder.getOrCreateCategory(text("deposit"));
        deposit.addEntry(new ResponsiveConfigEntries.Toggle(text("deposit.matching"),config.depositMatchingOnly,entry.getResetButtonKey(),true,v->config.depositMatchingOnly=v));
        deposit.addEntry(new ResponsiveConfigEntries.Toggle(text("deposit.hotbar"),config.depositKeepHotbar,entry.getResetButtonKey(),true,v->config.depositKeepHotbar=v));
        deposit.addEntry(new ScreenLinkEntry(Component.translatable("stow.keep.title"),parentScreen->new dev.stow.client.ui.KeepAmountsScreen(parentScreen,"")));
        var watch=builder.getOrCreateCategory(text("dock"));
        watch.addEntry(new ResponsiveConfigEntries.Toggle(text("dock.enabled"),config.dockEnabled&&config.equipmentWatch,entry.getResetButtonKey(),true,v->{config.dockEnabled=v;config.equipmentWatch=v;}));
        watch.addEntry(new ResponsiveConfigEntries.Toggle(text("equipment.all"),config.equipmentAll,entry.getResetButtonKey(),true,v->config.equipmentAll=v));
        watch.addEntry(new ResponsiveConfigEntries.Slider(text("equipment.threshold"),config.equipmentThreshold,1,50,entry.getResetButtonKey(),15,v->config.equipmentThreshold=v));
        var dock=watch;
        dock.addEntry(new ResponsiveConfigEntries.Slider(text("dock.scale"),config.dockScale,60,150,entry.getResetButtonKey(),100,v->config.dockScale=v));
        dock.addEntry(new ScreenLinkEntry(Component.translatable("stow.dock.layout"),dev.stow.client.ui.DockLayoutScreen::new));
        var projectHud=builder.getOrCreateCategory(text("project"));
        projectHud.addEntry(new ResponsiveConfigEntries.Toggle(text("dock.materials"),config.dockMaterials,entry.getResetButtonKey(),true,v->config.dockMaterials=v));
        projectHud.addEntry(option(entry,"project.corner",DockCorner.class,config.projectCorner,DockCorner.TOP_RIGHT,value->Component.translatable("stow.dock.corner."+value.name().toLowerCase(Locale.ROOT)),value->config.projectCorner=value,null));
        projectHud.addEntry(new ResponsiveConfigEntries.Slider(text("dock.rows"),config.dockRows,3,5,entry.getResetButtonKey(),3,v->config.dockRows=v));
        projectHud.addEntry(new ResponsiveConfigEntries.Slider(text("dock.scale"),config.projectScale,60,150,entry.getResetButtonKey(),100,v->config.projectScale=v));
        projectHud.addEntry(new ScreenLinkEntry(Component.translatable("stow.project.layout"),parentScreen->new dev.stow.client.ui.DockLayoutScreen(parentScreen,true)));
        projectHud.addEntry(new ResponsiveConfigEntries.Number(text("project.x"),config.projectOffsetX,0,1000,entry.getResetButtonKey(),8,v->config.projectOffsetX=v));
        projectHud.addEntry(new ResponsiveConfigEntries.Number(text("project.y"),config.projectOffsetY,0,1000,entry.getResetButtonKey(),8,v->config.projectOffsetY=v));
        builder.setSavingRunnable(()->{config.save();Stow.config=config;});
        return builder.build();
    }
}
