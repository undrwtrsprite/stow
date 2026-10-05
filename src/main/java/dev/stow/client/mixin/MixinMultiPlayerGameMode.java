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
    @Inject(method={"startDestroyBlock","continueDestroyBlock"},at=@At("HEAD"))
    private void stow_autoTool(net.minecraft.core.BlockPos pos,net.minecraft.core.Direction direction,CallbackInfoReturnable<Boolean> ci){
        if(dev.stow.client.inventory.ToolPicker.autoSwitch(net.minecraft.client.Minecraft.getInstance(),pos))ensureHasSentCarriedItem();
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
