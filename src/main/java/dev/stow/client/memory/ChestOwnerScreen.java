package dev.stow.client.memory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import static dev.stow.client.memory.ChestMemoryStore.*;
import static dev.stow.companion.protocol.StorageOwnerProtocol.*;
public final class ChestOwnerScreen extends MemoryScreenBase {
 private final ChestMemoryStore store;private final Location location;private final java.util.List<PlannerButton> actions=new java.util.ArrayList<>();
 public ChestOwnerScreen(Screen parent,ChestMemoryStore store,Location location){super(Component.translatable("stow.owner.title"),parent,store);this.store=store;this.location=location;}
 protected void init(){layoutDialog(height);actions.clear();StorageOwners.request(store,location,QUERY);int w=Math.min(260,contentWidth()-16),x=contentLeft()+(contentWidth()-w)/2,y=Math.max(70,height/2-30);
  int[] ids={CLAIM,SHARE,PRIVATE};String[] keys={"stow.owner.claim","stow.owner.share","stow.owner.private"};for(int i=0;i<ids.length;i++){int action=ids[i];var b=new PlannerButton(Component.translatable(keys[i]),button->StorageOwners.request(store,location,action),x,y+i*26,w,22,false);actions.add(addRenderableWidget(b));}
  addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b->onClose(),contentLeft()+8,height-30,66,22,false));tick();
 }
 public void tick(){for(var b:actions)b.active=StorageOwners.editable(location);}
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){extractMenuBackdrop(g,delta);var chest=store.get(location);int y=Math.max(70,height/2-30);g.centeredText(font,chest==null?"":store.displayName(chest),width/2,y-42,0xFFFFFFFF);g.centeredText(font,Component.translatable("stow.owner.current",chest==null?"":StorageOwners.label(chest)),width/2,y-26,0xFFB8D8CE);g.textWithWordWrap(font,Component.translatable(!StorageOwners.available()?"stow.owner.requires-companion":StorageOwners.editable(location)?"stow.owner.info":"stow.owner.open-first"),contentLeft()+8,y+84,contentWidth()-16,0xFFAAAAAA);super.extractRenderState(g,mx,my,delta);}
}
