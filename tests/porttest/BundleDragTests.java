package porttest;

import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import dev.stow.Stow;
import dev.stow.client.inventory.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BundleContents;

final class BundleDragTests {
    private static int checks;
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;System.out.println("BUNDLE PASS: "+message);}
    private static ItemStack bundle(ItemStack... items){var b=new ItemStack(Items.BUNDLE);b.set(DataComponents.BUNDLE_CONTENTS,new BundleContents(Arrays.stream(items).map(ItemStackTemplate::fromNonEmptyStack).toList()));return b;}
    private static int contained(ItemStack b,Item item){return b.get(DataComponents.BUNDLE_CONTENTS).itemCopies().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();}
    static class SilentPlayer extends net.minecraft.client.player.RemotePlayer {
        SilentPlayer(){super(null,null);}
        @Override public void playSound(net.minecraft.sounds.SoundEvent sound,float volume,float pitch){}
    }
    static int run(Minecraft mc)throws Exception{
        var uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);var unsafe=(sun.misc.Unsafe)uf.get(null);
        var player=(SilentPlayer)unsafe.allocateInstance(SilentPlayer.class);
        var level=(net.minecraft.client.multiplayer.ClientLevel)unsafe.allocateInstance(net.minecraft.client.multiplayer.ClientLevel.class);
        var listener=(net.minecraft.client.multiplayer.ClientPacketListener)unsafe.allocateInstance(net.minecraft.client.multiplayer.ClientPacketListener.class);
        FeatureTest.setField(net.minecraft.client.multiplayer.ClientPacketListener.class,listener,"enabledFeatures",net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
        FeatureTest.setField(net.minecraft.client.multiplayer.ClientLevel.class,level,"connection",listener);
        FeatureTest.setField(net.minecraft.world.level.Level.class,level,"random",net.minecraft.util.RandomSource.create(13));
        FeatureTest.setField(net.minecraft.world.entity.Entity.class,player,"level",level);
        var inventory=new Inventory(player,new EntityEquipment());FeatureTest.setField(Player.class,player,"inventory",inventory);FeatureTest.setField(Player.class,player,"abilities",new Abilities());
        var menu=new FeatureTest.TestMenu(inventory);player.containerMenu=menu;
        java.util.function.BiConsumer<Integer,Integer> click=(id,b)->menu.clicked(id,b,ContainerInput.PICKUP,player);
        var drag=new BundleDrag();var carried=new CarriedBundleDrag();var source=menu.slots.get(0);var a=menu.slots.get(1);var b=menu.slots.get(2);var c=menu.slots.get(3);
        var named=new ItemStack(Items.COBBLESTONE,10);named.set(DataComponents.CUSTOM_NAME,Component.literal("Keep my name"));
        source.set(bundle(new ItemStack(Items.DIRT,12),named));BundleItem.toggleSelectedItem(source.getItem(),1);
        check(drag.begin(menu,source,player)&&drag.visit(a,player,click)&&ItemStack.matches(a.getItem(),named),"unpack preserves the selected entry and its components");
        check(!drag.visit(a,player,click)&&a.getItem().getCount()==10,"revisiting a crossed slot cannot unpack a second stack");
        check(drag.visit(b,player,click)&&b.getItem().is(Items.DIRT)&&b.getItem().getCount()==12&&source.getItem().is(Items.BUNDLE)&&menu.getCarried().isEmpty(),"remaining entry unpacks and bundle stays in its original slot");
        check(!drag.visit(c,player,click),"empty bundle stops unpacking");
        menu.chest.clearContent();source.set(bundle(named));a.set(new ItemStack(Items.DIAMOND,1));drag.begin(menu,source,player);
        check(!drag.visit(a,player,click)&&contained(source.getItem(),Items.COBBLESTONE)==10,"different items are never swapped while unpacking");
        a.set(named.copyWithCount(60));check(!drag.visit(a,player,click),"insufficient merge space skips the destination without partial extraction");
        a.set(named.copyWithCount(54));check(drag.visit(a,player,click)&&a.getItem().getCount()==64,"matching components merge with exactly enough space");
        menu.chest.clearContent();source.set(bundle(named));var pin=menu.slots.get(4);PinnedSlots.toggle(pin);
        try{drag.begin(menu,source,player);check(!drag.visit(pin,player,click)&&pin.getItem().isEmpty(),"unpacking skips pinned destinations");pin.set(bundle(named));check(!drag.begin(menu,pin,player),"pinned bundle sources stay protected");}
        finally{PinnedSlots.toggle(pin);inventory.clearContent();}
        menu.chest.clearContent();source.set(bundle(named));drag.begin(menu,source,player);source.set(bundle(new ItemStack(Items.DIRT,5)));
        check(!drag.visit(a,player,click)&&a.getItem().isEmpty(),"an externally changed bundle cancels pending unpacking");
        menu.chest.clearContent();a.set(new ItemStack(Items.COBBLESTONE,40));b.set(new ItemStack(Items.DIRT,40));menu.setCarried(bundle());
        check(carried.begin(menu,player,false)&&carried.visit(a,player,click)&&a.getItem().isEmpty(),"held empty bundle fills through vanilla pickup clicks");
        check(carried.visit(b,player,click)&&b.getItem().getCount()==16&&contained(menu.getCarried(),Items.COBBLESTONE)==40&&contained(menu.getCarried(),Items.DIRT)==24,"partial fill takes only the remaining capacity without losing items");
        check(!carried.visit(b,player,click)&&!carried.visit(c,player,click)&&menu.getCarried().is(Items.BUNDLE),"full bundle and empty fill targets leave the bundle on the cursor");
        menu.chest.clearContent();menu.setCarried(bundle());pin.set(named.copy());PinnedSlots.toggle(pin);
        try{carried.begin(menu,player,false);check(!carried.visit(pin,player,click)&&ItemStack.matches(pin.getItem(),named),"held-bundle filling skips pinned source stacks");}
        finally{PinnedSlots.toggle(pin);inventory.clearContent();}
        a.set(named.copy());carried.begin(menu,player,false);check(carried.visit(a,player,click)&&menu.getCarried().get(DataComponents.BUNDLE_CONTENTS).itemCopies().findFirst().orElseThrow().has(DataComponents.CUSTOM_NAME),"filling keeps custom names and item components");
        carried.begin(menu,player,true);check(carried.visit(b,player,click)&&ItemStack.matches(b.getItem(),named)&&menu.getCarried().is(Items.BUNDLE),"held bundle unpacks directly into an empty slot and stays on cursor");
        menu.chest.clearContent();menu.setCarried(bundle(named));a.set(new ItemStack(Items.DIRT));carried.begin(menu,player,true);
        check(!carried.visit(a,player,click)&&a.getItem().is(Items.DIRT),"held unpack never inserts into occupied slots");
        menu.setCarried(bundle(new ItemStack(Items.DIAMOND)));check(!carried.visit(b,player,click),"external cursor change cancels carried-bundle drag");
        menu.setCarried(ItemStack.EMPTY);check(!carried.begin(menu,player,false),"ordinary cursors do not start a bundle gesture");
        var oldPlayer=mc.player;var oldScreen=mc.gui.screen();boolean enabled=Stow.config.bundleDrag;
        try{
            var local=CompanionTests.previewPlayer(mc,menu);FeatureTest.setField(Player.class,local,"inventory",inventory);mc.player=local;Stow.config.bundleDrag=true;
            var screen=new FeatureTest.TestScreen(menu,inventory){@Override protected void slotClicked(Slot s,int id,int button,ContainerInput type){if(type==ContainerInput.PICKUP)menu.clicked(id,button,type,player);else super.slotClicked(s,id,button,type);}};
            mc.gui.setScreen(screen);menu.chest.clearContent();inventory.clearContent();menu.setCarried(bundle());source.set(new ItemStack(Items.DIRT,8));a.set(named.copy());b.set(new ItemStack(Items.DIAMOND,6));
            var start=screen.at(source,InputConstants.MOD_SHIFT);var end=screen.at(b,InputConstants.MOD_SHIFT);screen.mouseClicked(start,false);screen.mouseDragged(end,end.x()-start.x(),end.y()-start.y());screen.mouseReleased(end);
            check(source.getItem().isEmpty()&&a.getItem().isEmpty()&&b.getItem().isEmpty()&&contained(menu.getCarried(),Items.DIRT)==8&&contained(menu.getCarried(),Items.DIAMOND)==6,"actual Shift-left drag fills all crossed slots, including fast mouse movement");
            var rStart=new MouseButtonEvent(start.x(),start.y(),new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT,InputConstants.MOD_SHIFT));var rEnd=new MouseButtonEvent(end.x(),end.y(),rStart.buttonInfo());screen.mouseClicked(rStart,false);screen.mouseDragged(rEnd,rEnd.x()-rStart.x(),rEnd.y()-rStart.y());screen.mouseReleased(rEnd);
            check(menu.getCarried().get(DataComponents.BUNDLE_CONTENTS).isEmpty()&&source.hasItem()&&a.hasItem()&&b.hasItem(),"actual Shift-right drag unpacks across empty slots");
            menu.chest.clearContent();menu.setCarried(ItemStack.EMPTY);source.set(bundle(new ItemStack(Items.DIRT,8),named));screen.mouseClicked(start,false);screen.mouseDragged(end,end.x()-start.x(),end.y()-start.y());screen.mouseReleased(end);
            check(source.getItem().is(Items.BUNDLE)&&a.hasItem()&&b.hasItem()&&menu.getCarried().isEmpty(),"actual drag from a placed bundle unpacks and leaves its slot unchanged");
            menu.chest.clearContent();source.set(bundle(named));screen.moved.clear();screen.mouseClicked(start,false);screen.mouseReleased(start);
            check(screen.moved.equals(List.of(source.index)),"Shift-click without a drag still transfers the whole bundle");
            Stow.config.bundleDrag=false;Stow.config.save();check(!FeatureTest.reloadConfig().bundleDrag,"quick-bundle preference survives configuration reload");
        }finally{Stow.config.bundleDrag=enabled;Stow.config.save();mc.player=oldPlayer;mc.gui.setScreen(oldScreen);}
        return checks;
    }
}
