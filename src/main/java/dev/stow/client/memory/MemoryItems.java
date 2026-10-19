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
    public static final int PREVIEW_CELL=26;
    public record Entry(String id,String name) {}
    public static ItemStack storageIcon(SavedChest chest){
        return switch(chest.location().kind()){
            case "Shulker box" -> shulkerIcon(chest);
            case "Barrel" -> new ItemStack(Items.BARREL);
            default -> new ItemStack(Items.CHEST);
        };
    }
    public static ItemStack shulkerIcon(SavedChest chest){
        if(!chest.location().kind().equals("Shulker box"))return ItemStack.EMPTY;
        var color=DyeColor.byName(chest.shulkerColor()==null?"":chest.shulkerColor(),null);
        return new ItemStack(color==null?Items.SHULKER_BOX:Items.DYED_SHULKER_BOX.pick(color));
    }
    public static int shulkerAccent(SavedChest chest){
        var color=DyeColor.byName(chest.shulkerColor()==null?"":chest.shulkerColor(),null);
        int rgb=color==null?0xA08DAF:color.getTextureDiffuseColor();
        return 0xFF000000|(((rgb>>16&255)+100)/2+40)<<16|(((rgb>>8&255)+100)/2+40)<<8|((rgb&255)+100)/2+40;
    }
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
    /** English descriptions stay usable when the player's active language is different. */
    public static List<MaterialListImporter.Item> importCatalogue(){
        var english=new HashMap<String,String>();
        try{
            var resource=Minecraft.getInstance().getResourceManager().getResource(Identifier.withDefaultNamespace("lang/en_us.json"));
            if(resource.isPresent())try(var reader=resource.get().openAsReader()){
                var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                for(var entry:json.entrySet())if(entry.getValue().isJsonPrimitive())english.put(entry.getKey(),entry.getValue().getAsString());
            }
        }catch(java.io.IOException|RuntimeException ignored){}
        return catalogue().stream().map(entry->{
            var stack=icon(entry.id());var aliases=new ArrayList<String>();
            aliases.add(entry.id().substring(entry.id().indexOf(':')+1).replace('_',' '));
            aliases.add(english.getOrDefault(item(entry.id()).getDescriptionId(),entry.name()));
            return new MaterialListImporter.Item(entry.id(),entry.name(),stack.isEmpty()?64:stack.getMaxStackSize(),List.copyOf(aliases));
        }).toList();
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
        drawSortedPreview(graphics,sortPreview(items),x,y,slots,mouseX,mouseY,panelTop);
    }
    public record Preview(List<MemoryItem> items,List<ItemStack> icons,List<String> counts) {}
    public static Preview preparePreview(List<MemoryItem> items){var sorted=sortPreview(items);return new Preview(sorted,sorted.stream().map(i->icon(i.id())).toList(),sorted.stream().map(i->shortCount(i.count())).toList());}
    public static void drawPreparedPreview(GuiGraphicsExtractor graphics,Preview preview,int x,int y,int slots,int mouseX,int mouseY,int panelTop){drawPreviewContents(graphics,preview.items(),preview.icons(),preview.counts(),x,y,slots,mouseX,mouseY,panelTop);}
    public static List<MemoryItem> sortPreview(List<MemoryItem> items){return items.stream().sorted(Comparator.comparingInt(MemoryItem::count).reversed()).toList();}
    public static void drawSortedPreview(GuiGraphicsExtractor graphics,List<MemoryItem> sorted,int x,int y,int slots,int mouseX,int mouseY,int panelTop) {
        drawPreviewContents(graphics,sorted,null,null,x,y,slots,mouseX,mouseY,panelTop);
    }
    private static void drawPreviewContents(GuiGraphicsExtractor graphics,List<MemoryItem> sorted,List<ItemStack> icons,List<String> counts,int x,int y,int slots,int mouseX,int mouseY,int panelTop){
        var font=Minecraft.getInstance().font;
        for(int i=0;i<Math.min(slots,sorted.size());i++) {
            MemoryItem entry=sorted.get(i);int ix=x+i*PREVIEW_CELL;
            ItemStack stack=icons==null?icon(entry.id()):icons.get(i);
            graphics.fill(ix-2,y-2,ix+22,y+23,0xD0303030);
            if(!stack.isEmpty()) graphics.item(stack,ix,y);
            else graphics.text(font,"?",ix+4,y+4,0xFFFFFFFF);
            String count=counts==null?shortCount(entry.count()):counts.get(i);float scale=Math.min(1,22f/Math.max(1,font.width(count)));
            graphics.pose().pushMatrix();graphics.pose().translate(ix+21-font.width(count)*scale,y+21-9*scale);graphics.pose().scale(scale,scale);graphics.text(font,count,0,0,0xFFFFFFFF);graphics.pose().popMatrix();
            if(mouseX>=ix-2 && mouseX<ix+22 && mouseY-panelTop>=y-2 && mouseY-panelTop<y+23)
                PlannerTooltips.show(graphics,mouseX,mouseY,Component.literal(entry.name()+" × "+entry.count()));
        }
        if(sorted.size()>slots) graphics.text(font,"+"+(sorted.size()-slots),x+slots*PREVIEW_CELL+2,y+7,0xFFCCCCCC);
    }
    public static String shortCount(long count) { return count<1000?Long.toString(count):count<1_000_000?String.format(Locale.ROOT,"%.1fk",count/1000.0):String.format(Locale.ROOT,"%.1fm",count/1_000_000.0); }
}
