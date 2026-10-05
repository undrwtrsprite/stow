package dev.stow.client.hud;

import dev.stow.Stow;
import java.util.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Current hands and worn equipment only; spare tools in the backpack do not raise alarms. */
public final class EquipmentWatch {
    public record Entry(int slot,ItemStack item,int remaining,int maximum,int percent) {
        public boolean low(){return percent<=Stow.config.equipmentThreshold;}
        public int color(){return percent<=5?0xFFFF967D:low()?0xFFFFD486:0xFF9EE6C4;}
    }
    private EquipmentWatch(){}
    public static List<Entry> entries(Inventory inventory){
        if(inventory==null)return List.of();var result=new ArrayList<Entry>();
        var positions=new LinkedHashSet<Integer>();positions.add(inventory.getSelectedSlot());
        for(var entry:Inventory.EQUIPMENT_SLOT_MAPPING.int2ObjectEntrySet())positions.add(entry.getIntKey());
        for(int slot:positions){
            if(slot<0||slot>=inventory.getContainerSize())continue;
            ItemStack item=inventory.getItem(slot);if(item.isEmpty()||!item.isDamageableItem()||item.getMaxDamage()<=0)continue;
            int maximum=item.getMaxDamage(),remaining=Math.max(0,maximum-item.getDamageValue());
            int percent=(int)Math.ceil(remaining*100.0/maximum);
            result.add(new Entry(slot,item.copy(),remaining,maximum,percent));
        }
        result.sort(Comparator.comparingInt(Entry::percent).thenComparingInt(Entry::slot));return List.copyOf(result);
    }
    public static List<Entry> warnings(Inventory inventory){return Stow.config.equipmentWatch?entries(inventory).stream().filter(Entry::low).toList():List.of();}
}
