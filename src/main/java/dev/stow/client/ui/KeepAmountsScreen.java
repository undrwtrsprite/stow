package dev.stow.client.ui;

import dev.stow.Stow;
import dev.stow.client.inventory.SmartDeposit;
import dev.stow.client.memory.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Item-type keep minimums. Draft values survive pagination and commit together. */
public final class KeepAmountsScreen extends ToolListScreen {
    private final Map<String,String> draft=new LinkedHashMap<>();
    private final List<MemoryItems.Entry> catalogue=MemoryItems.catalogue();
    private List<MemoryItems.Entry> results=List.of();private PlannerButton save;
    public KeepAmountsScreen(Screen parent,String item){super(Component.translatable("stow.keep.title"),parent);Stow.config.keepAmounts.forEach((id,n)->draft.put(id,Integer.toString(n)));initialQuery=item;}
    public static int parse(String value){try{int amount=Integer.parseInt(value);return amount>=0&&amount<=1_000_000?amount:-1;}catch(NumberFormatException e){return -1;}}
    @Override protected void init(){
        begin(40,Component.translatable("stow.need.search"));
        save=addRenderableWidget(new PlannerButton(Component.translatable("stow.common.save"),b->{
            var rules=new LinkedHashMap<String,Integer>();draft.forEach((id,value)->{int n=parse(value);if(n>0)rules.put(id,n);});Stow.config.keepAmounts=rules;Stow.config.save();UiNotifications.show("stow.feedback.keep-saved");onClose();
        },left+80,height-30,68,22,true));
        refresh();setFocused(search);
    }
    private void sync(){save.active=draft.values().stream().allMatch(s->parse(s)>=0);}
    @Override protected void refresh(){
        if(search.getValue().isBlank()){
            var ids=new HashSet<>(draft.keySet());if(minecraft.player!=null)for(var stack:minecraft.player.getInventory().getNonEquipmentItems())if(!stack.isEmpty())ids.add(SmartDeposit.itemId(stack));
            results=catalogue.stream().filter(e->ids.contains(e.id())).toList();
        }else results=MemoryItems.search(catalogue,search.getValue());
        resetRows(results.size());
        for(int i=page;i<Math.min(results.size(),(page+pageSize));i++){
            var item=results.get(i);int y=rowY(i-page);
            var amount=row(new EditBox(font,left+bodyWidth-108,y,64,22,Component.translatable("stow.keep.amount")));amount.setMaxLength(7);amount.setValue(draft.getOrDefault(item.id(),"0"));
            amount.setTextColor(parse(amount.getValue())<0?0xFFFFAA91:0xFFFFFFFF);
            amount.setResponder(value->{draft.put(item.id(),value);amount.setTextColor(parse(value)<0?0xFFFFAA91:0xFFFFFFFF);sync();});
            row(new PlannerButton(Component.literal("×"),b->{draft.remove(item.id());refresh();},left+bodyWidth-38,y,30,22,false));
        }
        if(save!=null)sync();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int x,int y,float delta){
        chrome(g,delta);g.text(font,font.plainSubstrByWidth(Component.translatable("stow.keep.hint").getString(),bodyWidth-16),left+8,72,0xFFAAAAAA);
        beginList(g);
        for(int i=page;i<Math.min(results.size(),(page+pageSize));i++){
            var item=results.get(i);int top=rowY(i-page);rowBackdrop(g,left+4,top-3,bodyWidth-8,rowHeight-4,parse(draft.getOrDefault(item.id(),"0"))>0?0xFF9EE6C4:0xFF808080);g.item(MemoryItems.icon(item.id()),left+8,top+2);
            g.text(font,font.plainSubstrByWidth(item.name(),bodyWidth-150),left+32,top+6,0xFFFFFFFF);
        }
        if(results.isEmpty())g.text(font,Component.translatable("stow.keep.empty"),left+8,92,0xFFAAAAAA);
        endList(g);
        super.extractRenderState(g,x,y,delta);
    }
}
