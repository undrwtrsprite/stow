package dev.stow.client.inventory;

import dev.stow.client.util.TextQuery;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class InventorySearch {
    private InventorySearch() {}

    public static boolean matches(ItemStack stack, String query) {
        if (stack.isEmpty()) return false;
        return TextQuery.of(query).matches(stack.getHoverName().getString(),
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),false);
    }
}
