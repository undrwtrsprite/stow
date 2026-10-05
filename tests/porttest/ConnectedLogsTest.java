package porttest;

import dev.stow.stripping.server.ConnectedLogs;
import java.util.*;

/** Runs without Minecraft initialization; tests the production incremental group discovery. */
public final class ConnectedLogsTest {
    private record Pos(int x,int y,int z){}
    private static Iterable<Pos> neighbors(Pos p){
        var result=new ArrayList<Pos>();
        for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)
            if(x!=0||y!=0||z!=0)result.add(new Pos(p.x+x,p.y+y,p.z+z));
        return result;
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static ConnectedLogs<Pos,Integer> scan(Set<Pos> logs,int max,java.util.function.Predicate<Pos> loaded){
        return new ConnectedLogs<>(new Pos(0,0,0),max,ConnectedLogsTest::neighbors,p->logs.contains(p)?1:null,loaded);
    }
    public static void main(String[] args){
        var group=new HashSet<Pos>();
        for(int x=0;x<2000;x++)group.add(new Pos(x,0,0));
        for(int y=0;y<384;y++)group.add(new Pos(0,y,0));
        for(int x=0;x<8;x++)for(int y=0;y<8;y++)for(int z=0;z<8;z++)group.add(new Pos(x,y,z));
        var disconnected=new Pos(-5,0,0);group.add(disconnected);
        var longGroup=scan(group,16384,p->true);int ticks=0;
        while(!longGroup.done()){check(longGroup.advance(17)<=17,"scan budget");ticks++;}
        check(ticks>1,"incremental discovery");check(longGroup.error()==null,"complete discovery");
        check(longGroup.logs().size()==group.size()-1,"all heights, lengths and dense clusters");
        check(!longGroup.logs().containsKey(disconnected),"separate group excluded");
        var diagonal=Set.of(new Pos(0,0,0),new Pos(1,1,1),new Pos(2,2,2));
        var d=scan(diagonal,3,p->true);while(!d.done())d.advance(1);
        check(d.error()==null&&d.logs().size()==3,"diagonal connectivity and exact cap");
        var cap=scan(group,16,p->true);while(!cap.done())cap.advance(7);
        check("limit".equals(cap.error())&&cap.logs().size()==16,"oversize rejection");
        var unloaded=scan(group,16384,p->p.x<100);while(!unloaded.done())unloaded.advance(25);
        check("unloaded".equals(unloaded.error()),"unloaded boundary rejection");
        var missing=scan(Set.of(),16,p->true);check(missing.done()&&"aim".equals(missing.error()),"missing seed");
        var badSeed=scan(group,16,p->false);check("unloaded".equals(badSeed.error()),"unloaded seed");
        System.out.println("PASS: long rows (2000), tall columns (384), dense piles, diagonal connectivity, count cap, unloaded boundary, separate groups and incremental work budgets.");
    }
}
