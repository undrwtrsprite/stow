package dev.stow.client.stow;

import dev.stow.Stow;
import dev.stow.client.input.AttackAutomation;
import dev.stow.client.input.StowShortcuts;
import dev.stow.client.input.StowShortcuts.Action;
import dev.stow.client.inventory.*;
import dev.stow.client.memory.*;
import dev.stow.client.ui.UIIcons;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The 1.0.6 command palette, folded into Find ("&gt;" lists only actions). */
public final class StowActions {
    public record Entry(String id,Component title,String keywords,ItemStack icon,UIIcons.Kind glyph,Action shortcut,boolean enabled,Component reason,Runnable run) {
        public String shortcutLabel(){return shortcut==null||StowShortcuts.binding(shortcut).code()<0?"":StowShortcuts.label(StowShortcuts.binding(shortcut));}
    }
    private StowActions(){}
    private static Component t(String key,Object... args){return Component.translatable(key,args);}
    public static List<Entry> all(StowScreen screen){
        var mc=Minecraft.getInstance();var store=screen.store();var origin=screen.origin();
        var container=origin instanceof AbstractContainerScreen<?> c?c:null;
        boolean world=origin==null;var list=new ArrayList<Entry>();
        list.add(new Entry("settings",t("stow.settings.title"),"settings options config preferences einstellungen",ItemStack.EMPTY,UIIcons.Kind.SETTINGS,null,true,null,()->mc.gui.setScreen(SettingsScreen.open(screen))));
        list.add(new Entry("controls",t("stow.ui.action.controls"),"controls keys keybinds shortcuts tasten steuerung",ItemStack.EMPTY,UIIcons.Kind.KEYBOARD,null,true,null,()->mc.gui.setScreen(SettingsScreen.controls(screen))));
        list.add(new Entry("keep",t("stow.keep.title"),"keep amounts deposit behalten",ItemStack.EMPTY,UIIcons.Kind.KEEP,Action.KEEP,true,null,()->mc.gui.setScreen(SettingsScreen.keep(screen,""))));
        list.add(new Entry("arrange",t("stow.ui.arrange.title"),"arrange hud layout position move anordnen",ItemStack.EMPTY,UIIcons.Kind.GRID,null,true,null,()->mc.gui.setScreen(new ArrangeHudScreen(screen,null))));
        list.add(new Entry("whatsnew",t("stow.whatsnew.open"),"what's new whats new changelog updates neu",ItemStack.EMPTY,UIIcons.Kind.COMMAND,null,true,null,()->mc.gui.setScreen(dev.stow.client.ui.WhatsNew.screen(screen))));
        list.add(new Entry("equipment",t("stow.watch.title"),"equipment watch durability armor tools haltbarkeit",ItemStack.EMPTY,UIIcons.Kind.EQUIPMENT,Action.EQUIPMENT,true,null,()->mc.gui.setScreen(new dev.stow.client.ui.EquipmentScreen(screen))));
        if(container!=null&&SmartDeposit.supported(container.getMenu())){
            list.add(new Entry("deposit",t("stow.action.deposit"),"deposit store einlagern",ItemStack.EMPTY,UIIcons.Kind.DEPOSIT,Action.DEPOSIT,true,null,()->{mc.gui.setScreen(container);SmartDeposit.start(container);}));
            list.add(new Entry("take",t("stow.take.button-plain"),"take missing project fetch nehmen",ItemStack.EMPTY,UIIcons.Kind.MATERIALS,null,Stow.config.takeMissing,t("stow.take.disabled"),()->{mc.gui.setScreen(container);TakeMissing.start(container);}));
        }
        if(container!=null)list.add(new Entry("sort",t("stow.action.sort"),"sort order sortieren",ItemStack.EMPTY,UIIcons.Kind.SORT,Action.SORT,true,null,()->{mc.gui.setScreen(container);dev.stow.client.ui.InventoryStrip.sort(container,false);}));
        list.add(new Entry("glow",t(Stow.config.chestGlow?"stow.ui.action.glow-off":"stow.ui.action.glow-on"),"glow highlight chest leuchten",ItemStack.EMPTY,Stow.config.chestGlow?UIIcons.Kind.EYE_OFF:UIIcons.Kind.EYE,null,true,null,()->{ChestMemory.setGlowEnabled(!Stow.config.chestGlow);screen.refresh();}));
        if(ChestMemory.glowing())list.add(new Entry("stop-glow",t("stow.ui.strip.stop-glow"),"stop glow",ItemStack.EMPTY,UIIcons.Kind.EYE_OFF,null,true,null,()->{ChestMemory.stopGlow();screen.refresh();}));
        list.add(new Entry("auto_tool",t("stow.action.auto_tool"),"auto tool werkzeug",new ItemStack(Items.IRON_SHOVEL),null,Action.AUTO_TOOL,true,null,()->{StowShortcuts.dispatch(Action.AUTO_TOOL,origin,null);screen.refresh();}));
        list.add(new Entry("auto_attack",t("stow.action.auto_attack"),"auto attack angriff",new ItemStack(Items.IRON_SWORD),null,Action.AUTO_ATTACK,true,null,()->{StowFind.closeAll();AttackAutomation.toggle(mc);}));
        list.add(new Entry("auto_mend",t("stow.action.auto_mend"),"mending repair reparatur",new ItemStack(Items.EXPERIENCE_BOTTLE),null,Action.AUTO_MEND,true,null,()->{StowFind.closeAll();AutoMending.toggle(mc);}));
        list.add(new Entry("bulk_strip",t("stow.action.bulk_strip"),"strip logs entrinden",new ItemStack(Items.IRON_AXE),null,Action.BULK_STRIP,Stow.config.bulkStrip||BulkStrip.busy(),t("stow.ui.action.strip-disabled"),()->{StowFind.closeAll();BulkStrip.toggle(mc);}));
        list.add(new Entry("drag",t("stow.ui.action.drag",t("stow.inventory.drag."+Stow.config.dragMode.name().toLowerCase(Locale.ROOT))),"drag mode shift ziehen",ItemStack.EMPTY,UIIcons.Kind.BOTH,Action.CYCLE_DRAG,true,null,()->{Stow.config.cycleDragMode();screen.refresh();}));
        list.add(new Entry("dock",t(Stow.config.dockEnabled?"stow.ui.action.dock-off":"stow.ui.action.dock-on"),"equipment hud dock",ItemStack.EMPTY,UIIcons.Kind.EQUIPMENT,Action.DOCK,true,null,()->{StowShortcuts.dispatch(Action.DOCK,screen,null);screen.refresh();}));
        if(store!=null){
            list.add(new Entry("import",t("stow.import.title"),"import paste material list litematica importieren",ItemStack.EMPTY,UIIcons.Kind.COPY,null,true,null,()->mc.gui.setScreen(new MaterialImportScreen(screen,store))));
            list.add(new Entry("copy",t("stow.export.copy"),"copy export clipboard kopieren",ItemStack.EMPTY,UIIcons.Kind.COPY,null,!store.goals().isEmpty(),t("stow.ui.project.empty"),()->screen.copyList()));
            list.add(new Entry("new-project",t("stow.ui.project.create"),"new project create projekt",ItemStack.EMPTY,UIIcons.Kind.PROJECT,null,store.projects().size()<16,t("stow.ui.project.limit"),()->screen.openProjects()));
            list.add(new Entry("claim",t("stow.ui.containers.claim"),"claim area owner companion bereich",ItemStack.EMPTY,UIIcons.Kind.STORAGE,null,AreaClaims.available(),t("stow.owner.requires-companion"),()->AreaClaimScreen.direct(screen,store)));
            if(store.canUndo())list.add(new Entry("undo",t("stow.ui.undo",screen.undoName()),"undo rückgängig",ItemStack.EMPTY,UIIcons.Kind.LEFT,null,true,null,()->screen.undo()));
        }
        return list;
    }
    public static boolean matches(Entry entry,String query){
        String text=(entry.title().getString()+" "+entry.keywords()+" "+entry.id()).toLowerCase(Locale.ROOT);
        for(String word:query.toLowerCase(Locale.ROOT).split("\\s+"))if(!word.isEmpty()&&!text.contains(word))return false;
        return true;
    }
}
