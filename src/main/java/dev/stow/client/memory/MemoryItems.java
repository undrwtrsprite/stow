package dev.stow.client.memory;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import static dev.stow.client.memory.ChestMemoryStore.*;

/** Icons are reconstructed from item IDs; remembered names/counts remain independent of registries. */
public final class MemoryItems {
    public record Entry(String id,String name) {}
    public static Item item(String id) {
        Identifier key=Identifier.tryParse(id);
        return key==null?Items.AIR:BuiltInRegistries.ITEM.getOptional(key).orElse(Items.AIR);
    }
    public static String name(String id) {
        Item item=item(id);
        return item==Items.AIR?id:Component.translatable(item.getDescriptionId()).getString();
    }
    public static ItemStack icon(String id) { Item item=item(id);return item==Items.AIR || !item.builtInRegistryHolder().areComponentsBound()?ItemStack.EMPTY:new ItemStack(item); }
    public static List<Entry> catalogue() {
        List<Entry> entries=new ArrayList<>();
        for(Item item:BuiltInRegistries.ITEM) if(item!=Items.AIR) {
            String id=BuiltInRegistries.ITEM.getKey(item).toString();
            entries.add(new Entry(id,Component.translatable(item.getDescriptionId()).getString()));
        }
        entries.sort(Comparator.comparing(Entry::name,String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::id));
        return List.copyOf(entries);
    }
    public static String normalizeQuery(String query) {
        return query.strip().equalsIgnoreCase("cobble")?"cobblestone":query.strip();
    }
    public static List<Entry> search(List<Entry> catalogue,String query) {
        String normal=normalizeQuery(query);
        return catalogue.stream().filter(e -> ChestMemoryStore.matches(new MemoryItem(e.id,e.name,1),normal))
            .sorted(Comparator.comparingInt((Entry e) -> e.id.equalsIgnoreCase(normal) || e.id.equalsIgnoreCase("minecraft:"+normal) || e.name.equalsIgnoreCase(normal)?0:1)).toList();
    }
    /** Never resolve a substring to an arbitrary item. Ambiguous names require a visible choice. */
    public static Entry resolve(List<Entry> catalogue,String query) {
        String normal=normalizeQuery(query).toLowerCase(Locale.ROOT);
        List<Entry> exact=catalogue.stream().filter(e -> e.id.equals(normal) || e.id.equals("minecraft:"+normal)).toList();
        if(exact.size()==1)return exact.get(0);
        exact=catalogue.stream().filter(e -> e.name.equalsIgnoreCase(normal)).toList();
        if(exact.size()==1)return exact.get(0);
        List<Entry> matches=search(catalogue,normal);
        return matches.size()==1?matches.get(0):null;
    }
    public static void drawPreview(GuiGraphicsExtractor graphics,List<MemoryItem> items,int x,int y,int slots,int mouseX,int mouseY) {
        drawPreview(graphics,items,x,y,slots,mouseX,mouseY,0);
    }
    public static void drawPreview(GuiGraphicsExtractor graphics,List<MemoryItem> items,int x,int y,int slots,int mouseX,int mouseY,int panelTop) {
        List<MemoryItem> sorted=items.stream().sorted(Comparator.comparingInt(MemoryItem::count).reversed()).toList();
        var font=Minecraft.getInstance().font;
        for(int i=0;i<Math.min(slots,sorted.size());i++) {
            MemoryItem entry=sorted.get(i);int ix=x+i*22;
            ItemStack stack=icon(entry.id());
            graphics.fill(ix-1,y-1,ix+19,y+19,0x88323D42);
            if(!stack.isEmpty()) graphics.item(stack,ix,y);
            else graphics.text(font,"?",ix+4,y+4,0xFFFFFFFF);
            String count=shortCount(entry.count());float scale=Math.min(1,20f/Math.max(1,font.width(count)));
            graphics.pose().pushMatrix();graphics.pose().translate(ix+20-font.width(count)*scale,y+19-9*scale);graphics.pose().scale(scale,scale);graphics.text(font,count,0,0,0xFFFFFFFF);graphics.pose().popMatrix();
            if(mouseX>=ix-1 && mouseX<ix+20 && mouseY-panelTop>=y-1 && mouseY-panelTop<y+20)
                PlannerTooltips.show(graphics,mouseX,mouseY,Component.literal(entry.name()+" × "+entry.count()));
        }
        if(sorted.size()>slots) graphics.text(font,"+"+(sorted.size()-slots),x+slots*22,y+5,0xFFACB9BA);
    }
    public static String shortCount(long count) { return count<1000?Long.toString(count):count<1_000_000?String.format(Locale.ROOT,"%.1fk",count/1000.0):String.format(Locale.ROOT,"%.1fm",count/1_000_000.0); }
}
