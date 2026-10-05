package dev.stow.client.memory;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.*;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Submit only the selected storage silhouette into vanilla's spectral/entity outline phase. */
public final class NativeChestOutline {
    public static final int COLOR=0xFFFFFFFF;
    public record Entry(BlockPos position,BlockEntityRenderState entity,BlockModelRenderState model) {}
    public interface Frame {List<Entry> stow$outlines();void stow$outlines(List<Entry> entries);}
    private NativeChestOutline(){}
    public static List<BlockPos> positions(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
        return state.getBlock() instanceof ChestBlock&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE?List.of(pos,ChestBlock.getConnectedBlockPos(pos,state)):List.of(pos);
    }
    public static void extract(Minecraft mc,LevelRenderState frame){
        var output=(Frame)frame;output.stow$outlines(List.of());
        var selected=ChestMemory.selected();if(ChestMemory.glowTarget()==null||selected==null||mc.level==null||!mc.level.hasChunkAt(selected.pos()))return;
        var state=mc.level.getBlockState(selected.pos());if(!(state.getBlock() instanceof ChestBlock||state.getBlock() instanceof BarrelBlock||state.getBlock() instanceof ShulkerBoxBlock))return;
        var entries=new ArrayList<Entry>();var resolver=new BlockModelResolver(mc.getModelManager());
        for(var pos:positions(selected.pos(),state)){
            var block=mc.level.getBlockState(pos);var entity=mc.level.getBlockEntity(pos);
            var render=entity==null?null:mc.levelRenderer.blockEntityRenderDispatcher().tryExtractRenderState(entity,frame.worldPartialTicks,null,false);
            BlockModelRenderState model=null;
            if(render==null){model=new BlockModelRenderState();resolver.update(model,block,BlockDisplayContext.create());}
            entries.add(new Entry(pos,render,model));
        }
        output.stow$outlines(List.copyOf(entries));
    }
    public static boolean ready(LevelRenderState frame){return !((Frame)frame).stow$outlines().isEmpty()&&frame.shouldShowEntityOutlines;}
    public static void submit(LevelRenderState frame,SubmitNodeStorage destination,net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher dispatcher){
        if(!ready(frame))return;
        var temporary=new OutlineStorage();var pose=new PoseStack();var camera=frame.cameraRenderState;
        for(var entry:((Frame)frame).stow$outlines()){
            pose.pushPose();pose.translate(entry.position().getX()-camera.pos.x,entry.position().getY()-camera.pos.y,entry.position().getZ()-camera.pos.z);
            if(entry.entity()!=null)dispatcher.submit(entry.entity(),pose,temporary,camera);
            else if(entry.model()!=null)entry.model().submit(pose,temporary,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,COLOR);
            pose.popPose();
        }
        copyOutlines(temporary,destination);
    }
    /** Discard every solid/translucent submit: no duplicate chest skin, scene depth or fullscreen tint. */
    public static void copyOutlines(SubmitNodeStorage source,SubmitNodeStorage destination){
        source.getSubmitsPerOrder().forEach((order,collection)->collection.outline.sortInto((node,translucent)->destination.order(order).outline.submit(node)));
    }
    private static final class OutlineStorage extends SubmitNodeStorage {
        @Override public <S> void submitModel(Model<? super S> model,S state,PoseStack pose,RenderType type,int light,int overlay,int tint,UvMapping uv,int outline){super.submitModel(model,state,pose,type,light,overlay,tint,uv,COLOR);}
    }
}
