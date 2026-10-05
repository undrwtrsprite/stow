package dev.stow.stripping.server;

import java.util.*;
import java.util.function.*;

/** Incremental flood fill. Distance from the seed never limits membership. */
public final class ConnectedLogs<P,S> {
    private final int limit;
    private final Function<P,Iterable<P>> neighbors;
    private final Function<P,S> readLog;
    private final Predicate<P> loaded;
    private final ArrayDeque<P> frontier=new ArrayDeque<>();
    private final Map<P,S> logs=new LinkedHashMap<>();
    private String error;
    public ConnectedLogs(P seed,int limit,Function<P,Iterable<P>> neighbors,Function<P,S> readLog,Predicate<P> loaded){
        this.limit=limit;this.neighbors=neighbors;this.readLog=readLog;this.loaded=loaded;
        if(!loaded.test(seed)){error="unloaded";return;}
        S state=readLog.apply(seed);
        if(state==null){error="aim";return;}
        logs.put(seed,state);frontier.add(seed);
    }
    public int advance(int budget){
        int used=0;
        while(error==null&&!frontier.isEmpty()&&used<budget){
            P pos=frontier.removeFirst();used++;
            for(P next:neighbors.apply(pos)){
                if(logs.containsKey(next))continue;
                // Never generate or load terrain merely to discover a batch.
                if(!loaded.test(next)){error="unloaded";break;}
                S state=readLog.apply(next);
                if(state==null)continue;
                if(logs.size()>=limit){error="limit";break;}
                logs.put(next,state);frontier.addLast(next);
            }
        }
        return used;
    }
    public boolean done(){return error!=null||frontier.isEmpty();}
    public String error(){return error;}
    public Map<P,S> logs(){return Collections.unmodifiableMap(logs);}
}
