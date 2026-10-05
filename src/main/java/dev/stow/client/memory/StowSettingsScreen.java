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
        ConfigEntryBuilder entry=builder.entryBuilder();
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
        var deposit=builder.getOrCreateCategory(text("deposit"));
        deposit.addEntry(entry.startBooleanToggle(text("deposit.hotbar"),config.depositKeepHotbar).setDefaultValue(true).setSaveConsumer(v->config.depositKeepHotbar=v).build());
        var watch=builder.getOrCreateCategory(text("equipment"));
        watch.addEntry(entry.startBooleanToggle(text("equipment.enabled"),config.equipmentWatch).setDefaultValue(true).setSaveConsumer(v->config.equipmentWatch=v).build());
        watch.addEntry(entry.startIntSlider(text("equipment.threshold"),config.equipmentThreshold,1,50).setDefaultValue(15).setSaveConsumer(v->config.equipmentThreshold=v).build());
        var dock=builder.getOrCreateCategory(text("dock"));
        dock.addEntry(entry.startBooleanToggle(text("dock.enabled"),config.dockEnabled).setDefaultValue(true).setSaveConsumer(v->config.dockEnabled=v).build());
        dock.addEntry(entry.startBooleanToggle(text("dock.materials"),config.dockMaterials).setDefaultValue(true).setSaveConsumer(v->config.dockMaterials=v).build());
        dock.addEntry(entry.startIntSlider(text("dock.rows"),config.dockRows,1,6).setDefaultValue(3).setSaveConsumer(v->config.dockRows=v).build());
        dock.addEntry(entry.startIntSlider(text("dock.scale"),config.dockScale,60,150).setDefaultValue(100).setSaveConsumer(v->config.dockScale=v).build());
        builder.setSavingRunnable(()->{config.save();Stow.config=config;});
        return builder.build();
    }
}
