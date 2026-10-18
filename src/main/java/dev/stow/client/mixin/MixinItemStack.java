package dev.stow.client.mixin;

import dev.stow.client.ui.ShulkerPreview;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {
    @Inject(method="addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",at=@At("HEAD"),cancellable=true)
    private void stow_skipDuplicateContents(DataComponentType<?> type,Item.TooltipContext context,TooltipDisplay display,Consumer<Component> lines,TooltipFlag flag,CallbackInfo ci){
        // The preview already shows every stack; keep all other item details intact.
        if(type==DataComponents.CONTAINER&&((ItemStack)(Object)this).getTooltipImage().orElse(null) instanceof ShulkerPreview.Contents)ci.cancel();
    }
    @Inject(method="getTooltipImage",at=@At("RETURN"),cancellable=true)
    private void stow_shulkerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir){
        if(cir.getReturnValue().isEmpty())ShulkerPreview.contents((ItemStack)(Object)this).ifPresent(contents->cir.setReturnValue(Optional.of(contents)));
    }
}
