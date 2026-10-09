package dev.stow.client.mixin;
import dev.stow.client.memory.ChestMemory;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.*;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode {
    @org.spongepowered.asm.mixin.Shadow private void ensureHasSentCarriedItem(){throw new AssertionError();}
    @Inject(method={"startDestroyBlock","continueDestroyBlock"},at=@At("HEAD"),cancellable=true)
    private void stow_autoTool(net.minecraft.core.BlockPos pos,net.minecraft.core.Direction direction,CallbackInfoReturnable<Boolean> ci){
        if(dev.stow.client.inventory.ToolPicker.autoSwitch(net.minecraft.client.Minecraft.getInstance(),pos))ensureHasSentCarriedItem();
        var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player!=null&&dev.stow.client.inventory.ToolRefill.protect(mc,dev.stow.client.inventory.ToolRefill.miningCost(mc.player.getMainHandItem())))ci.setReturnValue(false);
    }
    @Inject(method="attack",at=@At("HEAD"),cancellable=true)
    private void stow_protectAttack(net.minecraft.world.entity.player.Player player,net.minecraft.world.entity.Entity entity,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){var mc=net.minecraft.client.Minecraft.getInstance();if(dev.stow.client.inventory.ToolRefill.protect(mc,dev.stow.client.inventory.ToolRefill.attackCost(player.getMainHandItem())))ci.cancel();}
    @Inject(method="useItem",at=@At("HEAD"),cancellable=true)
    private void stow_protectUse(net.minecraft.world.entity.player.Player player,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){if(hand==InteractionHand.MAIN_HAND&&dev.stow.client.inventory.ToolRefill.protect(net.minecraft.client.Minecraft.getInstance(),dev.stow.client.inventory.ToolRefill.useCost(player.getMainHandItem())))ci.setReturnValue(InteractionResult.FAIL);}
    @Inject(method="piercingAttack",at=@At("HEAD"),cancellable=true)
    private void stow_protectPiercing(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player!=null&&dev.stow.client.inventory.ToolRefill.protect(mc,dev.stow.client.inventory.ToolRefill.attackCost(mc.player.getMainHandItem())))ci.cancel();}
    @Inject(method="interact",at=@At("HEAD"),cancellable=true)
    private void stow_protectShearing(net.minecraft.world.entity.player.Player player,net.minecraft.world.entity.Entity entity,net.minecraft.world.phys.EntityHitResult hit,InteractionHand hand,CallbackInfoReturnable<InteractionResult> ci){
        if(hand==InteractionHand.MAIN_HAND&&player.getMainHandItem().getItem() instanceof net.minecraft.world.item.ShearsItem&&entity instanceof net.minecraft.world.entity.Shearable shearable&&shearable.readyForShearing()&&dev.stow.client.inventory.ToolRefill.protect(net.minecraft.client.Minecraft.getInstance(),1))ci.setReturnValue(InteractionResult.FAIL);
    }
    @Inject(method="useItemOn",at=@At("HEAD"),cancellable=true)
    private void stow_protectBlockShearing(LocalPlayer player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(hand==InteractionHand.MAIN_HAND&&mc.level!=null&&player.getMainHandItem().getItem() instanceof net.minecraft.world.item.ShearsItem){
            var block=mc.level.getBlockState(hit.getBlockPos());
            if((block.is(net.minecraft.world.level.block.Blocks.PUMPKIN)||block.is(net.minecraft.tags.BlockTags.BEEHIVES))&&dev.stow.client.inventory.ToolRefill.protect(mc,1))ci.setReturnValue(InteractionResult.FAIL);
        }
    }
    @Redirect(method="performUseItemOn",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult stow_protectUseOn(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.context.UseOnContext context){
        var mc=net.minecraft.client.Minecraft.getInstance();
        if(context.getHand()!=InteractionHand.MAIN_HAND||context.getPlayer()!=mc.player)return stack.useOn(context);
        int cost=dev.stow.client.inventory.ToolRefill.useOnCost(stack);
        if(cost>0&&dev.stow.client.inventory.ToolRefill.protect(mc,cost))return InteractionResult.FAIL;
        return mc.player.getMainHandItem().useOn(context);
    }
    @Inject(method="handleContainerInput",at=@At("HEAD"),cancellable=true)
    private void stow_protectPinnedDestinations(int containerId,int slotId,int button,net.minecraft.world.inventory.ContainerInput input,net.minecraft.world.entity.player.Player player,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
        if(input!=net.minecraft.world.inventory.ContainerInput.QUICK_MOVE||player==null||player.containerMenu==null||player.containerMenu.containerId!=containerId||slotId<0||slotId>=player.containerMenu.slots.size())return;
        var mode=(MultiPlayerGameMode)(Object)this;
        if(dev.stow.client.inventory.PinnedTransfer.move(player.containerMenu,player.containerMenu.slots.get(slotId),player,(id,b)->mode.handleContainerInput(containerId,id,b,net.minecraft.world.inventory.ContainerInput.PICKUP,player)))ci.cancel();
    }
    @org.spongepowered.asm.mixin.Unique private net.minecraft.world.item.ItemStack stow_before=net.minecraft.world.item.ItemStack.EMPTY;
    @org.spongepowered.asm.mixin.Unique private int stow_selected;
    @Inject(method="useItemOn",at=@At("HEAD"))
    private void stow_beforePlacement(LocalPlayer player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){
        stow_selected=player.getInventory().getSelectedSlot();stow_before=hand==InteractionHand.MAIN_HAND?player.getInventory().getSelectedItem().copy():net.minecraft.world.item.ItemStack.EMPTY;
    }
    @Inject(method="useItemOn",at=@At("RETURN"))
    private void stow_afterPlacement(LocalPlayer player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){
        if(ci.getReturnValue().consumesAction())dev.stow.client.inventory.HandRefill.placed(player,stow_selected,stow_before);
        stow_before=net.minecraft.world.item.ItemStack.EMPTY;
    }
    @Inject(method={"interact","useItem"},at=@At("HEAD"))
    private void stow_clear(CallbackInfoReturnable<InteractionResult> ci){ChestMemory.clearPending();}
    @Inject(method="useItemOn",at=@At("HEAD"))
    private void stow_clicked(LocalPlayer player,InteractionHand hand,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){ChestMemory.clicked(hit);}
}
