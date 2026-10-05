package dev.stow.client.memory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import static dev.stow.client.memory.ChestMemoryStore.*;

public final class ChestRenameScreen extends MemoryScreenBase {
    private final ChestMemoryStore store;
    private final SavedChest chest;
    private EditBox name;
    public ChestRenameScreen(Screen parent,ChestMemoryStore store,SavedChest chest) {
        super(Component.translatable("stow.memory.rename"),parent,store);this.store=store;this.chest=chest;
    }
    @Override protected void init() {
        layoutDialog(height);
        String value=name==null?store.customName(chest.location()):name.getValue();
        int w=Math.min(480,contentWidth()-16),x=contentLeft()+(contentWidth()-w)/2,y=height/2;
        name=addRenderableWidget(new EditBox(font,x,y-10,w,20,title));name.setMaxLength(48);name.setValue(value);
        name.setHint(Component.literal(chest.title()));setFocused(name);
        addRenderableWidget(PlannerButton.plan(Component.translatable("stow.memory.save-name"),b -> {
            store.rename(chest.location(),name.getValue());ChestMemory.saveLater(store);onClose();
        }).pos(x,y+20).size((w-8)/2,20).build());
        addRenderableWidget(PlannerButton.plan(Component.translatable("stow.memory.reset-name"),b -> {
            store.rename(chest.location(),"");ChestMemory.saveLater(store);onClose();
        }).pos(x+(w+8)/2,y+20).size((w-8)/2,20).build());
        addRenderableWidget(PlannerButton.plan(Component.translatable("gui.back"),b -> onClose()).pos(contentLeft()+8,height-28).size(66,20).build());
    }
    @Override public boolean keyPressed(KeyEvent event){if(name.isFocused() && (event.key()==InputConstants.KEY_RETURN || event.key()==InputConstants.KEY_NUMPADENTER)){store.rename(chest.location(),name.getValue());ChestMemory.saveLater(store);onClose();return true;}return super.keyPressed(event);}
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mx,int my,float delta) {
        extractMenuBackdrop(graphics,delta);
        int bodyWidth=contentWidth(),left=contentLeft();

        graphics.centeredText(font,store.displayName(chest),sidebarWidth()+(width-sidebarWidth())/2,height/2-70,0xFFFFFFFF);
        Component notice=Component.translatable("stow.memory.rename-notice");
        graphics.textWithWordWrap(font,notice,left+8,height/2-54,bodyWidth-16,0xFFACB9BA);
        MemoryItems.drawPreview(graphics,chest.items(),sidebarWidth()+(width-sidebarWidth())/2-55,height/2-36,5,mx,my);
        super.extractRenderState(graphics,mx,my,delta);
    }
}
