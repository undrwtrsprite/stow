package dev.stow.client.inventory;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One gesture has one fixed drag mode and one source inventory. */
public final class MatchingItemDrag {
    private ItemStack target = ItemStack.EMPTY;
    private Container source;
    private boolean matchingOnly;
    private final Set<Slot> visited = new HashSet<>();

    public void begin(Slot first) {
        begin(first, true);
    }

    public void begin(Slot first, boolean matchingOnly) {
        clear();
        this.matchingOnly = matchingOnly;
        if (first != null) {
            target = first.getItem().copy();
            source = first.container;
        }
    }

    public boolean visit(Slot slot, Player player) {
        return slot != null && slot.hasItem() && slot.container == source
                && slot.isActive() && !PinnedSlots.isPinned(slot) && slot.mayPickup(player)
                && (!matchingOnly || !target.isEmpty() && ItemStack.isSameItemSameComponents(target, slot.getItem()))
                && visited.add(slot);
    }

    public void clear() {
        target = ItemStack.EMPTY;
        source = null;
        matchingOnly = true;
        visited.clear();
    }
}
