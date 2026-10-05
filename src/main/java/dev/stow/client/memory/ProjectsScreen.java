package dev.stow.client.memory;

import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import static dev.stow.client.memory.ChestMemoryStore.*;

/** One named project owns its goals and storage choices; shared chest knowledge stays intact. */
public final class ProjectsScreen extends MemoryScreenBase {
    private final ChestMemoryStore store;
    private EditBox name;
    private PlannerButton create,rename,remove,previous,next,undo;
    private final List<PlannerButton> rows=new ArrayList<>();
    private int left,bodyWidth,page,pageSize;
    private String error="";
    public ProjectsScreen(Screen parent,ChestMemoryStore store){super(Component.translatable("stow.need.projects"),parent,store);this.store=store;}
    @Override protected void init(){
        layoutDialog(480);
        String draft=name==null?store.projectName():name.getValue();
        bodyWidth=contentWidth();left=contentLeft();pageSize=Math.max(1,(dialogHeight-188)/28);
        name=addRenderableWidget(new EditBox(font,left+8,74,bodyWidth-16,22,Component.translatable("stow.need.project-name")));name.setMaxLength(48);name.setValue(draft);
        int buttonWidth=(bodyWidth-24)/3;
        create=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.project-create"),b -> {try{store.createProject(name.getValue());error="";page=0;save();}catch(IllegalArgumentException e){error=e.getMessage();}},left+8,102,buttonWidth,22,true));
        rename=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.project-rename"),b -> {try{store.renameProject(name.getValue());error="";save();}catch(IllegalArgumentException e){error=e.getMessage();}},left+12+buttonWidth,102,buttonWidth,22,false));
        remove=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.project-delete"),b -> {store.deleteProject();name.setValue(store.projectName());save();},left+16+buttonWidth*2,102,buttonWidth,22,false));
        addRenderableWidget(new PlannerButton(Component.translatable("gui.back"),b -> onClose(),left+8,dialogHeight-30,66,22,false));
        undo=addRenderableWidget(new PlannerButton(Component.translatable("stow.need.undo"),b -> {store.undo();name.setValue(store.projectName());save();},left+80,dialogHeight-30,68,22,false));
        previous=addRenderableWidget(new PlannerButton(Component.literal("<"),b -> {page--;refresh();},left+bodyWidth-68,dialogHeight-30,26,22,false));
        next=addRenderableWidget(new PlannerButton(Component.literal(">"),b -> {page++;refresh();},left+bodyWidth-36,dialogHeight-30,26,22,false));
        name.setResponder(v -> sync());refresh();
    }
    private void save(){ChestMemory.saveLater(store);refresh();}
    private void sync(){boolean valid=!name.getValue().isBlank();create.active=valid && store.projects().size()<16 && store.projects().stream().noneMatch(p -> p.name().equalsIgnoreCase(name.getValue().strip()));rename.active=valid && !name.getValue().equals(store.projectName());remove.active=store.projects().size()>1;undo.active=store.canUndo();undo.setTooltip(PlannerTooltips.undo(store));}
    private void refresh(){List<Project> projects=store.projects();page=Math.max(0,Math.min(page,Math.max(0,(projects.size()-1)/pageSize)));rows.forEach(this::removeWidget);rows.clear();
        for(int i=page*pageSize;i<Math.min(projects.size(),(page+1)*pageSize);i++){Project project=projects.get(i);rows.add(addRenderableWidget(new PlannerButton(Component.literal(project.name()),b -> {store.switchProject(project.id());name.setValue(project.name());save();},left+8,146+(i-page*pageSize)*28,bodyWidth-16,22,false).selected(() -> store.activeProjectId().equals(project.id()))));}
        previous.active=page>0;next.active=(page+1)*pageSize<projects.size();sync();
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(vertical!=0 && mx>=left && mx<=left+bodyWidth && my-dialogTop>=146 && my-dialogTop<dialogHeight-38){page+=vertical<0?1:-1;refresh();return true;}return super.mouseScrolled(mx,my,horizontal,vertical);}
    @Override public boolean keyPressed(KeyEvent event){if(name.isFocused() && (event.key()==InputConstants.KEY_RETURN || event.key()==InputConstants.KEY_NUMPADENTER) && create.active){create.onPress(event);return true;}return super.keyPressed(event);}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){extractMenuBackdrop(g,delta);g.pose().pushMatrix();g.pose().translate(0,dialogTop);g.textWithWordWrap(font,Component.translatable("stow.need.projects-hint"),left+8,32,bodyWidth-16,0xFFAAAAAA);g.text(font,Component.translatable("stow.need.project-name"),left+8,58,0xFFAAAAAA);if(!error.isEmpty())g.text(font,font.plainSubstrByWidth(error,bodyWidth-16),left+8,130,0xFFFFAA91);g.pose().popMatrix();super.extractRenderState(g,mx,my,delta);}
}
