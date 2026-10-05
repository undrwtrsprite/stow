package dev.stow.client.mixin;
import java.util.List;
import dev.stow.client.memory.NativeChestOutline;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(LevelRenderState.class)
public abstract class LevelRenderStateOutlineMixin implements NativeChestOutline.Frame {
    @Unique private List<NativeChestOutline.Entry> stow$outlines=List.of();
    public List<NativeChestOutline.Entry> stow$outlines(){return stow$outlines;}
    public void stow$outlines(List<NativeChestOutline.Entry> entries){stow$outlines=entries;}
}
