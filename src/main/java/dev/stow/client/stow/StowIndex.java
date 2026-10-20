package dev.stow.client.stow;

import dev.stow.client.memory.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import static dev.stow.client.memory.ChestMemoryStore.*;

/**
 * Read-only, item-first view over every remembered container (all scopes), rebuilt once per store
 * revision. Distances are computed on demand from the player's current position.
 */
public final class StowIndex {
    public static final long STALE_MS=5L*24*60*60*1000;
    public record Source(SavedChest chest,long count) {}
    public record ItemStat(String id,long stored,List<Source> sources,long newest,long oldest,Set<String> names) {}
    private static StowIndex cached;
    private final ChestMemoryStore store;private final long revision;
    private final Map<String,ItemStat> items;
    private StowIndex(ChestMemoryStore store){
        this.store=store;revision=store.revision();
        var totals=new HashMap<String,Long>();var sources=new HashMap<String,List<Source>>();var newest=new HashMap<String,Long>();var oldest=new HashMap<String,Long>();var names=new HashMap<String,Set<String>>();
        for(var chest:store.chests()){
            var perChest=new HashMap<String,Long>();
            for(var item:chest.items()){perChest.merge(item.id(),(long)item.count(),Long::sum);names.computeIfAbsent(item.id(),k->new HashSet<>()).add(item.name());}
            for(var entry:perChest.entrySet()){
                totals.merge(entry.getKey(),entry.getValue(),Long::sum);
                sources.computeIfAbsent(entry.getKey(),k->new ArrayList<>()).add(new Source(chest,entry.getValue()));
                newest.merge(entry.getKey(),chest.lastSeen(),Math::max);oldest.merge(entry.getKey(),chest.lastSeen(),Math::min);
            }
        }
        var built=new HashMap<String,ItemStat>();
        for(var entry:totals.entrySet())built.put(entry.getKey(),new ItemStat(entry.getKey(),entry.getValue(),List.copyOf(sources.get(entry.getKey())),newest.get(entry.getKey()),oldest.get(entry.getKey()),Set.copyOf(names.get(entry.getKey()))));
        items=Map.copyOf(built);
    }
    public static StowIndex of(ChestMemoryStore store){
        if(store==null)return null;
        var current=cached;if(current==null||current.store!=store||current.revision!=store.revision())cached=current=new StowIndex(store);
        return current;
    }
    public Collection<ItemStat> items(){return items.values();}
    public ItemStat item(String id){return items.get(id);}
    public static String dimension(){var mc=Minecraft.getInstance();return mc.level==null?"":mc.level.dimension().identifier().toString();}
    public static BlockPos player(){var mc=Minecraft.getInstance();return mc.player==null?null:mc.player.blockPosition();}
    /** Metres to a container, or -1 when it is in another dimension or the player is unknown. */
    public static double distance(SavedChest chest){
        var pos=player();if(pos==null||!chest.location().dimension().equals(dimension()))return -1;
        return Math.sqrt(pos.distSqr(chest.location().pos()));
    }
    /** Same dimension first, then nearest, then most recently seen. */
    public static Comparator<SavedChest> nearestFirst(){
        String dim=dimension();BlockPos pos=player();
        return Comparator.<SavedChest>comparingInt(c->c.location().dimension().equals(dim)?0:1)
            .thenComparingDouble(c->pos==null||!c.location().dimension().equals(dim)?Double.MAX_VALUE:pos.distSqr(c.location().pos()))
            .thenComparing(Comparator.comparingLong(SavedChest::lastSeen).reversed());
    }
    public List<Source> sourcesNearestFirst(String id){
        var stat=items.get(id);if(stat==null)return List.of();
        var order=nearestFirst();return stat.sources().stream().sorted((a,b)->order.compare(a.chest(),b.chest())).toList();
    }
    /** Nearest source in the current dimension, ignoring the project's container scope. */
    public SavedChest nearest(String id){
        var sorted=sourcesNearestFirst(id);if(sorted.isEmpty())return null;
        var first=sorted.getFirst().chest();return first.location().dimension().equals(dimension())?first:null;
    }
    public static boolean stale(SavedChest chest){return System.currentTimeMillis()-chest.lastSeen()>=STALE_MS;}
    public static long carried(String id){
        var mc=Minecraft.getInstance();if(mc.player==null)return 0;return MaterialPlanner.inventoryCount(mc.player.getInventory(),mc.player.containerMenu.getCarried(),id);
    }
}
