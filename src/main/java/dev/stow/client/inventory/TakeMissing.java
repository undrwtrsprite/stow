package dev.stow.client.inventory;

import dev.stow.Stow;
import dev.stow.client.memory.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/**
 * Project "Take missing": moves only what the active project still needs carried, only from the
 * open chest or shulker box, through the same verified vanilla pickup session as Smart deposit.
 * Pinned player slots are never filled; destinations must match item and components exactly.
 */
public final class TakeMissing {
    private TakeMissing(){}
    /** Amount still to carry per item: target minus what is already in the 36 player slots. */
    public static Map<String,Integer> stillToCarry(ChestMemoryStore store,Player player){
        var result=new LinkedHashMap<String,Integer>();
        if(store==null||player==null||store.isFinished())return result;
        var carried=new HashMap<String,Long>();var inventory=player.getInventory();
        for(int i=0;i<36;i++){var stack=inventory.getItem(i);if(!stack.isEmpty())carried.merge(SmartDeposit.itemId(stack),(long)stack.getCount(),Long::sum);}
        for(var goal:store.goals()){long need=goal.target()-carried.getOrDefault(goal.itemId(),0L);if(need>0)result.put(goal.itemId(),(int)Math.min(need,Integer.MAX_VALUE));}
        return result;
    }
    public static SmartDeposit.Plan plan(AbstractContainerMenu menu,Player player,ChestMemoryStore store){
        if(!Stow.config.takeMissing||!SmartDeposit.supported(menu)||!menu.getCarried().isEmpty()||player==null)return new SmartDeposit.Plan(List.of());
        var need=stillToCarry(store,player);if(need.isEmpty())return new SmartDeposit.Plan(List.of());
        List<Slot> storage=menu.slots.stream().filter(s->!(s.container instanceof Inventory)&&s.isActive()).toList();
        // Main inventory before the hotbar, so the hotbar layout is disturbed last.
        List<Slot> inventory=new ArrayList<>(menu.slots.stream().filter(s->s.container instanceof Inventory&&s.getContainerSlot()>=9&&s.getContainerSlot()<36&&s.isActive()).toList());
        inventory.addAll(menu.slots.stream().filter(s->s.container instanceof Inventory&&s.getContainerSlot()>=0&&s.getContainerSlot()<9&&s.isActive()).toList());
        Map<Slot,ItemStack> simulated=new IdentityHashMap<>();for(Slot s:inventory)simulated.put(s,s.getItem().copy());
        var remaining=new HashMap<>(need);var moves=new ArrayList<SmartDeposit.Move>();
        for(Slot source:storage){
            ItemStack item=source.getItem();if(item.isEmpty()||!source.mayPickup(player))continue;
            String id=SmartDeposit.itemId(item);int amount=Math.min(item.getCount(),remaining.getOrDefault(id,0));if(amount<=0)continue;
            List<SmartDeposit.Placement> destinations=new ArrayList<>();
            for(boolean empty:new boolean[]{false,true})for(Slot destination:inventory){
                ItemStack present=simulated.get(destination);
                if(amount==0||PinnedSlots.isPinned(destination)||present.isEmpty()!=empty||!destination.mayPlace(item)||!present.isEmpty()&&!ItemStack.isSameItemSameComponents(item,present))continue;
                int capacity=Math.min(item.getMaxStackSize(),destination.getMaxStackSize(item))-present.getCount();
                int moved=Math.min(amount,Math.max(0,capacity));if(moved==0)continue;
                if(present.isEmpty())simulated.put(destination,item.copyWithCount(moved));else present.grow(moved);
                destinations.add(new SmartDeposit.Placement(destination,moved));amount-=moved;
            }
            if(!destinations.isEmpty()){var move=new SmartDeposit.Move(source,List.copyOf(destinations));moves.add(move);remaining.merge(id,-move.amount(),Integer::sum);}
        }
        return new SmartDeposit.Plan(List.copyOf(moves));
    }
    /** Item ids worth highlighting in this container: still needed and present in at least one slot. */
    public static Set<String> highlighted(AbstractContainerMenu menu,Player player,ChestMemoryStore store){
        if(!SmartDeposit.supported(menu))return Set.of();
        var need=stillToCarry(store,player);if(need.isEmpty())return Set.of();
        var found=new HashSet<String>();
        for(Slot slot:menu.slots)if(!(slot.container instanceof Inventory)&&slot.hasItem()){String id=SmartDeposit.itemId(slot.getItem());if(need.containsKey(id))found.add(id);}
        return Set.copyOf(found);
    }
    public static boolean start(AbstractContainerScreen<?> screen){
        var mc=Minecraft.getInstance();var plan=plan(screen.getMenu(),mc.player,ChestMemory.currentStore());
        if(plan.amount()==0){dev.stow.client.ui.UiNotifications.show("stow.take.nothing");return false;}
        return SmartDeposit.startTransfer(screen,plan,"stow.take.done","stow.take.stopped");
    }
}
