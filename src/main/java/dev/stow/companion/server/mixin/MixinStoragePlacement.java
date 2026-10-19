package dev.stow.companion.server.mixin;
import dev.stow.companion.server.StorageOwnerServer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(BlockItem.class)
public abstract class MixinStoragePlacement {
 @Redirect(method="place",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/Block;setPlacedBy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"))
 private void stow$placed(Block block,Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){block.setPlacedBy(level,pos,state,placer,stack);StorageOwnerServer.placed(level,pos,placer);}
}
