package dev.stow.stripping;

import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Shared log selection and expected vanilla axe result for client and companion. */
public final class StrippableLogs {
    private static final Map<Block,Block> STRIPPED=Map.ofEntries(
        Map.entry(Blocks.OAK_LOG,Blocks.STRIPPED_OAK_LOG),
        Map.entry(Blocks.SPRUCE_LOG,Blocks.STRIPPED_SPRUCE_LOG),
        Map.entry(Blocks.BIRCH_LOG,Blocks.STRIPPED_BIRCH_LOG),
        Map.entry(Blocks.JUNGLE_LOG,Blocks.STRIPPED_JUNGLE_LOG),
        Map.entry(Blocks.ACACIA_LOG,Blocks.STRIPPED_ACACIA_LOG),
        Map.entry(Blocks.CHERRY_LOG,Blocks.STRIPPED_CHERRY_LOG),
        Map.entry(Blocks.DARK_OAK_LOG,Blocks.STRIPPED_DARK_OAK_LOG),
        Map.entry(Blocks.PALE_OAK_LOG,Blocks.STRIPPED_PALE_OAK_LOG),
        Map.entry(Blocks.MANGROVE_LOG,Blocks.STRIPPED_MANGROVE_LOG),
        Map.entry(Blocks.POPLAR_LOG,Blocks.STRIPPED_POPLAR_LOG),
        Map.entry(Blocks.CRIMSON_STEM,Blocks.STRIPPED_CRIMSON_STEM),
        Map.entry(Blocks.WARPED_STEM,Blocks.STRIPPED_WARPED_STEM)
    );
    public static boolean canStrip(BlockState state){return STRIPPED.containsKey(state.getBlock());}
    public static BlockState strippedState(BlockState state){
        var block=STRIPPED.get(state.getBlock());
        return block==null?null:block.defaultBlockState().setValue(RotatedPillarBlock.AXIS,state.getValue(RotatedPillarBlock.AXIS));
    }
    private StrippableLogs(){}
}
