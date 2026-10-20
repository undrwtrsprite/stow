package dev.stow.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.stow.Stow;
import dev.stow.StowConfig;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Bundled release highlights shown once after the installed version changes.
 * Content lives in assets/stow/whatsnew/index.json (newest first) and one &lt;version&gt;.json per release,
 * which only reference lang keys, so every text is translated. A new release just adds an entry.
 */
public final class WhatsNew {
    /** show: optional "Show me" target (stow:find, stow:project, stow:containers, settings:CATEGORY, controls, arrange). */
    public record Section(String title,String text,List<String> how,String show){
        public Section(String title,String text,List<String> how){this(title,text,how,null);}
    }
    public record Entry(String version,List<Section> sections){}
    private static final String ROOT="/assets/stow/whatsnew/";
    private static boolean checked;
    private WhatsNew(){}

    /** Installed version without the +mc build suffix, e.g. 1.0.5. */
    public static String currentVersion(){
        String full=FabricLoader.getInstance().getModContainer(Stow.MOD_ID).map(c->c.getMetadata().getVersion().getFriendlyString()).orElse("");
        int plus=full.indexOf('+');return plus<0?full:full.substring(0,plus);
    }
    /** Runs on the first title screen, or the first in-game frame when a launcher skips the title screen. */
    public static void tick(Minecraft mc){
        if(checked)return;
        Screen screen=mc.gui.screen();
        if(!(screen instanceof TitleScreen)&&!(mc.player!=null&&screen==null))return;
        checked=true;
        String current=currentVersion(),last=Stow.config.lastSeenVersion==null?"":Stow.config.lastSeenVersion;
        if(current.isEmpty()||current.equals(last))return;
        // A brand-new install has nothing "new" to compare against: record the version quietly.
        if(last.isEmpty()&&StowConfig.freshInstall){markSeen();return;}
        if(!Stow.config.whatsNew){markSeen();return;}
        List<Entry> entries=unseen(current,last);
        if(entries.isEmpty()){markSeen();return;}
        mc.gui.setScreen(new WhatsNewScreen(screen,entries,true));
    }
    public static void markSeen(){Stow.config.lastSeenVersion=currentVersion();Stow.config.save();}
    /** Settings button: every bundled entry up to the installed version, newest first. */
    public static Screen screen(Screen parent){
        String current=currentVersion();
        return new WhatsNewScreen(parent,load().stream().filter(e->current.isEmpty()||compare(e.version(),current)<=0).toList(),false);
    }
    /** Opens a "Show me" target; What's new stays the parent so Back returns to it. */
    public static Screen show(Screen parent,String target){
        var empty=net.minecraft.world.item.ItemStack.EMPTY;
        return switch(target){
            case "stow:find"->dev.stow.client.stow.StowScreen.open(parent,empty,dev.stow.client.stow.StowSession.Segment.FIND);
            case "stow:project"->dev.stow.client.stow.StowScreen.open(parent,empty,dev.stow.client.stow.StowSession.Segment.PROJECT);
            case "stow:containers"->dev.stow.client.stow.StowScreen.open(parent,empty,dev.stow.client.stow.StowSession.Segment.CONTAINERS);
            case "controls"->dev.stow.client.stow.SettingsScreen.controls(parent);
            case "arrange"->new dev.stow.client.stow.ArrangeHudScreen(parent,null);
            default->target.startsWith("settings:")?dev.stow.client.stow.SettingsScreen.category(parent,target.substring(9)):dev.stow.client.stow.SettingsScreen.open(parent);
        };
    }
    static List<Entry> unseen(String current,String last){
        return load().stream().filter(e->compare(e.version(),current)<=0&&(last.isEmpty()?e.version().equals(current):compare(e.version(),last)>0)).toList();
    }
    public static List<Entry> load(){
        List<Entry> entries=new ArrayList<>();
        JsonObject index=read("index.json");if(index==null)return entries;
        for(var version:index.getAsJsonArray("versions")){
            JsonObject entry=read(version.getAsString()+".json");if(entry==null)continue;
            List<Section> sections=new ArrayList<>();
            for(var element:entry.getAsJsonArray("sections")){
                var section=element.getAsJsonObject();List<String> how=new ArrayList<>();
                if(section.has("how"))for(var step:section.getAsJsonArray("how"))how.add(step.getAsString());
                sections.add(new Section(section.get("title").getAsString(),section.get("text").getAsString(),List.copyOf(how),section.has("show")?section.get("show").getAsString():null));
            }
            entries.add(new Entry(version.getAsString(),List.copyOf(sections)));
        }
        return entries;
    }
    private static JsonObject read(String name){
        try(InputStream stream=WhatsNew.class.getResourceAsStream(ROOT+name)){
            if(stream==null)return null;
            return JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(Exception error){Stow.createLogger(WhatsNew.class).warn("Could not read release notes "+name,error);return null;}
    }
    private static int compare(String a,String b){
        try{return SemanticVersion.parse(a).compareTo((net.fabricmc.loader.api.Version)SemanticVersion.parse(b));}catch(Exception ignored){return a.compareTo(b);}
    }
}
