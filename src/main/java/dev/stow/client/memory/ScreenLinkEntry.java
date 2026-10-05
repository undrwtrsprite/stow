package dev.stow.client.memory;

import java.util.*;
import java.util.function.Function;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Navigation belongs inside settings; the native button remains keyboard accessible. */
final class ScreenLinkEntry extends AbstractConfigListEntry<Boolean> {
    private final PlannerButton button;
    ScreenLinkEntry(Component label,Function<Screen,Screen> open){super(label,false);button=new PlannerButton(label,b->Minecraft.getInstance().gui.setScreen(open.apply(getConfigScreen())),0,0,100,22,false);}
    @Override public Boolean getValue(){return false;}
    @Override public Optional<Boolean> getDefaultValue(){return Optional.empty();}
    @Override public boolean isEdited(){return false;}
    @Override public int getItemHeight(){return 26;}
    @Override public List<? extends GuiEventListener> children(){return List.of(button);}
    @Override public List<? extends NarratableEntry> narratables(){return List.of(button);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int index,int y,int x,int rowWidth,int rowHeight,int mx,int my,boolean hovered,float delta){button.setX(x);button.setY(y+2);button.setWidth(rowWidth);button.extractRenderState(g,mx,my,delta);}
}
