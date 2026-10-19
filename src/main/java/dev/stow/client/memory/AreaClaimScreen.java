package dev.stow.client.memory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import dev.stow.companion.protocol.AreaClaimProtocol;
public final class AreaClaimScreen extends MemoryScreenBase {
 private final ChestMemoryStore store;private BlockPos first,second;private PlannerButton fly,claim,cancel;private boolean progress=AreaClaims.running();
 public AreaClaimScreen(Screen parent,ChestMemoryStore store){super(Component.translatable("stow.area.title"),parent,store);this.store=store;}
 public void selection(BlockPos a,BlockPos b){first=a;second=b;progress=false;}
 protected void init(){layoutDialog(height);int w=Math.min(300,contentWidth()-16),x=contentLeft()+(contentWidth()-w)/2,y=Math.max(132,height/2);
  fly=addRenderableWidget(new PlannerButton(Component.translatable("stow.area.fly"),b->AreaSelection.start(this),x,y,w,22,false));
  claim=addRenderableWidget(new PlannerButton(Component.translatable("stow.area.claim"),b->{AreaClaims.start(store,first,second);progress=true;},x,y+26,w,22,false));
  cancel=addRenderableWidget(new PlannerButton(Component.translatable("stow.area.cancel"),b->AreaClaims.cancel(),x,y+52,w,22,false));
  addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b->onClose(),contentLeft()+8,height-30,66,22,false));fly.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("stow.area.info")));tick();
 }
 public void tick(){boolean running=AreaClaims.running();fly.active=AreaClaims.available()&&!running;claim.active=AreaClaims.available()&&!running&&first!=null&&second!=null&&AreaClaims.validSelection(first,second);cancel.active=running;}
 public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){extractMenuBackdrop(g,delta);int x=contentLeft()+8,y=Math.max(132,height/2);
  g.textWithWordWrap(font,Component.translatable(AreaClaims.available()?"stow.area.summary":"stow.area.requires"),x,44,contentWidth()-16,0xFFB8C6CC);
  if(!progress&&first!=null&&second!=null)g.textWithWordWrap(font,Component.translatable("stow.area.box",first.toShortString(),second.toShortString(),AreaClaimProtocol.volume(first,second)),x,y-40,contentWidth()-16,claim.active?0xFFFFD486:0xFFB8C6CC);
  var r=AreaClaims.result();if(progress&&r!=null)g.textWithWordWrap(font,Component.translatable("stow.area.progress",Component.translatable("stow.area.status."+r.status()),r.total()==0?0:(int)(100L*r.scanned()/r.total()),r.claimed(),r.skipped()),x,y-40,contentWidth()-16,0xFFB8C6CC);
  super.extractRenderState(g,mx,my,delta);
 }
}
