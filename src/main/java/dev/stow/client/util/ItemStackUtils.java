/*
 * Copyright 2020 Siphalor and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

package dev.stow.client.util;

import com.google.common.collect.Sets;
import dev.stow.Stow;
import java.awt.*;
import java.util.*;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;


public class ItemStackUtils {
    private static final org.slf4j.Logger log=Stow.createLogger(ItemStackUtils.class);
	private static final Item.TooltipContext TOOLTIP_CONTEXT = Item.TooltipContext.EMPTY;

	public static boolean hasCustomData(ItemStack stack) {
		return !stack.getComponentsPatch().isEmpty();
	}

	public static int getMaxStackSize(ItemStack stack) {
		return stack.getMaxStackSize();
	}

	public static boolean canCombine(ItemStack a, ItemStack b) {
		return ItemStack.isSameItemSameComponents(a, b);
	}

	public static int compareEqualItems(ItemStack a, ItemStack b) {
		// compare counts
		int cmp = Integer.compare(b.getCount(), a.getCount());
		if (cmp != 0) {
			return cmp;
		}
		return compareEqualItems2(a, b);
	}

	private static int compareEqualItems2(ItemStack a, ItemStack b) {
		// compare names
		if (hasCustomName(a)) {
			if (!hasCustomName(b)) {
				return -1;
			}
			return compareEqualItems3(a, b);
		}
		if (hasCustomName(b)) {
			return 1;
		}
		return compareEqualItems3(a, b);
	}

	private static boolean hasCustomName(ItemStack stack) {
		return stack.has(DataComponents.CUSTOM_NAME);
	}

	private static int compareEqualItems3(ItemStack a, ItemStack b) {
		// compare tooltips
		Iterator<Component> tooltipsA = getTooltipLines(a);
		Iterator<Component> tooltipsB = getTooltipLines(b);

		while (tooltipsA.hasNext()) {
			if (!tooltipsB.hasNext()) {
				return 1;
			}

			int cmp = tooltipsA.next().getString().compareToIgnoreCase(tooltipsB.next().getString());
			if (cmp != 0) {
				return cmp;
			}
		}
		if (tooltipsB.hasNext()) {
			return -1;
		}
		return compareEqualItems4(a, b);
	}

	private static Iterator<Component> getTooltipLines(ItemStack stack) {
		try {
			return stack.getTooltipLines(TOOLTIP_CONTEXT, null, TooltipFlag.Default.NORMAL).iterator();
		} catch (Exception e) {
			log.debug("Uncaught exception while resolving tooltip of stack {}", stack, e);
			return Collections.emptyIterator();
		}
	}

	private static int compareEqualItems4(ItemStack a, ItemStack b) {
		// compare color
		int colorA = DyedItemColor.getOrDefault(a, 0);
		int colorB = DyedItemColor.getOrDefault(b, 0);
		if (colorA == 0 && colorB != 0) {
			return -1;
		} else if (colorA != 0 && colorB == 0) {
			return 1;
		} else if (colorA != 0) {
			float[] hsbA = Color.RGBtoHSB(colorA >> 16 & 0xFF, colorA >> 8 & 0xFF, colorA & 0xFF, null);
			float[] hsbB = Color.RGBtoHSB(colorB >> 16 & 0xFF, colorB >> 8 & 0xFF, colorB & 0xFF, null);
			int cmp = Float.compare(hsbA[0], hsbB[0]);
			if (cmp != 0) {
				return cmp;
			}
			cmp = Float.compare(hsbA[1], hsbB[1]);
			if (cmp != 0) {
				return cmp;
			}
			cmp = Float.compare(hsbA[2], hsbB[2]);
			if (cmp != 0) {
				return cmp;
			}
		}
		return compareEqualItems5(a, b);
	}

	private static int compareEqualItems5(ItemStack a, ItemStack b) {
		// compare damage
		return Integer.compare(a.getDamageValue(), b.getDamageValue());
	}

}
