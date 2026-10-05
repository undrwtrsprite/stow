package dev.stow.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.stow.Stow;
import dev.stow.StowConfig.Shortcut;
import dev.stow.client.input.StowShortcuts;
import dev.stow.client.input.StowShortcuts.Action;
import dev.stow.client.memory.PlannerButton;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;

public final class ShortcutEditorScreen extends ToolListScreen {
    private final Map<String,Shortcut> draft=new LinkedHashMap<>();
    private List<Action> results=List.of();private Action waiting;
    public ShortcutEditorScreen(Screen parent){super(Component.translatable("stow.shortcuts.title"),parent);for(Action action:Action.values())draft.put(action.name(),StowShortcuts.binding(action));}
    @Override protected void init(){
        begin(48,Component.translatable("stow.shortcuts.search"));
        addRenderableWidget(new PlannerButton(Component.translatable("stow.common.save"),b->{Stow.config.shortcuts=new LinkedHashMap<>(draft);Stow.config.save();onClose();},left+80,height-30,68,22,true));
        refresh();setFocused(search);
    }
    @Override protected void refresh(){
        String query=search.getValue().strip().toLowerCase(Locale.ROOT);
        results=Arrays.stream(Action.values()).filter(a->(a.title().getString()+" "+a.group().getString()).toLowerCase(Locale.ROOT).contains(query)).toList();resetRows(results.size());
        for(int i=page;i<Math.min(results.size(),(page+pageSize));i++){
            Action action=results.get(i);int y=rowY(i-page);
            row(new PlannerButton(Component.literal(waiting==action?Component.translatable("stow.shortcuts.press").getString():StowShortcuts.label(draft.get(action.name()))),b->{waiting=action;setFocused(null);refresh();},left+bodyWidth-176,y,100,22,false));
            row(new PlannerButton(Component.translatable("controls.reset"),b->{draft.put(action.name(),action.defaults());waiting=null;refresh();},left+bodyWidth-70,y,62,22,false));
        }
    }
    private void capture(Shortcut shortcut){draft.put(waiting.name(),shortcut);waiting=null;refresh();}
    @Override public boolean keyPressed(KeyEvent event){
        if(waiting!=null){
            if(event.isEscape()){waiting=null;refresh();return true;}
            if(event.key()==InputConstants.KEY_DELETE||event.key()==InputConstants.KEY_BACKSPACE){capture(new Shortcut(false,-1,0));return true;}
            if(Set.of(InputConstants.KEY_LSHIFT,InputConstants.KEY_RSHIFT,InputConstants.KEY_LCONTROL,InputConstants.KEY_RCONTROL,InputConstants.KEY_LALT,InputConstants.KEY_RALT,InputConstants.KEY_LGUI,InputConstants.KEY_RGUI).contains(event.key()))return true;
            capture(StowShortcuts.chord(event));return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){if(waiting!=null){capture(StowShortcuts.chord(event.buttonInfo()));return true;}return super.mouseClicked(event,twice);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        chrome(g,delta);g.text(font,font.plainSubstrByWidth(Component.translatable(waiting==null?"stow.shortcuts.hint":"stow.shortcuts.capture-hint").getString(),bodyWidth-16),left+8,72,0xFFAAAAAA);
        beginList(g);
        for(int i=page;i<Math.min(results.size(),(page+pageSize));i++){
            Action action=results.get(i);int top=rowY(i-page);
            rowBackdrop(g,left+4,top-3,bodyWidth-8,rowHeight-4,waiting==action?0xFFFFD486:0xFF808080);
            g.text(font,font.plainSubstrByWidth(action.title().getString(),bodyWidth-194),left+8,top+6,0xFFFFFFFF);
            List<String> conflicts=StowShortcuts.conflicts(action,draft.get(action.name()),draft);
            String note=conflicts.isEmpty()?action.group().getString():Component.translatable("stow.shortcuts.conflict",conflicts.getFirst()).getString();
            g.text(font,font.plainSubstrByWidth(note,bodyWidth-16),left+8,top+27,conflicts.isEmpty()?0xFFAAAAAA:0xFFFFC388);
        }
        endList(g);
        super.extractRenderState(g,x,y,delta);
    }
}
