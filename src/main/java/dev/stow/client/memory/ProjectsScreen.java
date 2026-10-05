package dev.stow.client.memory;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import static dev.stow.client.memory.ChestMemoryStore.*;

/** Choose a project first; its name and storage controls stay together in one editor. */
public final class ProjectsScreen extends MemoryScreenBase {
    private final ChestMemoryStore store;
    private EditBox name;
    private PlannerButton create,rename,remove,previous,next,undo,storage;
    private final List<PlannerButton> rows=new ArrayList<>();
    private int left,bodyWidth,page,pageSize,listWidth,listTop,editorX,editorY,editorWidth;
    private String error="";
    public ProjectsScreen(Screen parent,ChestMemoryStore store){super(Component.translatable("stow.need.projects"),parent,store);this.store=store;}
    @Override protected void init(){
        layoutDialog(480);String draft=name==null?store.projectName():name.getValue();bodyWidth=contentWidth();left=contentLeft();listTop=workspaceTop();
        boolean split=bodyWidth>=480;listWidth=split?(bodyWidth-24)*2/5:bodyWidth-16;editorWidth=split?bodyWidth-listWidth-32:bodyWidth-16;editorX=split?left+listWidth+24:left+8;editorY=split?listTop+14:height-112;
        pageSize=Math.max(1,((split?height-38:editorY-20)-listTop)/28);
        name=addRenderableWidget(new EditBox(font,editorX,editorY,editorWidth,22,Component.translatable("stow.need.project-name")));name.setMaxLength(48);name.setValue(draft);
        int w=(editorWidth-8)/3;
        create=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.project-create"),b->{try{store.createProject(name.getValue());error="";page=0;save();}catch(IllegalArgumentException e){error=e.getMessage();}},editorX,editorY+28,w,22,true));
        rename=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.project-rename"),b->{try{store.renameProject(name.getValue());error="";save();}catch(IllegalArgumentException e){error=e.getMessage();}},editorX+w+4,editorY+28,w,22,false));
        remove=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.project-delete"),b->{store.deleteProject();name.setValue(store.projectName());error="";save();},editorX+2*(w+4),editorY+28,w,22,false));
        storage=addRenderableWidget(new PlannerButton(Component.empty(),b->minecraft.gui.setScreen(new ChestMemoryScreen(this,store,"",true,null)),editorX,editorY+56,editorWidth,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b->onClose(),left+8,height-30,66,22,false));
        undo=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.undo"),b->{store.undo();name.setValue(store.projectName());error="";save();},left+80,height-30,56,22,false));
        previous=addRenderableWidget(new PlannerButton(Component.literal("<"),b->{page-=pageSize;refresh();},left+bodyWidth-68,height-30,26,22,false));
        next=addRenderableWidget(new PlannerButton(Component.literal(">"),b->{page+=pageSize;refresh();},left+bodyWidth-36,height-30,26,22,false));
        name.setResponder(v->sync());refresh();
    }
    private void save(){ChestMemory.saveLater(store);refresh();}
    private void sync(){storage.setMessage(Component.translatable("stow.project.chests",store.includedChestCount()));boolean valid=!name.getValue().isBlank();create.active=valid&&store.projects().size()<16&&store.projects().stream().noneMatch(p->p.name().equalsIgnoreCase(name.getValue().strip()));rename.active=valid&&!name.getValue().equals(store.projectName());remove.active=store.projects().size()>1;undo.active=store.canUndo();undo.setTooltip(PlannerTooltips.undo(store));}
    private void refresh(){var projects=store.projects();page=Math.clamp(page,0,Math.max(0,projects.size()-pageSize));rows.forEach(this::removeWidget);rows.clear();
        for(int i=page;i<Math.min(projects.size(),(page+pageSize));i++){var project=projects.get(i);rows.add(addRenderableWidget(new PlannerButton(Component.literal(project.name()),b->{store.switchProject(project.id());name.setValue(project.name());error="";save();},left+8,listTop+(i-page)*28,listWidth,22,false).selected(()->store.activeProjectId().equals(project.id()))));}
        trackList(rows,listTop,bodyWidth>=480?height-38:editorY-20,page,28,1);
        previous.active=page>0;next.active=(page+pageSize)<projects.size();sync();
    }
    @Override public boolean mouseScrolled(double mx,double my,double h,double v){if(v!=0&&mx>=left&&mx<left+listWidth+16&&my>=listTop&&my<(bodyWidth>=480?height-38:editorY-20)){page=scrollIndex(v,page,store.projects().size(),pageSize,1);refresh();return true;}return super.mouseScrolled(mx,my,h,v);}
    @Override public boolean keyPressed(KeyEvent event){if(name.isFocused()&&(event.key()==InputConstants.KEY_RETURN||event.key()==InputConstants.KEY_NUMPADENTER)){if(rename.active)rename.onPress(event);return true;}return super.keyPressed(event);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        extractMenuBackdrop(g,delta);g.text(font,font.plainSubstrByWidth(error.isEmpty()?Component.translatable("stow.project.edit",store.projectName()).getString():error,editorWidth),editorX,editorY-14,error.isEmpty()?0xFFAAAAAA:0xFFFFAA91);
        if(bodyWidth>=480){g.verticalLine(editorX-12,listTop,height-42,0xFF555555);g.textWithWordWrap(font,Component.translatable("stow.project.scope-help"),editorX,editorY+90,editorWidth,0xFFAAAAAA);}
        listRangeLabel(g,page,pageSize,store.projects().size());beginList(g);endList(g);super.extractRenderState(g,mx,my,delta);
    }
}
