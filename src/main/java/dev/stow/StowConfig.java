package dev.stow;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Pattern;

/** Only the settings stow actually uses. Old files remain untouched on import. */
public final class StowConfig {
    public enum DragMode { MATCHING_ONLY,ALL_ITEMS,BOTH }
    public enum SortOrder {
        CREATIVE,NAME,AMOUNT,ITEM_TYPE,NONE;
        public dev.stow.client.inventory.sort.SortMode mode(){return switch(this){
            case CREATIVE -> dev.stow.client.inventory.sort.SortMode.CREATIVE;
            case NAME -> dev.stow.client.inventory.sort.SortMode.ALPHABET;
            case AMOUNT -> dev.stow.client.inventory.sort.SortMode.QUANTITY;
            case ITEM_TYPE -> dev.stow.client.inventory.sort.SortMode.RAW_ID;
            case NONE -> dev.stow.client.inventory.sort.SortMode.NONE;
        };}
    }
    public enum HotbarScoping { SOFT,NONE }
    public enum PinStyle { LOCK,HIGHLIGHT }
    public enum DockCorner { BOTTOM_RIGHT,BOTTOM_LEFT,TOP_RIGHT,TOP_LEFT }
    public record Shortcut(boolean mouse,int code,int modifiers) {}
    public java.util.Map<String,Shortcut> shortcuts=new java.util.LinkedHashMap<>();
    public java.util.Map<String,Integer> keepAmounts=new java.util.LinkedHashMap<>();
    public boolean buildingStock=true;
    public int buildingStockScale=135;
    public boolean chestGlow=true;
    public boolean pinProtectTransfers=true;
    public boolean bundleDrag=true;
    public boolean equipmentAll=true;
    public boolean materialsIncludeInventory=true;
    public PinStyle pinStyle=PinStyle.LOCK;
    public boolean rememberMaterialPage=true;
    public boolean handRefill=true;
    public int refillThreshold=8;
    public boolean toolPick=true;
    public boolean autoTool=false;
    public boolean bulkStrip=true;
    public boolean depositKeepHotbar=true;
    public boolean depositMatchingOnly=true;
    public boolean dockEnabled=true;
    public boolean dockMaterials=true;
    public boolean equipmentWatch=true;
    public int equipmentThreshold=15;
    public DockCorner projectCorner=DockCorner.TOP_RIGHT;
    public int projectOffsetX=8,projectOffsetY=8,projectScale=100;
    public DockCorner dockCorner=DockCorner.BOTTOM_RIGHT;
    public int dockOffsetX=8,dockOffsetY=62,dockScale=100,dockRows=3;
    public DragMode dragMode=DragMode.BOTH;
    public SortOrder sortOrder=SortOrder.CREATIVE;
    public SortOrder shiftSortOrder=SortOrder.AMOUNT;
    public SortOrder controlSortOrder=SortOrder.NAME;
    public HotbarScoping hotbarScoping=HotbarScoping.SOFT;
    public boolean optimizeCreativeSearchSort=true;
    public int sortSettingsVersion=2;
    private static final com.google.gson.Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public static void initialize(){
        Path directory=Stow.dataDirectory(), old=directory.getParent();
        try {
            Files.createDirectories(directory);
            if(!Files.exists(directory.resolve("imported-v1"))){
                Path chests=old.resolve("mousewheelie-chest-memory");
                if(Files.isDirectory(chests))try(var files=Files.list(chests)){
                    Files.createDirectories(directory.resolve("chests"));
                    for(Path file:files.filter(p->p.getFileName().toString().endsWith(".json")).toList())copyMissing(file,directory.resolve("chests").resolve(file.getFileName()));
                }
                copyMissing(old.resolve("mousewheelie-pinned-slots.txt"),directory.resolve("pins.txt"));
                if(!Files.exists(directory.resolve("settings.json")) && Files.exists(old.resolve("mousewheelie.hjson"))){
                    String text=Files.readString(old.resolve("mousewheelie.hjson"));
                    var match=Pattern.compile("(?i)(?:shift-drag-mode|shiftDragMode)[\"']?\\s*:\\s*[\"']?(MATCHING_ONLY|ALL_ITEMS|BOTH)").matcher(text);
                    if(match.find())Stow.config.dragMode=DragMode.valueOf(match.group(1).toUpperCase(java.util.Locale.ROOT));
                    Stow.config.save();
                }
                Files.writeString(directory.resolve("imported-v1"),"Imported legacy data; original files preserved.\n");
            }
            Path settings=directory.resolve("settings.json");
            if(Files.exists(settings)){
                StowConfig loaded=JSON.fromJson(Files.readString(settings),StowConfig.class);
                if(loaded!=null){if(loaded.dragMode==null)loaded.dragMode=DragMode.BOTH;if(!Files.readString(settings).contains("sortSettingsVersion"))loaded.sortOrder=SortOrder.CREATIVE;
                    if(loaded.sortOrder==null)loaded.sortOrder=SortOrder.CREATIVE;
                    if(loaded.shiftSortOrder==null)loaded.shiftSortOrder=SortOrder.AMOUNT;
                    if(loaded.controlSortOrder==null)loaded.controlSortOrder=SortOrder.NAME;
                    if(loaded.hotbarScoping==null)loaded.hotbarScoping=HotbarScoping.SOFT;
                    if(loaded.shortcuts==null)loaded.shortcuts=new java.util.LinkedHashMap<>();
                    if(loaded.keepAmounts==null)loaded.keepAmounts=new java.util.LinkedHashMap<>();
                    loaded.keepAmounts.entrySet().removeIf(e->e.getKey()==null||e.getValue()==null||e.getValue()<0||e.getValue()>1_000_000);
                    if(loaded.pinStyle==null)loaded.pinStyle=PinStyle.LOCK;
                    if(loaded.projectCorner==null)loaded.projectCorner=DockCorner.TOP_RIGHT;
                    loaded.projectScale=Math.clamp(loaded.projectScale,60,150);loaded.projectOffsetX=Math.clamp(loaded.projectOffsetX,0,1000);loaded.projectOffsetY=Math.clamp(loaded.projectOffsetY,0,1000);
                    if(loaded.dockCorner==null)loaded.dockCorner=DockCorner.BOTTOM_RIGHT;
                    loaded.refillThreshold=Math.clamp(loaded.refillThreshold,1,16);
                    loaded.equipmentThreshold=Math.clamp(loaded.equipmentThreshold,1,50);
                    loaded.dockScale=Math.clamp(loaded.dockScale,60,150);loaded.dockRows=Math.clamp(loaded.dockRows,3,5);
                    loaded.buildingStockScale=Math.clamp(loaded.buildingStockScale,100,200);
                    loaded.dockOffsetX=Math.clamp(loaded.dockOffsetX,0,1000);loaded.dockOffsetY=Math.clamp(loaded.dockOffsetY,0,1000);
                    Stow.config=loaded;}
            }
        }catch(IOException|RuntimeException error){Stow.createLogger(StowConfig.class).warn("Could not load or import settings",error);}
    }
    private static void copyMissing(Path source,Path destination)throws IOException {
        if(Files.isRegularFile(source)&&!Files.exists(destination)){Files.createDirectories(destination.getParent());Files.copy(source,destination);}
    }
    public void cycleDragMode(){dragMode=DragMode.values()[(dragMode.ordinal()+1)%DragMode.values().length];save();}
    public void save(){
        try {Path destination=Stow.dataDirectory().resolve("settings.json");Files.createDirectories(destination.getParent());Path temporary=destination.resolveSibling("settings.json.tmp");Files.writeString(temporary,JSON.toJson(this)+"\n");Files.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING);}
        catch(IOException error){Stow.createLogger(StowConfig.class).warn("Could not save settings",error);}
    }
}
