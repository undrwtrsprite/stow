package dev.stow.client.inventory;

import dev.stow.Stow;
import dev.stow.client.hud.SurvivalDock;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Explicit pickup transactions into ordinary storage; machine/output menus are excluded. */
public final class SmartDeposit {
    public record Placement(Slot slot,int amount) {}
    public record Move(Slot source,List<Placement> destinations) {
        public int amount(){return destinations.stream().mapToInt(Placement::amount).sum();}
    }
    public record Plan(List<Move> moves) {
        public int amount(){return moves.stream().mapToInt(Move::amount).sum();}
        public boolean affects(Slot slot){return moves.stream().anyMatch(m->m.source()==slot);}
    }
    private static Session active;
    private static AbstractContainerScreen<?> owner;
    private SmartDeposit(){}
    public static boolean supported(AbstractContainerMenu menu){return menu instanceof ChestMenu||menu instanceof ShulkerBoxMenu;}
    public static boolean busy(AbstractContainerMenu menu){return active!=null&&active.menu==menu;}
    public static void cancel(){active=null;owner=null;}
    public static String itemId(ItemStack stack){return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();}
    public static Plan plan(AbstractContainerMenu menu,Player player,Map<String,Integer> keep,boolean protectHotbar){
        if(!supported(menu)||!menu.getCarried().isEmpty())return new Plan(List.of());
        List<Slot> storage=menu.slots.stream().filter(s->!(s.container instanceof Inventory)&&s.isActive()).toList();
        List<Slot> inventory=menu.slots.stream().filter(s->s.container instanceof Inventory&&s.getContainerSlot()>=0&&s.getContainerSlot()<36&&s.isActive()).toList();
        Map<String,Long> totals=new HashMap<>();for(Slot s:inventory)if(s.hasItem())totals.merge(itemId(s.getItem()),(long)s.getItem().getCount(),Long::sum);
        Map<Slot,ItemStack> simulated=new IdentityHashMap<>();for(Slot s:storage)simulated.put(s,s.getItem().copy());
        var moves=new ArrayList<Move>();
        for(Slot source:inventory){
            ItemStack item=source.getItem();
            if(item.isEmpty()||PinnedSlots.isPinned(source)||protectHotbar&&source.getContainerSlot()<9||player!=null&&!source.mayPickup(player))continue;
            if(storage.stream().noneMatch(s->s.hasItem()&&ItemStack.isSameItemSameComponents(item,s.getItem())))continue;
            String id=itemId(item);int amount=(int)Math.min(item.getCount(),Math.max(0,totals.getOrDefault(id,0L)-Math.max(0,keep.getOrDefault(id,0))));
            List<Placement> destinations=new ArrayList<>();
            for(boolean empty:new boolean[]{false,true})for(Slot destination:storage){
                ItemStack present=simulated.get(destination);
                if(amount==0||present.isEmpty()!=empty||!destination.mayPlace(item)||!present.isEmpty()&&!ItemStack.isSameItemSameComponents(item,present))continue;
                int capacity=Math.min(item.getMaxStackSize(),destination.getMaxStackSize(item))-present.getCount();
                int moved=Math.min(amount,Math.max(0,capacity));if(moved==0)continue;
                if(present.isEmpty())simulated.put(destination,item.copyWithCount(moved));else present.grow(moved);
                destinations.add(new Placement(destination,moved));amount-=moved;
            }
            if(!destinations.isEmpty()){
                Move move=new Move(source,List.copyOf(destinations));moves.add(move);totals.merge(id,-(long)move.amount(),Long::sum);
            }
        }
        return new Plan(List.copyOf(moves));
    }
    public static Plan preview(AbstractContainerMenu menu){return plan(menu,Minecraft.getInstance().player,Stow.config.keepAmounts,Stow.config.depositKeepHotbar);}
    public static boolean start(AbstractContainerScreen<?> screen){
        var mc=Minecraft.getInstance();var menu=screen.getMenu();
        if(mc.player==null||mc.gameMode==null||InventorySorting.busy(menu)||busy(menu)||!menu.getCarried().isEmpty())return false;
        Plan plan=preview(menu);if(plan.amount()==0){SurvivalDock.notice("stow.deposit.nothing");return false;}
        owner=screen;active=new Session(menu,plan,(slot,button)->mc.gameMode.handleContainerInput(menu.containerId,slot,button,ContainerInput.PICKUP,mc.player));return true;
    }
    public static void tick(Minecraft mc){
        if(active==null)return;
        if(mc.player==null||mc.player.containerMenu!=active.menu||mc.gui.screen()!=owner){cancel();return;}
        if(!active.step()){
            SurvivalDock.notice(active.failed?"stow.deposit.stopped":"stow.deposit.done",active.moved);cancel();
        }
    }
    /** One source stack per tick; finish every cycle with an empty cursor. */
    public static final class Session {
        private final AbstractContainerMenu menu;private final Plan plan;private final BiConsumer<Integer,Integer> click;
        private List<ItemStack> expected;private int index,moved;private boolean failed;
        public Session(AbstractContainerMenu menu,Plan plan,BiConsumer<Integer,Integer> click){this.menu=menu;this.plan=plan;this.click=click;expected=snapshot();}
        private List<ItemStack> snapshot(){return menu.slots.stream().map(s->s.getItem().copy()).toList();}
        public int moved(){return moved;}public boolean failed(){return failed;}
        public boolean step(){
            if(failed||index>=plan.moves().size()||!menu.getCarried().isEmpty())return false;
            for(int i=0;i<menu.slots.size();i++)if(!ItemStack.matches(expected.get(i),menu.slots.get(i).getItem())){failed=true;return false;}
            Move move=plan.moves().get(index++);
            if(PinnedSlots.isPinned(move.source())||!move.source().isActive()||move.destinations().stream().anyMatch(p->!p.slot().isActive()||!p.slot().mayPlace(move.source().getItem()))){failed=true;return false;}
            int original=move.source().getItem().getCount();
            click.accept(move.source().index,0);
            for(Placement destination:move.destinations()){
                if(menu.getCarried().isEmpty()){failed=true;break;}
                int capacity=Math.min(menu.getCarried().getMaxStackSize(),destination.slot().getMaxStackSize(menu.getCarried()))-destination.slot().getItem().getCount();
                if(Math.min(capacity,menu.getCarried().getCount())==destination.amount())click.accept(destination.slot().index,0);
                else for(int n=0;n<destination.amount();n++)click.accept(destination.slot().index,1);
            }
            if(!menu.getCarried().isEmpty())click.accept(move.source().index,0);
            int transferred=original-move.source().getItem().getCount();moved+=transferred;
            failed=failed||!menu.getCarried().isEmpty()||transferred!=move.amount();expected=snapshot();
            return !failed&&index<plan.moves().size();
        }
    }
}
