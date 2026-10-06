package dev.stow.client.inventory;

import dev.stow.Stow;
import dev.stow.client.mixin.ClientLevelAccessor;
import dev.stow.stripping.StrippableLogs;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** One explicit shortcut batches ordinary axe uses, with one server-confirmed request at a time. */
public final class BulkStrip {
    private static final int LIMIT=64, USE_INTERVAL=4, ACK_TIMEOUT=100;
    private record Target(BlockPos pos,BlockState state) {}
    private static final class Batch {
        final LocalPlayer player;
        final ClientLevel level;
        final int selected;
        final ItemStack axe;
        final ArrayDeque<Target> targets;
        Target waiting;
        int sequence,elapsed,cooldown,stripped,skipped;
        Batch(Minecraft mc,List<Target> targets){
            player=mc.player;level=mc.level;selected=player.getInventory().getSelectedSlot();
            axe=identity(player.getMainHandItem());this.targets=new ArrayDeque<>(targets);
        }
    }
    private static Batch batch;
    private BulkStrip(){}
    public static boolean busy(){return batch!=null||CompanionHarvest.busy();}
    public static void cancel(){batch=null;CompanionHarvest.clear();}
    private static ItemStack identity(ItemStack item){
        var copy=item.copy();if(copy.isDamageableItem())copy.setDamageValue(0);return copy;
    }
    private static boolean usableAxe(ItemStack item){
        return item.is(ItemTags.AXES)&&(!item.isDamageableItem()||item.getMaxDamage()-item.getDamageValue()>1);
    }
    private static boolean ready(Minecraft mc){
        if(mc.player==null||mc.level==null||mc.gameMode==null||mc.gui.screen()!=null
                ||mc.player.isUsingItem()||mc.gameMode.isDestroying()||mc.options.keyAttack.isDown()||mc.options.keyUse.isDown())return false;
        var mode=mc.gameMode.getPlayerMode();
        if(mode!=GameType.SURVIVAL&&mode!=GameType.CREATIVE)return false;
        var menu=mc.player.inventoryMenu;
        return menu!=null&&mc.player.containerMenu==menu&&menu.getCarried().isEmpty()
                &&!InventorySorting.busy(menu)&&!SmartDeposit.busy(menu);
    }
    private static void message(Minecraft mc,String key,Object... arguments){
        if(mc.player!=null)mc.gui.hud.setOverlayMessage(Component.translatable("stow.bulk-strip."+key,arguments),false);
    }
    private static void finish(Minecraft mc,String key){
        var current=batch;batch=null;
        if(current!=null)message(mc,key,current.stripped,current.skipped);
    }
    /** Trace visible faces and respect the player's actual interaction range; never reach through blocks. */
    private static BlockHitResult visibleHit(Minecraft mc,BlockPos pos){
        if(!mc.player.isWithinBlockInteractionRange(pos,0))return null;
        var eye=mc.player.getEyePosition();double reach=mc.player.blockInteractionRange();
        var center=Vec3.atCenterOf(pos);
        for(var face:Direction.values()){
            var point=center.add(face.getStepX()*0.499,face.getStepY()*0.499,face.getStepZ()*0.499);
            var hit=mc.level.clip(new ClipContext(eye,point,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,mc.player));
            if(hit.getType()==HitResult.Type.BLOCK&&hit.getBlockPos().equals(pos)&&eye.distanceToSqr(hit.getLocation())<=reach*reach)return hit;
        }
        return null;
    }
    public static boolean toggle(Minecraft mc){
        if(CompanionHarvest.busy()){CompanionHarvest.cancel();return true;}
        if(batch!=null){finish(mc,"cancelled");return true;}
        if(!Stow.config.bulkStrip){message(mc,"disabled");return true;}
        if(!ready(mc)){message(mc,"unavailable");return true;}
        if(!usableAxe(mc.player.getMainHandItem())){message(mc,"axe");return true;}
        if(!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK
                ||!StrippableLogs.canStrip(mc.level.getBlockState(hit.getBlockPos()))||visibleHit(mc,hit.getBlockPos())==null){
            message(mc,"aim");return true;
        }
        if(CompanionHarvest.available()){CompanionHarvest.start(mc,hit.getBlockPos());return true;}
        var log=mc.level.getBlockState(hit.getBlockPos()).getBlock();
        var eye=mc.player.getEyePosition();var center=BlockPos.containing(eye);
        // Bound scanning even on servers with unusually large interaction-range attributes.
        int radius=Math.min(8,(int)Math.ceil(mc.player.blockInteractionRange()));
        var candidates=new ArrayList<Target>();
        for(var pos:BlockPos.betweenClosed(center.offset(-radius,-radius,-radius),center.offset(radius,radius,radius))){
            var state=mc.level.getBlockState(pos);
            if(state.is(log)&&visibleHit(mc,pos)!=null)candidates.add(new Target(pos.immutable(),state));
        }
        candidates.sort(Comparator.<Target>comparingInt(t->t.pos.equals(hit.getBlockPos())?0:1)
                .thenComparingDouble(t->eye.distanceToSqr(Vec3.atCenterOf(t.pos))));
        if(candidates.isEmpty()){message(mc,"aim");return true;}
        batch=new Batch(mc,List.copyOf(candidates.subList(0,Math.min(LIMIT,candidates.size()))));
        HandRefill.cancel();message(mc,"started",batch.targets.size());return true;
    }
    public static void tick(Minecraft mc){
        CompanionHarvest.tick(mc,Stow.config.bulkStrip&&ready(mc));
        var current=batch;if(current==null)return;
        if(!Stow.config.bulkStrip||!ready(mc)||mc.player!=current.player||mc.level!=current.level
                ||mc.player.getInventory().getSelectedSlot()!=current.selected
                ||!usableAxe(mc.player.getMainHandItem())
                ||!ItemStack.isSameItemSameComponents(identity(mc.player.getMainHandItem()),current.axe)){
            finish(mc,"cancelled");return;
        }
        if(current.waiting!=null){if(++current.elapsed>=ACK_TIMEOUT)finish(mc,"rejected");return;}
        if(current.cooldown>0){current.cooldown--;return;}
        while(!current.targets.isEmpty()){
            var target=current.targets.removeFirst();
            if(mc.level.getBlockState(target.pos)!=target.state){current.skipped++;continue;}
            var hit=visibleHit(mc,target.pos);
            if(hit==null){current.skipped++;continue;}
            var result=mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,hit);
            if(!result.consumesAction()){finish(mc,"rejected");return;}
            current.waiting=target;current.elapsed=0;
            current.sequence=((ClientLevelAccessor)mc.level).stow_predictionHandler().currentSequence();
            if(result instanceof InteractionResult.Success success&&success.swingSource()==InteractionResult.SwingSource.PREDICTED)
                mc.player.swing(InteractionHand.MAIN_HAND,mc.player.getMainHandItem().getInteractAnimation(),false);
            return;
        }
        finish(mc,"done");
    }
    /** Called after vanilla reconciles predicted states with this server acknowledgement. */
    public static void acknowledged(Minecraft mc,int sequence){
        var current=batch;
        if(current==null||current.waiting==null||sequence<current.sequence||mc.level!=current.level)return;
        var target=current.waiting;var state=mc.level.getBlockState(target.pos);
        if(state!=StrippableLogs.strippedState(target.state)){
            finish(mc,"rejected");return;
        }
        current.stripped++;current.waiting=null;current.cooldown=USE_INTERVAL;
    }
}
