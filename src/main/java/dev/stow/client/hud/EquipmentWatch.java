package dev.stow.client.hud;

import dev.stow.Stow;
import java.util.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** All damageable items across main inventory, hotbar, offhand and worn equipment. */
public final class EquipmentWatch {
    public record Entry(int slot,ItemStack item,int remaining,int maximum,int percent) {
        public boolean low(){return percent<=Stow.config.equipmentThreshold;}
        public int color(){return percent<=5?0xFFFF967D:low()?0xFFFFD486:0xFF9EE6C4;}
    }
    private EquipmentWatch(){}
    public static List<Entry> entries(Inventory inventory){
        if(inventory==null)return List.of();var result=new ArrayList<Entry>();
        for(int slot=0;slot<inventory.getContainerSize();slot++){
            ItemStack item=inventory.getItem(slot);if(item.isEmpty()||item.getMaxDamage()<=0)continue;
            int maximum=item.getMaxDamage(),remaining=Math.max(0,maximum-item.getDamageValue());
            int percent=(int)Math.ceil(remaining*100.0/maximum);
            result.add(new Entry(slot,item.copy(),remaining,maximum,percent));
        }
        result.sort(Comparator.comparingInt(Entry::percent).thenComparingInt(Entry::slot));return List.copyOf(result);
    }
    public static List<Entry> warnings(Inventory inventory){return Stow.config.equipmentWatch?entries(inventory).stream().filter(Entry::low).toList():List.of();}
    /** The screen HUD is reserved for active hotbar equipment and worn armor. */
    public static List<Entry> hudEntries(Inventory inventory){
        return entries(inventory).stream().filter(entry->{
            if(entry.slot()>=0&&entry.slot()<9)return true;
            var worn=Inventory.EQUIPMENT_SLOT_MAPPING.get(entry.slot());
            return worn==net.minecraft.world.entity.EquipmentSlot.HEAD||worn==net.minecraft.world.entity.EquipmentSlot.CHEST
                    ||worn==net.minecraft.world.entity.EquipmentSlot.LEGS||worn==net.minecraft.world.entity.EquipmentSlot.FEET;
        }).toList();
    }
}
