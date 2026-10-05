package dev.stow.client.mixin;
import dev.stow.*;
import dev.stow.client.inventory.*;
import dev.stow.client.memory.*;
import dev.stow.client.input.StowShortcuts;
import dev.stow.client.input.StowShortcuts.Action;
import dev.stow.client.ui.CommandPaletteScreen;
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
public abstract class MixinAbstractContainerScreen extends Screen {
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
    @Unique private Button stow_sortButton;
    @Unique private Button stow_paletteButton;
    @Unique private Button stow_depositButton;
    @Unique private Button stow_glowButton;
    @Unique private SmartDeposit.Plan stow_depositPreview;
    @Unique private InventoryExtrasLayout stow_layout;
    @Unique private FloatingMaterialTracker stow_tracker;
    @Unique private boolean stow_controlClick;
    @Unique private Object stow_countStore;
    @Unique private long stow_countRevision = -1;
    @Unique private String stow_countQuery = "";
    @Unique private int stow_memoryCount;

    @Inject(method = "init", at = @At("TAIL"))
    private void stow_initSearch(CallbackInfo ci) {
        // Creative already has its own searchable catalogue and special menu slots.
        if ((Object) this instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen) return;
        InventoryExtrasLayout layout = InventoryExtrasLayout.of(width,height,leftPos,topPos,imageWidth,imageHeight);stow_layout=layout;
        stow_modeButton = addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.DRAG,Component.empty(),button -> {
            Stow.config.cycleDragMode(); stow_updateButtons();
        },layout.modeX(),layout.modeY()));
        stow_modeButton.setTooltip(Tooltip.create(Component.translatable("stow.inventory.drag.description")));
        stow_memoryButton = addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.CHESTS,Component.translatable("stow.inventory.chests"), button -> {
            if (menu.getCarried().isEmpty()) Minecraft.getInstance().gui.setScreen(new ChestMemoryScreen(this,ChestMemory.currentStore(),stow_searchQuery));
        },layout.memoryX(),layout.memoryY()));
        stow_memoryButton.setTooltip(Tooltip.create(Component.translatable("stow.inventory.chests.description")));
        stow_needButton=addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.MATERIALS,Component.translatable("stow.need.title-short"),button -> {
            var memory=ChestMemory.currentStore();
            if(memory!=null && menu.getCarried().isEmpty())Minecraft.getInstance().gui.setScreen(new MaterialsScreen(this,memory));
        },layout.needX(),layout.needY()));
        stow_sortButton=addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.SORT,Component.translatable("stow.sort.title"),button -> {
            if(menu.getCarried().isEmpty())Minecraft.getInstance().gui.setScreen(StowSettingsScreen.create(this));
        },layout.sortX(),layout.sortY()));
        stow_sortButton.setTooltip(Tooltip.create(Component.translatable("stow.sort.title")));
        stow_paletteButton=addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.PALETTE,Component.translatable("stow.palette.title"),button->{
            if(menu.getCarried().isEmpty())Minecraft.getInstance().gui.setScreen(new CommandPaletteScreen(this,hoveredSlot==null?net.minecraft.world.item.ItemStack.EMPTY:hoveredSlot.getItem()));
        },layout.paletteX(),layout.paletteY()));
        stow_paletteButton.setTooltip(Tooltip.create(Component.translatable("stow.palette.title")));
        stow_depositButton=addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.DEPOSIT,Component.translatable("stow.action.deposit"),button->SmartDeposit.start((AbstractContainerScreen<?>)(Object)this),layout.depositX(),layout.depositY()));
        stow_glowButton=addRenderableWidget(new InventoryIconButton(InventoryIconButton.Kind.GLOW,Component.translatable("stow.glow.toggle"),b->{ChestMemory.setGlowEnabled(!Stow.config.chestGlow);stow_updateButtons();},layout.glowX(),layout.glowY()));
        stow_tracker=addRenderableWidget(new FloatingMaterialTracker(layout.trackerX(),layout.trackerY(),layout.trackerWidth(),layout.trackerHeight(),itemId -> {
            var memory=ChestMemory.currentStore();
            if(memory!=null && menu.getCarried().isEmpty())Minecraft.getInstance().gui.setScreen(new MaterialsScreen(this,memory,itemId));
        }));
        stow_search = new EditBox(font, layout.searchX(), layout.searchY(), layout.searchWidth(), 20, Component.literal("Search inventory"));
        stow_search.setMaxLength(80);
        stow_search.setHint(Component.translatable("stow.need.search"));
        stow_search.setValue(stow_searchQuery);
        stow_search.setResponder(value -> stow_searchQuery = value);
        stow_search.setVisible(stow_searchVisible);
        addRenderableWidget(stow_search);
        ChestMemory.opened((AbstractContainerScreen<?>) (Object) this);
        stow_updateButtons();
    }

    @Unique
    private void stow_updateButtons() {
        if (stow_modeButton == null) return;
        stow_layout=InventoryExtrasLayout.of(width,height,leftPos,topPos,imageWidth,imageHeight);
        stow_modeButton.setPosition(stow_layout.modeX(),stow_layout.modeY());stow_memoryButton.setY(stow_layout.memoryY());
        stow_needButton.setPosition(stow_layout.needX(),stow_layout.needY());stow_sortButton.setPosition(stow_layout.sortX(),stow_layout.sortY());stow_paletteButton.setPosition(stow_layout.paletteX(),stow_layout.paletteY());stow_depositButton.setPosition(stow_layout.depositX(),stow_layout.depositY());
        stow_search.setY(stow_layout.searchY());stow_tracker.place(stow_layout.trackerX(),stow_layout.trackerY(),stow_layout.trackerWidth(),stow_layout.trackerHeight());
        boolean toolbar=stow_layout.modeY()==stow_layout.glowY();
        stow_glowButton.setPosition(toolbar?leftPos+imageWidth-20:stow_layout.glowX(),toolbar?stow_layout.glowY():SmartDeposit.supported(menu)?stow_layout.glowY():stow_layout.depositY());
        String mode = Stow.config.dragMode.name().toLowerCase(java.util.Locale.ROOT);
        Component label = Component.translatable("stow.inventory.drag."+mode);
        stow_modeButton.setMessage(Component.translatable("stow.inventory.drag",label));
        stow_modeButton.setTooltip(Tooltip.create(Component.translatable("stow.inventory.drag",label).append(" · ").append(Component.translatable("stow.inventory.drag.hint."+mode))));
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
        } else stow_memoryButton.setMessage(Component.translatable("stow.inventory.chests"));
        stow_memoryButton.setTooltip(Tooltip.create(stow_memoryButton.getMessage()));
        stow_memoryButton.active = menu.getCarried().isEmpty();
        stow_needButton.active=memory!=null && menu.getCarried().isEmpty();
        stow_needButton.setMessage(Component.translatable("stow.need.title-short"));
        stow_needButton.setTooltip(Tooltip.create(Component.translatable("stow.need.quick-hint")));
        boolean busy=InventorySorting.busy(menu)||SmartDeposit.busy(menu);
        stow_sortButton.active=menu.getCarried().isEmpty()&&!busy;
        stow_paletteButton.active=menu.getCarried().isEmpty()&&!busy;
        stow_depositButton.visible=SmartDeposit.supported(menu);
        var preview=SmartDeposit.preview(menu);stow_depositButton.active=menu.getCarried().isEmpty()&&!busy;
        stow_depositButton.setTooltip(Tooltip.create(preview.amount()>0?Component.translatable("stow.deposit.preview",preview.amount()):Component.translatable(SmartDeposit.emptyReason(menu,Stow.config.depositKeepHotbar,Stow.config.depositMatchingOnly))));
        boolean expanded=stow_searchVisible&&stow_layout.modeY()==stow_layout.searchY();
        stow_search.setX(expanded?leftPos:stow_layout.searchX());stow_search.setWidth(expanded?imageWidth-24:stow_layout.searchWidth());
        stow_modeButton.visible=stow_memoryButton.visible=stow_needButton.visible=stow_sortButton.visible=stow_paletteButton.visible=!expanded;
        stow_memoryButton.visible=true;stow_memoryButton.setX(expanded?leftPos+imageWidth-20:stow_layout.memoryX());
        stow_depositButton.visible=SmartDeposit.supported(menu)&&!expanded;
        stow_glowButton.visible=!expanded;stow_glowButton.setTooltip(Tooltip.create(Component.translatable(Stow.config.chestGlow?"stow.glow.on":"stow.glow.off")));
        stow_tracker.update(memory,menu.getCarried().isEmpty());
    }

    @Inject(method = "extractContents", at = @At("HEAD"))
    private void stow_updateInventoryButtons(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta,CallbackInfo ci) {
        stow_updateButtons();
        stow_depositPreview=stow_depositButton!=null&&stow_depositButton.visible&&stow_depositButton.active&&stow_depositButton.isMouseOver(mouseX,mouseY)?SmartDeposit.preview(menu):null;
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void stow_saveChest(CallbackInfo ci) {
        stow_bundleDragging=false;stow_bundleDrag.clear();
        stow_carriedBundleButton=-1;stow_carriedBundleDrag.clear();
        ChestMemory.screenRemoved(menu);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void stow_searchAndPinKeys(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
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
        if (PinnedSlots.isPinned(slot)) {
            if(Stow.config.pinStyle==StowConfig.PinStyle.HIGHLIGHT){graphics.fill(slot.x,slot.y,slot.x+16,slot.y+16,0x35FFD45A);graphics.outline(slot.x,slot.y,16,16,0xFFFFD45A);}else SlotMarkers.lock(graphics,slot.x+12,slot.y);
        }
        if(stow_depositPreview!=null&&stow_depositPreview.affects(slot))graphics.outline(slot.x-1,slot.y-1,18,18,0xFF88D6C1);
    }

    @Unique
    private void stow_moveMatchingSlot(Slot slot) {
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
                    stow_carriedBundleDrag.visit(getHoveredSlot(event.x()-dx+dx*progress,event.y()-dy+dy*progress),Minecraft.getInstance().player,
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
                    stow_bundleDrag.visit(getHoveredSlot(event.x()-dx+dx*progress,event.y()-dy+dy*progress),Minecraft.getInstance().player,
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
        if(stow_modeButton==null)return;
        if(InventorySorting.busy(menu)||SmartDeposit.busy(menu)){cir.setReturnValue(true);return;}
        double x=event.x(),y=event.y();
        if(stow_modeButton!=null&&(stow_modeButton.visible&&stow_modeButton.isMouseOver(x,y)||stow_memoryButton.visible&&stow_memoryButton.isMouseOver(x,y)||stow_needButton.visible&&stow_needButton.isMouseOver(x,y)||stow_sortButton.visible&&stow_sortButton.isMouseOver(x,y)||stow_paletteButton.visible&&stow_paletteButton.isMouseOver(x,y)||stow_depositButton.visible&&stow_depositButton.isMouseOver(x,y)||stow_glowButton.visible&&stow_glowButton.isMouseOver(x,y)||stow_tracker.visible&&stow_tracker.isMouseOver(x,y))){
            super.mouseClicked(event,doubleClick);setFocused(null);stow_controlClick=true;cir.setReturnValue(true);return;
        }
        if(stow_search!=null&&stow_search.isVisible()){
            if(stow_search.isMouseOver(x,y)){super.mouseClicked(event,doubleClick);stow_controlClick=true;cir.setReturnValue(true);return;}
            if(stow_search.isFocused())setFocused(null);
        }
        Slot slot=getHoveredSlot(x,y);
        if(stow_search!=null&&StowShortcuts.matches(Action.SEARCH,StowShortcuts.chord(event.buttonInfo()))){stow_searchVisible=true;stow_search.setVisible(true);setFocused(stow_search);stow_controlClick=true;cir.setReturnValue(true);return;}
        if(StowShortcuts.inventoryMouse(this,slot,event)){stow_controlClick=true;cir.setReturnValue(true);return;}
        if(PinnedTransfer.isCraftingResult(menu,slot))return;
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
        if(event.button()==InputConstants.MOUSE_BUTTON_LEFT&&event.hasShiftDown()&&(!event.hasControlDown()||mode==StowConfig.DragMode.BOTH)&&!event.hasAltDown()&&menu.getCarried().isEmpty()){
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
        if(stow_tracker!=null&&stow_tracker.mouseScrolled(x,y,horizontal,vertical))cir.setReturnValue(true);
    }
}
