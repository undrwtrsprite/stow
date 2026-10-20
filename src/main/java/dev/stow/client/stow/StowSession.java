package dev.stow.client.stow;

import java.util.*;

/** In-memory Stow screen state per world (query, selection, scroll, chips); never written to disk. */
public final class StowSession {
    public enum Segment { FIND,PROJECT,CONTAINERS }
    public enum ProjectFilter { MISSING,ALL,DONE }
    public enum ContainerFilter { ALL,IN_PROJECT,NAMED,STALE }
    public enum ContainerSort { NEAREST,NAME,LAST_SEEN }
    private static String world;
    private static StowSession current;
    public Segment segment=Segment.FIND;
    public final EnumMap<Segment,String> query=new EnumMap<>(Segment.class);
    public final EnumMap<Segment,String> selection=new EnumMap<>(Segment.class);
    public final EnumMap<Segment,Integer> scroll=new EnumMap<>(Segment.class);
    public ProjectFilter projectFilter=ProjectFilter.MISSING;
    public ContainerFilter containerFilter=ContainerFilter.ALL;
    public ContainerSort containerSort=ContainerSort.NEAREST;
    public String ownerFilter="";
    public final Deque<String> history=new ArrayDeque<>();
    public final Deque<String> recentItems=new ArrayDeque<>();
    public final Deque<String> recentContainers=new ArrayDeque<>();
    private StowSession(){for(var s:Segment.values()){query.put(s,"");scroll.put(s,0);}}
    public static StowSession get(String worldKey){
        if(current==null||!Objects.equals(world,worldKey)){current=new StowSession();world=worldKey;}
        return current;
    }
    public static void clear(){current=null;world=null;}
    public void remember(String text){text=text.strip();if(text.isEmpty())return;history.remove(text);history.addFirst(text);while(history.size()>20)history.removeLast();}
    public void recentItem(String id){recentItems.remove(id);recentItems.addFirst(id);while(recentItems.size()>8)recentItems.removeLast();}
    public void recentContainer(String key){recentContainers.remove(key);recentContainers.addFirst(key);while(recentContainers.size()>8)recentContainers.removeLast();}
}
