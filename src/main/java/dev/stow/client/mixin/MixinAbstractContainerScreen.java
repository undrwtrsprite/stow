package dev.stow.client.mixin;
import dev.stow.*;
import dev.stow.client.inventory.*;
import dev.stow.client.memory.*;
import dev.stow.client.input.StowShortcuts;
import dev.stow.client.input.StowShortcuts.Action;
import dev.stow.client.ui.InventoryStrip;
import dev.stow.client.stow.StowScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen extends Screen implements StowDragHandler {
    protected MixinAbstractContainerScreen(Component title){super(title);}
    @Shadow protected abstract Slot getHoveredSlot(double x,double y);
    @Shadow protected abstract void slotClicked(Slot slot,int slotId,int button,ContainerInput input);
    @Shadow @Final protected AbstractContainerMenu menu;
    @Shadow protected Slot hoveredSlot;
    @Shadow private Slot lastClickSlot;
    @Shadow protected boolean isQuickCrafting;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;
    @Unique private final MatchingItemDrag stow_matchingDrag = new MatchingItemDrag();
    @Unique private boolean stow_matchingDragging;
    @Override
    public boolean stow$ownsDrag() {
        return stow_matchingDragging || stow_bundleDragging || stow_carriedBundleButton != -1;
    }
    @Unique private final BundleDrag stow_bundleDrag = new BundleDrag();
    @Unique private boolean stow_bundleDragging;
    @Unique private double stow_bundleDistance;
    @Unique private final CarriedBundleDrag stow_carriedBundleDrag = new CarriedBundleDrag();
    @Unique private int stow_carriedBundleButton=-1;
    @Unique private EditBox stow_search;
    @Unique private String stow_searchQuery = "";
    @Unique private boolean stow_searchVisible;
    @Unique private Button stow_modeButton;
    @Unique private Button stow_memoryButton;
    @Unique private Button stow_needButton;
    @Unique private Button stow_paletteButton;
    @Unique private Button stow_depositButton;
    @Unique private Button stow_glowButton;
    @Unique private SmartDeposit.Plan stow_depositPreview;
    @Unique private SmartDeposit.Plan stow_depositPlan;
    @Unique private long stow_nextButtonUpdate;
    @Unique private int stow_buttonWidth,stow_buttonHeight,stow_effectCount;
    @Unique private int stow_layoutLeft=Integer.MIN_VALUE,stow_layoutTop,stow_layoutWidth,stow_layoutHeight,stow_layoutScreenWidth,stow_layoutScreenHeight;
    @Unique private InventoryExtrasLayout stow_layout;
    @Unique private FloatingMaterialTracker stow_tracker;
    @Unique private boolean stow_controlClick;
    @Unique private Object stow_countStore;
    @Unique private long stow_countRevision = -1;
    @Unique private String stow_countQuery = "";
    @Unique private int stow_memoryCount;
    @Unique private Button stow_takeButton;
    @Unique private SmartDeposit.Plan stow_takePlan;
    @Unique private java.util.Set<String> stow_highlight=java.util.Set.of();
    @Unique private boolean stow_legendVisible;
    @Unique private static int stow_legendContainer=Integer.MIN_VALUE;

    @Inject(method = "init", at = @At("TAIL"))
    private void stow_initSearch(CallbackInfo ci) {
        // Creative already has its own searchable catalogue and special menu slots.
        if ((Object) this instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) return;
        InventoryExtrasLayout layout = stow_extrasLayout();stow_layout=layout;
        // Redesign v2 strip: Sort ▼ · Stow · ? · Deposit · search, plus a Stop glow chip while a glow runs.
        stow_modeButton = addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.SORT,Component.translatable("stow.strip.sort"),button -> {
            if(menu.getCarried().isEmpty())InventoryStrip.sort((AbstractContainerScreen<?>)(Object)this,false);
        },layout.modeX(),layout.modeY()));
        stow_memoryButton = addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.CHESTS,Component.translatable("stow.strip.stow"), button -> {
            if (menu.getCarried().isEmpty()) Minecraft.getInstance().gui.setScreen(StowScreen.openForContainer(this));
        },layout.memoryX(),layout.memoryY()));
        stow_needButton=addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.LEGEND,Component.translatable("stow.strip.legend"),button -> {
            stow_legendVisible=!stow_legendVisible;((InventoryIconButton)stow_needButton).setHudSelected(stow_legendVisible);
        },layout.needX(),layout.needY()));
        stow_needButton.setTooltip(Tooltip.create(Component.translatable("stow.strip.legend.tip")));
        stow_paletteButton=addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.DEPOSIT,Component.translatable("stow.action.deposit"),button->SmartDeposit.start((AbstractContainerScreen<?>)(Object)this),layout.paletteX(),layout.paletteY()));
        stow_depositButton=stow_paletteButton;
        stow_glowButton=addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.STOP_GLOW,Component.translatable("stow.ui.strip.stop-glow"),b->{ChestMemory.stopGlow();stow_nextButtonUpdate=0;stow_updateButtons();},layout.depositX(),layout.depositY()));
        stow_glowButton.setTooltip(Tooltip.create(Component.translatable("stow.strip.stop-glow.tip")));
        stow_tracker=addRenderableWidget(new FloatingMaterialTracker(layout.trackerX(),layout.trackerY(),layout.trackerWidth(),layout.trackerHeight(),itemId -> {
            if(ChestMemory.currentStore()!=null && menu.getCarried().isEmpty())Minecraft.getInstance().gui.setScreen(StowScreen.openGoal(this,itemId));
        }));
        stow_takeButton=addRenderableWidget(new PlannerButton(Component.translatable("stow.take.button",0),b->{if(menu.getCarried().isEmpty())TakeMissing.start((AbstractContainerScreen<?>)(Object)this);stow_nextButtonUpdate=0;},layout.trackerX(),layout.trackerY(),Math.max(20,layout.trackerWidth()),18,true));
        stow_takeButton.visible=false;
        if(Stow.config.keyLegendShown<3&&!((Object)this instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)&&menu.containerId!=stow_legendContainer){
            stow_legendContainer=menu.containerId;Stow.config.keyLegendShown++;Stow.config.save();stow_legendVisible=true;
        }else if(menu.containerId!=stow_legendContainer)stow_legendVisible=false;
        ((InventoryIconButton)stow_needButton).setHudSelected(stow_legendVisible);
        stow_search = new dev.stow.client.ui.StowEditBox(font, layout.searchX(), layout.searchY(), layout.searchWidth(), 20, Component.literal("Search inventory"));
        stow_search.setMaxLength(80);
        stow_search.setHint(Component.translatable("stow.need.search"));
        stow_search.setValue(stow_searchQuery);
        stow_search.setResponder(value -> stow_searchQuery = value);
        stow_search.setVisible(stow_searchVisible);
        addRenderableWidget(stow_search);
        ChestMemory.opened((AbstractContainerScreen<?>) (Object) this);
        stow_nextButtonUpdate=0;
        stow_updateButtons();
    }

    @Unique
    private InventoryExtrasLayout stow_extrasLayout(){
        var mc=Minecraft.getInstance();int effects=(Object)this instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen&&mc.player!=null?mc.player.getActiveEffects().size():0;
        return InventoryExtrasLayout.of(width,height,leftPos,topPos,imageWidth,imageHeight,effects);
    }

    @Unique
    private void stow_updateButtons() {
        if (stow_modeButton == null) return;
        long now=System.nanoTime();
        int effects=(Object)this instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen&&Minecraft.getInstance().player!=null?Minecraft.getInstance().player.getActiveEffects().size():0;
        if(leftPos!=stow_layoutLeft||topPos!=stow_layoutTop||imageWidth!=stow_layoutWidth||imageHeight!=stow_layoutHeight||width!=stow_layoutScreenWidth||height!=stow_layoutScreenHeight||effects!=stow_effectCount){
            stow_layoutLeft=leftPos;stow_layoutTop=topPos;stow_layoutWidth=imageWidth;stow_layoutHeight=imageHeight;stow_layoutScreenWidth=width;stow_layoutScreenHeight=height;stow_effectCount=effects;
            stow_layout=stow_extrasLayout();stow_placeWidgets();stow_tracker.update(ChestMemory.currentStore(),menu.getCarried().isEmpty());
        }
        if(now<stow_nextButtonUpdate&&width==stow_buttonWidth&&height==stow_buttonHeight)return;
        stow_nextButtonUpdate=now+50_000_000L;stow_buttonWidth=width;stow_buttonHeight=height;
        stow_modeButton.setTooltip(Tooltip.create(InventoryStrip.sortTooltip()));
        var memory = ChestMemory.currentStore();
        if (memory != null && stow_searchVisible && !stow_searchQuery.isBlank()) {
            if (memory != stow_countStore || memory.revision() != stow_countRevision
                    || !stow_searchQuery.equals(stow_countQuery)) {
                stow_memoryCount = memory.search(stow_searchQuery,"",null).size();
                stow_countStore = memory;
                stow_countRevision = memory.revision();
                stow_countQuery = stow_searchQuery;
            }
            int count = stow_memoryCount;
            stow_memoryButton.setMessage(Component.translatable("stow.inventory.chests-count",count>99?"99+":Integer.toString(count)));
        } else stow_memoryButton.setMessage(Component.translatable("stow.strip.stow"));
        stow_memoryButton.setTooltip(Tooltip.create(Component.translatable("stow.strip.stow.tip",StowShortcuts.label(StowShortcuts.binding(Action.PALETTE)))));
        stow_memoryButton.active = menu.getCarried().isEmpty();
        boolean busy=InventorySorting.busy(menu)||SmartDeposit.busy(menu);
        stow_modeButton.active=menu.getCarried().isEmpty()&&!busy;
        var mc=Minecraft.getInstance();
        stow_takePlan=memory!=null&&mc.player!=null&&Stow.config.takeMissing&&SmartDeposit.supported(menu)?TakeMissing.plan(menu,mc.player,memory):null;
        stow_highlight=memory!=null&&mc.player!=null?TakeMissing.highlighted(menu,mc.player,memory):java.util.Set.of();
        if(stow_takePlan!=null&&stow_takePlan.amount()>0){stow_takeButton.setMessage(Component.translatable("stow.take.button",stow_takePlan.amount()));stow_takeButton.setTooltip(Tooltip.create(Component.translatable("stow.take.tip",memory.projectName())));}
        stow_takeButton.active=menu.getCarried().isEmpty()&&!busy;
        stow_depositButton.visible=SmartDeposit.supported(menu);
        stow_depositPlan=null;stow_depositButton.active=menu.getCarried().isEmpty()&&!busy;
        if(stow_depositButton.visible){
            var preview=SmartDeposit.preview(menu);stow_depositPlan=preview;
            stow_depositButton.setTooltip(Tooltip.create(preview.amount()>0?Component.translatable("stow.deposit.preview",preview.amount()):Component.translatable(SmartDeposit.emptyReason(menu,Stow.config.depositKeepHotbar,Stow.config.depositMatchingOnly))));
        }
        stow_tracker.update(memory,menu.getCarried().isEmpty());
        stow_placeWidgets();
    }

    @Unique private void stow_placeWidgets(){
        boolean expanded=stow_searchVisible&&stow_layout.modeY()==stow_layout.searchY();
        stow_modeButton.setPosition(stow_layout.modeX(),stow_layout.modeY());
        stow_memoryButton.setPosition(expanded?leftPos+imageWidth-20:stow_layout.memoryX(),stow_layout.memoryY());
        stow_needButton.setPosition(stow_layout.needX(),stow_layout.needY());
        stow_depositButton.setPosition(stow_layout.paletteX(),stow_layout.paletteY());
        boolean deposit=SmartDeposit.supported(menu);
        boolean toolbar=stow_layout.modeY()==stow_layout.glowY();
        // Stop glow takes the next free strip position: after Deposit when it is shown, otherwise Deposit's place.
        stow_glowButton.setPosition(deposit?stow_layout.depositX():stow_layout.paletteX(),deposit?stow_layout.depositY():stow_layout.paletteY());
        if(toolbar&&deposit)stow_glowButton.setPosition(stow_layout.depositX(),stow_layout.depositY());
        stow_search.setY(stow_layout.searchY());
        stow_search.setX(expanded?leftPos:stow_layout.searchX());stow_search.setWidth(expanded?imageWidth-24:stow_layout.searchWidth());
        stow_modeButton.visible=stow_needButton.visible=!expanded;
        stow_memoryButton.visible=true;
        stow_depositButton.visible=deposit&&!expanded;
        stow_glowButton.visible=ChestMemory.glowing()&&!expanded;
        boolean take=stow_takePlan!=null&&stow_takePlan.amount()>0&&stow_layout.trackerWidth()>=38;
        int trackerTop=stow_layout.trackerY()+(take?22:0);
        stow_takeButton.visible=take;
        if(take){stow_takeButton.setPosition(stow_layout.trackerX(),stow_layout.trackerY());stow_takeButton.setWidth(Math.min(stow_layout.trackerWidth(),Math.max(60,font.width(stow_takeButton.getMessage())+14)));}
        stow_tracker.place(stow_layout.trackerX(),trackerTop,stow_layout.trackerWidth(),Math.max(0,stow_layout.trackerHeight()-(take?22:0)));
    }

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void stow_updateInventoryButtons(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        stow_updateButtons();
        stow_depositPreview=stow_depositButton!=null&&stow_depositButton.visible&&stow_depositButton.active&&stow_depositButton.isMouseOver(mouseX,mouseY)?stow_depositPlan:null;
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void stow_drawLegend(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta,CallbackInfo ci){
        if(!stow_legendVisible||stow_modeButton==null)return;
        int w=InventoryStrip.legendWidth(),h=InventoryStrip.legendHeight();
        int x=leftPos-w-6>=4?leftPos-w-6:Math.max(4,leftPos+imageWidth+6);int y=Math.max(4,topPos);
        if(x+w>width-4){x=Math.max(4,leftPos);y=Math.min(height-h-4,topPos+imageHeight+4);}
        InventoryStrip.drawLegend(graphics,x,y);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void stow_saveChest(CallbackInfo ci) {
        MerchantTrading.cancel();
        stow_matchingDragging=false;stow_matchingDrag.clear();
        stow_bundleDragging=false;stow_bundleDrag.clear();
        stow_carriedBundleButton=-1;stow_carriedBundleDrag.clear();
        ChestMemory.screenRemoved(menu);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void stow_searchAndPinKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        MerchantTrading.cancel();
        if((InventorySorting.busy(menu)||SmartDeposit.busy(menu)) && !event.isEscape()){cir.setReturnValue(true);return;}
        if (stow_search != null && StowShortcuts.matches(Action.SEARCH,StowShortcuts.chord(event))) {
            stow_searchVisible = true;
            stow_search.setVisible(true);
            setFocused(stow_search);
            cir.setReturnValue(true);
        } else if (stow_search != null && stow_search.isFocused()) {
            if (event.isEscape()) {
                stow_search.setValue("");
                stow_searchVisible = false;
                stow_search.setVisible(false);
                setFocused(null);
            } else {
                stow_search.keyPressed(event);
            }
            // Inventory, drop and hotbar shortcuts must not run while typing.
            cir.setReturnValue(true);
        } else if(stow_tracker!=null&&!(getFocused() instanceof EditBox)&&stow_tracker.toggleHoveredHud(event)){cir.setReturnValue(true);
        } else if (!(getFocused() instanceof EditBox)&&StowShortcuts.inventoryKey(this,hoveredSlot,event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void stow_markSlots(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (stow_searchVisible && !stow_searchQuery.isBlank() && slot.hasItem()) {
            if (InventorySearch.matches(slot.getItem(), stow_searchQuery)) {
                graphics.outline(slot.x - 1, slot.y - 1, 18, 18, 0xFF65E6AA);
            } else {
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x99000000);
            }
        }
        if(!stow_highlight.isEmpty()&&slot.hasItem()&&!(slot.container instanceof net.minecraft.world.entity.player.Inventory)&&stow_highlight.contains(SmartDeposit.itemId(slot.getItem()))){
            // Two cues, not colour alone: a gold wash and a corner notch.
            graphics.fill(slot.x,slot.y,slot.x+16,slot.y+16,0x40C9A548);graphics.fill(slot.x,slot.y,slot.x+4,slot.y+1,0xFFC9A548);graphics.fill(slot.x,slot.y,slot.x+1,slot.y+4,0xFFC9A548);
        }
        if (PinnedSlots.isPinned(slot)) {
            if(Stow.config.pinStyle==StowConfig.PinStyle.HIGHLIGHT){graphics.fill(slot.x,slot.y,slot.x+16,slot.y+16,0x35FFD45A);graphics.outline(slot.x,slot.y,16,16,0xFFFFD45A);}else SlotMarkers.lock(graphics,slot.x+12,slot.y);
        }
        if(stow_depositPreview!=null&&stow_depositPreview.affects(slot))graphics.outline(slot.x-1,slot.y-1,18,18,0xFF88D6C1);
    }

    @Unique
    private void stow_moveMatchingSlot(Slot slot) {
        // A drag that starts in a merchant input can cross its output slot. Leave all result
        // transfers to vanilla so a Stow multi-slot gesture cannot repeat trades or crafting.
        if (PinnedTransfer.isVanillaResultSlot(menu, slot)) return;
        if (stow_matchingDrag.visit(slot, Minecraft.getInstance().player)) {
            slotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void stow_matchingDrag(MouseButtonEvent event, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if(InventorySorting.busy(menu)||SmartDeposit.busy(menu)){cir.setReturnValue(true);return;}
        if(stow_carriedBundleButton==event.button()){
            if(event.hasShiftDown()&&Stow.config.bundleDrag){
                int steps=Math.max(1,(int)Math.ceil(Math.hypot(dx,dy)/4.0));
                for(int i=1;i<=steps;i++){
                    double progress=(double)i/steps;
                    Slot target=getHoveredSlot(event.x()-dx+dx*progress,event.y()-dy+dy*progress);
                    if(!PinnedTransfer.isVanillaResultSlot(menu,target))stow_carriedBundleDrag.visit(target,Minecraft.getInstance().player,
                            (id,button)->slotClicked(menu.slots.get(id),id,button,ContainerInput.PICKUP));
                }
            }
            cir.setReturnValue(true);return;
        }
        if(stow_bundleDragging&&event.button()==InputConstants.MOUSE_BUTTON_LEFT){
            stow_bundleDistance+=Math.hypot(dx,dy);
            if(event.hasShiftDown()&&Stow.config.bundleDrag){
                int steps=Math.max(1,(int)Math.ceil(Math.hypot(dx,dy)/4.0));
                for(int i=1;i<=steps;i++){
                    double progress=(double)i/steps;
                    Slot target=getHoveredSlot(event.x()-dx+dx*progress,event.y()-dy+dy*progress);
                    if(!PinnedTransfer.isVanillaResultSlot(menu,target))stow_bundleDrag.visit(target,Minecraft.getInstance().player,
                            (id,button)->slotClicked(menu.slots.get(id),id,button,ContainerInput.PICKUP));
                }
            }
            cir.setReturnValue(true);return;
        }
        if (!stow_matchingDragging || event.button() != InputConstants.MOUSE_BUTTON_LEFT) return;
        if (event.hasShiftDown()) {
            // Walk from the old cursor position to the new one; do not sample beyond it.
            int steps = Math.max(1, (int) Math.ceil(Math.hypot(dx, dy) / 4.0));
            for (int i = 1; i <= steps; i++) {
                double progress = (double) i / steps;
                stow_moveMatchingSlot(getHoveredSlot(event.x() - dx + dx * progress, event.y() - dy + dy * progress));
            }
        }
        cir.setReturnValue(true);
    }

    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true)
    private void stow_click(MouseButtonEvent event,boolean doubleClick,CallbackInfoReturnable<Boolean> cir){
        MerchantTrading.cancel();
        if(stow_modeButton==null)return;
        if(InventorySorting.busy(menu)||SmartDeposit.busy(menu)){cir.setReturnValue(true);return;}
        double x=event.x(),y=event.y();
        if(stow_modeButton!=null&&(stow_modeButton.visible&&stow_modeButton.isMouseOver(x,y)||stow_memoryButton.visible&&stow_memoryButton.isMouseOver(x,y)||stow_needButton.visible&&stow_needButton.isMouseOver(x,y)||stow_paletteButton.visible&&stow_paletteButton.isMouseOver(x,y)||stow_depositButton.visible&&stow_depositButton.isMouseOver(x,y)||stow_glowButton.visible&&stow_glowButton.isMouseOver(x,y)||stow_takeButton.visible&&stow_takeButton.isMouseOver(x,y)||stow_tracker.visible&&stow_tracker.isMouseOver(x,y))){
            if(event.button()==InputConstants.MOUSE_BUTTON_RIGHT&&stow_modeButton.visible&&stow_modeButton.isMouseOver(x,y)){Minecraft.getInstance().gui.setScreen(dev.stow.client.stow.SettingsScreen.category(this,"INVENTORY"));stow_controlClick=true;cir.setReturnValue(true);return;}
            super.mouseClicked(event,doubleClick);setFocused(null);stow_controlClick=true;cir.setReturnValue(true);return;
        }
        if(stow_search!=null&&stow_search.isVisible()){
            if(stow_search.isMouseOver(x,y)){super.mouseClicked(event,doubleClick);stow_controlClick=true;cir.setReturnValue(true);return;}
            if(stow_search.isFocused())setFocused(null);
        }
        Slot slot=getHoveredSlot(x,y);
        if(stow_search!=null&&StowShortcuts.matches(Action.SEARCH,StowShortcuts.chord(event.buttonInfo()))){stow_searchVisible=true;stow_search.setVisible(true);setFocused(stow_search);stow_controlClick=true;cir.setReturnValue(true);return;}
        if(StowShortcuts.inventoryMouse(this,slot,event)){stow_controlClick=true;cir.setReturnValue(true);return;}
        if((Object)this instanceof net.minecraft.client.gui.screens.inventory.MerchantScreen merchant
                &&slot==menu.getSlot(2)&&event.button()==InputConstants.MOUSE_BUTTON_LEFT&&event.hasShiftDown()
                &&!event.hasControlDown()&&!event.hasAltDown()&&MerchantTrading.start(merchant)){
            stow_controlClick=true;lastClickSlot=null;isQuickCrafting=false;cir.setReturnValue(true);return;
        }
        if(PinnedTransfer.isVanillaResultSlot(menu,slot))return;
        if(Stow.config.bundleDrag&&slot!=null&&event.hasShiftDown()&&!event.hasControlDown()&&!event.hasAltDown()
                &&(event.button()==InputConstants.MOUSE_BUTTON_LEFT||event.button()==InputConstants.MOUSE_BUTTON_RIGHT)
                &&stow_carriedBundleDrag.begin(menu,Minecraft.getInstance().player,event.button()==InputConstants.MOUSE_BUTTON_RIGHT)){
            stow_carriedBundleButton=event.button();lastClickSlot=null;isQuickCrafting=false;
            stow_carriedBundleDrag.visit(slot,Minecraft.getInstance().player,
                    (id,button)->slotClicked(menu.slots.get(id),id,button,ContainerInput.PICKUP));
            cir.setReturnValue(true);return;
        }
        if(Stow.config.bundleDrag&&event.button()==InputConstants.MOUSE_BUTTON_LEFT&&event.hasShiftDown()&&!event.hasControlDown()
                &&!event.hasAltDown()&&stow_bundleDrag.begin(menu,slot,Minecraft.getInstance().player)){
            stow_bundleDragging=true;stow_bundleDistance=0;lastClickSlot=null;isQuickCrafting=false;cir.setReturnValue(true);return;
        }
        var mode=Stow.config.dragMode;
        if(slot!=null&&event.button()==InputConstants.MOUSE_BUTTON_LEFT&&event.hasShiftDown()&&(!event.hasControlDown()||mode==StowConfig.DragMode.BOTH)&&!event.hasAltDown()&&menu.getCarried().isEmpty()){
            stow_matchingDragging=true;
            boolean matching=mode!=StowConfig.DragMode.ALL_ITEMS&&!(mode==StowConfig.DragMode.BOTH&&event.hasControlDown());
            stow_matchingDrag.begin(slot,matching);lastClickSlot=null;isQuickCrafting=false;stow_moveMatchingSlot(slot);cir.setReturnValue(true);
        }
    }
    @Inject(method="mouseReleased",at=@At("HEAD"),cancellable=true)
    private void stow_release(MouseButtonEvent event,CallbackInfoReturnable<Boolean> cir){
        if(stow_carriedBundleButton==event.button()){
            stow_carriedBundleButton=-1;stow_carriedBundleDrag.clear();lastClickSlot=null;isQuickCrafting=false;cir.setReturnValue(true);return;
        }
        if(stow_bundleDragging&&event.button()==InputConstants.MOUSE_BUTTON_LEFT){
            if(stow_bundleDistance<4&&event.hasShiftDown()&&stow_bundleDrag.valid(Minecraft.getInstance().player)){
                var source=stow_bundleDrag.source();slotClicked(source,source.index,0,ContainerInput.QUICK_MOVE);
            }
            stow_bundleDragging=false;stow_bundleDrag.clear();lastClickSlot=null;isQuickCrafting=false;cir.setReturnValue(true);return;
        }
        if(stow_controlClick){stow_controlClick=false;setDragging(false);cir.setReturnValue(true);return;}
        if(stow_matchingDragging&&event.button()==InputConstants.MOUSE_BUTTON_LEFT){stow_matchingDragging=false;stow_matchingDrag.clear();lastClickSlot=null;isQuickCrafting=false;cir.setReturnValue(true);}
        if(InventorySorting.busy(menu)||SmartDeposit.busy(menu))cir.setReturnValue(true);
    }
    @Inject(method="mouseScrolled",at=@At("HEAD"),cancellable=true)
    private void stow_scroll(double x,double y,double horizontal,double vertical,CallbackInfoReturnable<Boolean> cir){
        MerchantTrading.cancel();
        if(stow_tracker!=null&&stow_tracker.mouseScrolled(x,y,horizontal,vertical)){cir.setReturnValue(true);return;}
        if(stow_modeButton!=null&&stow_modeButton.visible&&stow_modeButton.isMouseOver(x,y)&&vertical!=0){InventoryStrip.cycleOrder(vertical);stow_nextButtonUpdate=0;cir.setReturnValue(true);}
    }
}
