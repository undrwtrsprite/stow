package dev.stow.client.inventory;

import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class InventorySearch {
    private InventorySearch() {}

    public static boolean matches(ItemStack stack, String query) {
        if (stack.isEmpty()) return false;
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
        for (String word : query.strip().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (word.startsWith("@")) {
                if (!id.substring(0, id.indexOf(':')).contains(word.substring(1))) return false;
            } else if (!name.contains(word) && !id.contains(word)) {
                return false;
            }
        }
        return true;
    }
}
