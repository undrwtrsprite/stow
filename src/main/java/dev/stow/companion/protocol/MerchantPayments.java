package dev.stow.companion.protocol;

import java.util.function.IntConsumer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;

/** Top up payment slots in place, keeping partial payments out of the player's inventory. */
public final class MerchantPayments {
    public static boolean refill(MerchantMenu menu,int selected,Player player,IntConsumer pickup){
        if(!menu.getCarried().isEmpty()||selected<0||selected>=menu.getOffers().size())return false;
        var offer=menu.getOffers().get(selected);
        var a=offer.getItemCostA();var b=offer.getItemCostB();
        var left=menu.getSlot(0).getItem();var right=menu.getSlot(1).getItem();
        // Vanilla also accepts payment inputs in the opposite order.
        boolean reversed=(!left.isEmpty()&&!a.test(left)&&b.isPresent()&&b.get().test(left))
            ||(!right.isEmpty()&&a.test(right)&&(b.isEmpty()||!b.get().test(right)));
        int first=reversed?1:0,second=reversed?0:1;
        if(!compatible(menu.getSlot(first).getItem(),a)
                ||(b.isPresent()?!compatible(menu.getSlot(second).getItem(),b.get()):menu.getSlot(second).hasItem()))return false;
        return topUp(menu,first,a,player,pickup)
            &&(b.isEmpty()||topUp(menu,second,b.get(),player,pickup))&&menu.getCarried().isEmpty();
    }
    private static boolean compatible(ItemStack stack,ItemCost cost){return stack.isEmpty()||cost.test(stack);}
    private static boolean topUp(MerchantMenu menu,int payment,ItemCost cost,Player player,IntConsumer pickup){
        var target=menu.getSlot(payment);
        for(int i=3;i<menu.slots.size();i++){
            var source=menu.getSlot(i);var donor=source.getItem();var current=target.getItem();
            if(!menu.getCarried().isEmpty())return false;
            if(donor.isEmpty()||source.container!=player.getInventory()||!source.isActive()||!source.mayPickup(player)
                    ||!cost.test(donor)||!target.isActive()||!target.mayPlace(donor)
                    ||(!current.isEmpty()&&!ItemStack.isSameItemSameComponents(current,donor)))continue;
            if(current.getCount()>=Math.min(donor.getMaxStackSize(),target.getMaxStackSize(donor)))break;
            pickup.accept(i);
            if(menu.getCarried().isEmpty())return false;
            pickup.accept(payment);
            if(!menu.getCarried().isEmpty()){
                var remaining=source.getItem();
                if(!remaining.isEmpty()&&(!ItemStack.isSameItemSameComponents(remaining,menu.getCarried())
                        ||remaining.getCount()>=source.getMaxStackSize(menu.getCarried())))return false;
                pickup.accept(i);
                if(!menu.getCarried().isEmpty())return false;
            }
        }
        return true;
    }
    private MerchantPayments(){}
}
