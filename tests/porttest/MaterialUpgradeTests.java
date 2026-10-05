package porttest;

import dev.stow.*;
import dev.stow.client.memory.*;
import dev.stow.client.hud.*;
import dev.stow.client.input.StowShortcuts;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import com.mojang.blaze3d.platform.InputConstants;
import static dev.stow.client.memory.ChestMemoryStore.*;

final class MaterialUpgradeTests {
    private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;System.out.println("MATERIAL UPGRADE PASS: "+message);}
    static int run(Minecraft mc,FeatureTest.TestScreen parent)throws Exception{
        var oldConfig=Stow.config;String configSnapshot=new com.google.gson.Gson().toJson(oldConfig);var oldScreen=mc.gui.screen();
        var field=ChestMemory.class.getDeclaredField("store");field.setAccessible(true);var oldStore=field.get(null);
        try{
            var dir=Files.createTempDirectory("stow-goal-test-");var large=new ChestMemoryStore(dir,"large");
            for(int i=0;i<1024;i++)large.setGoal("porttest:material_"+i,1000+i);
            check(large.goals().size()==1024,"project accepts 1024 materials instead of 32");large.save();var reloaded=new ChestMemoryStore(dir,"large");
            check(reloaded.goals().size()==1024&&reloaded.goals().getLast().target()==2023,"all 1024 goals retain order and targets after reload");large.removeGoal("porttest:material_500");large.undo();check(large.goals().get(500).itemId().equals("porttest:material_500"),"large material list Undo preserves its original position");
            boolean rejected=false;try{large.setGoal("porttest:overflow",1000);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"1024-material limit has a predictable overflow result");
            check(new MaterialPlanner.Progress(399,0,1000,0).color()==0xFFFF967D&&new MaterialPlanner.Progress(0,0,1000,0).color()==0xFFFF967D,"low material totals are red below 40 percent");
            check(new MaterialPlanner.Progress(200,200,1000,1).color()==0xFFFFD486&&new MaterialPlanner.Progress(999,0,1000,0).color()==0xFFFFD486,"combined totals are yellow from 40 percent to completion");
            check(new MaterialPlanner.Progress(1000,0,1000,0).color()==0xFF8DE8B2&&new MaterialPlanner.Progress(2000,0,1000,0).color()==0xFF8DE8B2,"complete and surplus totals are green");
            var store=new ChestMemoryStore(dir,"selection");var ids=List.of("minecraft:cobblestone","minecraft:dirt","minecraft:diamond","minecraft:oak_planks","minecraft:sand");for(var id:ids)store.setGoal(id,1000);
            Stow.config.dockRows=3;check(store.hudGoals(3).stream().map(MaterialGoal::itemId).toList().equals(ids.subList(0,3))&&store.starredGoals(3).isEmpty(),"project HUD defaults to three ordinary goals without starring them");for(var id:ids.subList(0,3))store.toggleHudGoal(id,3);
            check(store.toggleHudGoal(ids.get(0),3)&&store.toggleHudGoal(ids.get(4),3)&&!store.toggleHudGoal(ids.get(3),3),"HUD stars let users replace a material and prevent exceeding three choices");
            check(store.hudGoals(3).stream().map(MaterialGoal::itemId).toList().equals(List.of(ids.get(1),ids.get(2),ids.get(4))),"HUD can show chosen materials from later in the inventory list");
            store.toggleGoalVisible(ids.get(4));check(store.hudGoals(3).getLast().itemId().equals(ids.get(4)),"HUD stars are independent of inventory counter visibility");store.toggleGoalVisible(ids.get(4));store.save();
            check(new ChestMemoryStore(dir,"selection").hudGoals(3).equals(store.hudGoals(3)),"project HUD stars survive reload");
            String original=store.activeProjectId();var a=new Location("minecraft:overworld",0,64,0,"Chest");var b=new Location("minecraft:overworld",10,64,0,"Chest");for(var pos:List.of(a,b))store.remember(new SavedChest(pos,"Chest",0,List.of(new MemoryItem(ids.get(0),"Cobblestone",64))));store.selectAllChests(false);store.toggleIncluded(a);
            String second=store.createProject("Other build");check(store.includedChestCount()==0&&!store.usesAllChests(),"new project starts with no chests selected");store.toggleIncluded(b);store.setGoal(ids.get(3),50);store.toggleHudGoal(ids.get(3),3);store.toggleHudGoal(ids.get(3),3);check(store.starredGoals(3).isEmpty()&&store.hudGoals(3).size()==1,"clearing HUD priorities restores ordinary goal filling");store.setMaterialPage(4);store.switchProject(original);
            check(store.isIncluded(a)&&!store.isIncluded(b)&&store.hudGoals(3).size()==3&&store.materialPage()==0,"switching project restores its own chests, HUD stars and inventory scroll");store.save();var saved=new ChestMemoryStore(dir,"selection");saved.switchProject(second);check(saved.isIncluded(b)&&!saved.isIncluded(a)&&saved.starredGoals(3).isEmpty()&&saved.hudGoals(3).size()==1,"each project's different chest selection and empty HUD choice persist");
            Stow.config.rememberMaterialPage=true;var tracker=new FloatingMaterialTracker(200,10,100,48,id->{});tracker.update(store,true);String first=tracker.displayedGoals().getFirst().itemId();tracker.mouseScrolled(202,12,0,-1);String later=tracker.displayedGoals().getFirst().itemId();
            var reopened=new FloatingMaterialTracker(200,10,100,48,id->{});reopened.update(store,true);check(!first.equals(later)&&reopened.displayedGoals().getFirst().itemId().equals(later),"remember-page setting restores inventory material scroll on reentry");
            Stow.config.rememberMaterialPage=false;var reset=new FloatingMaterialTracker(200,10,100,48,id->{});reset.update(store,true);check(reset.displayedGoals().getFirst().itemId().equals(first),"reset-page setting returns reopened inventory counters to the first item");
            store.toggleHudGoal(ids.get(4),3);reset.extractRenderState(new GuiGraphicsExtractor(mc,new GuiRenderState(),320,240),202,12,0);check(reset.toggleHoveredHud(FeatureTest.key(InputConstants.KEY_H,104,0))&&store.isHudGoal(first,3),"hover plus H toggles a HUD star without pinning an inventory slot");
            // Use the actual inventory mixin/button, with a controlled remembered world store.
            FeatureTest.setMemoryField("store",store);mc.gui.setScreen(parent);var render=new GuiRenderState();parent.extractRenderState(new GuiGraphicsExtractor(mc,render,parent.width,parent.height),-100,-100,0);
            var floating=(FloatingMaterialTracker)parent.children().stream().filter(FloatingMaterialTracker.class::isInstance).findFirst().orElseThrow();parent.extractRenderState(new GuiGraphicsExtractor(mc,new GuiRenderState(),parent.width,parent.height),floating.getX()+2,floating.getY()+2,0);parent.keyPressed(FeatureTest.key(InputConstants.KEY_H,104,0));check(!store.isHudGoal(first,3),"actual inventory H shortcut toggles the hovered floating material star");
            var location=a;Stow.config.chestGlow=true;ChestMemory.select(location);var glow=FeatureTest.button(parent,"Toggle chest glow");parent.mouseClicked(FeatureTest.mouse(glow.getX()+2,glow.getY()+2,0),false);
            check(!Stow.config.chestGlow&&ChestMemory.selected()!=null&&ChestMemory.glowTarget()==null,"inventory eye suppresses highlights while remembering the selected chest");ChestMemory.select(b);check(Stow.config.chestGlow&&ChestMemory.selected().equals(b),"explicit Glow action re-enables highlights after using the eye");parent.mouseClicked(FeatureTest.mouse(glow.getX()+2,glow.getY()+2,0),false);parent.mouseClicked(FeatureTest.mouse(glow.getX()+2,glow.getY()+2,0),false);check(Stow.config.chestGlow&&ChestMemory.selected()!=null,"inventory glow toggle re-enables the remembered highlight");ChestMemory.stopGlow();
            store.switchProject(second);var projects=new ProjectsScreen(parent,store);mc.gui.setScreen(projects);var storageButton=(Button)projects.children().stream().filter(w->w instanceof Button btn&&btn.getMessage().getString().startsWith("Choose project chests")).findFirst().orElseThrow();FeatureTest.press(storageButton);check(mc.gui.screen() instanceof ChestMemoryScreen,"Projects has a direct button to choose its own chests");var resultsField=ChestMemoryScreen.class.getDeclaredField("results");resultsField.setAccessible(true);var results=(java.util.List<SearchResult>)resultsField.get(mc.gui.screen());check(results.getFirst().chest().location().equals(b),"chosen project chests sort before unselected chests");mc.gui.screen().onClose();check(mc.gui.screen()==projects,"project chest selection returns directly to Projects");
            Stow.config.rememberMaterialPage=false;Stow.config.chestGlow=false;Stow.config.save();var config=FeatureTest.reloadConfig();check(!config.rememberMaterialPage&&!config.chestGlow,"page memory and glow toggle persist in settings");
        }finally{ChestMemory.stopGlow();FeatureTest.setMemoryField("store",oldStore);Stow.config=new com.google.gson.Gson().fromJson(configSnapshot,StowConfig.class);Stow.config.save();mc.gui.setScreen(oldScreen);}
        return checks;
    }
}
