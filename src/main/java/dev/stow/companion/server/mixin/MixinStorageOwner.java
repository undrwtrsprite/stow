package dev.stow.companion.server.mixin;
import dev.stow.companion.server.OwnedStorage;
import dev.stow.companion.protocol.StorageOwnerProtocol.Owner;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.storage.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BaseContainerBlockEntity.class)
public abstract class MixinStorageOwner implements OwnedStorage {
 @Unique private Owner stow$owner;
 public Owner stow$owner(){return stow$owner;}
 public void stow$owner(Owner owner){stow$owner=owner;((BaseContainerBlockEntity)(Object)this).setChanged();}
 @Inject(method="saveAdditional",at=@At("TAIL"))
 private void stow$save(ValueOutput out,CallbackInfo ci){if(stow$owner!=null){var tag=out.child("stow_owner");tag.putString("id",stow$owner.id());tag.putString("name",stow$owner.name());tag.putBoolean("shared",stow$owner.shared());}}
 @Inject(method="loadAdditional",at=@At("TAIL"))
 private void stow$load(ValueInput in,CallbackInfo ci){stow$owner=null;var tag=in.childOrEmpty("stow_owner");String id=tag.getStringOr("id",""),name=tag.getStringOr("name","");try{java.util.UUID.fromString(id);if(!name.isBlank()&&name.length()<=48)stow$owner=new Owner(id,name,tag.getBooleanOr("shared",false));}catch(IllegalArgumentException ignored){}}
}
