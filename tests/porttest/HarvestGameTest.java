package porttest;

import dev.stow.stripping.protocol.OakHarvestProtocol.Request;
import dev.stow.stripping.protocol.OakHarvestProtocol.Result;
import dev.stow.stripping.server.OakHarvestServer;
import java.lang.reflect.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;

/** Headless Fabric server test: real world, loot, axe actions and break/use callbacks. */
public final class HarvestGameTest {
    private static boolean denyUse,denyBreak,cascade;
    private static List<BlockPos> cascadeTargets=List.of();
    private static int breaks;
    private static boolean registered;
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void callbacks(){
        if(registered)return;registered=true;
        UseBlockCallback.EVENT.register((p,l,h,hit)->denyUse?InteractionResult.FAIL:InteractionResult.PASS);
        PlayerBlockBreakEvents.BEFORE.register((l,p,pos,state,entity)->!denyBreak);
        PlayerBlockBreakEvents.AFTER.register((l,p,pos,state,entity)->{
            breaks++;
            if(cascade){
                cascade=false;
                for(var next:cascadeTargets){
                    require(l.getBlockState(next).is(Blocks.STRIPPED_OAK_LOG),"whole group stripped before Timber cascade");
                    ((ServerPlayer)p).gameMode.destroyBlock(next);
                }
            }
        });
    }
    private static final class Fixture {
        final GameTestHelper h;
        final ServerPlayer p;
        final BlockPos origin;
        final List<BlockPos> logs=new ArrayList<>();
        final OakHarvestServer server=new OakHarvestServer();
        Fixture(GameTestHelper h,ServerPlayer p,BlockPos origin){
            this.h=h;this.p=p;this.origin=origin;
            p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);
            p.setPos(origin.getX()-1.5,origin.getY(),origin.getZ()+0.5);
            p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.NETHERITE_AXE));
            denyUse=false;denyBreak=false;cascade=false;breaks=0;
        }
        BlockPos log(int x,int y,int z){
            var pos=origin.offset(x,y,z);h.getLevel().getChunk(pos);
            h.getLevel().setBlock(pos,Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.X),3);
            logs.add(pos);return pos;
        }
        void start()throws Exception{
            var method=OakHarvestServer.class.getDeclaredMethod("request",ServerPlayer.class,Request.class);method.setAccessible(true);
            method.invoke(server,p,new Request(1,origin,p.getInventory().getSelectedSlot(),false));
        }
        Map<?,?> jobs()throws Exception{
            var field=OakHarvestServer.class.getDeclaredField("jobs");field.setAccessible(true);return (Map<?,?>)field.get(server);
        }
        void tick(int count)throws Exception{
            var method=OakHarvestServer.class.getDeclaredMethod("tick",MinecraftServer.class);method.setAccessible(true);
            for(int i=0;i<count;i++)method.invoke(server,h.getLevel().getServer());
        }
        void complete()throws Exception{for(int i=0;i<150&&!jobs().isEmpty();i++)tick(1);require(jobs().isEmpty(),"job completed");}
        int drops(){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(origin).inflate(220)).stream()
            .filter(e->e.getItem().is(Items.STRIPPED_OAK_LOG)).mapToInt(e->e.getItem().getCount()).sum();}
        void clear(){for(var pos:logs)h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            for(var e:h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(origin).inflate(220)))e.discard();}
    }
    @GameTest(maxTicks=200)
    public void harvest(GameTestHelper h)throws Exception{
        callbacks();var p=h.makeMockServerPlayerInLevel();p.getInventory().setSelectedSlot(0);
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
        var payload=new Request(37,new BlockPos(-15,318,120),8,true);
        Request.CODEC.encode(buffer,payload);require(Request.CODEC.decode(buffer).equals(payload),"request wire round trip");
        buffer.clear();var reply=new Result(37,"done",192,192);
        Result.CODEC.encode(buffer,reply);require(Result.CODEC.decode(buffer).equals(reply),"result wire round trip");buffer.release();
        var origin=h.absolutePos(new BlockPos(3,3,3));
        p.setNoGravity(true);p.setPos(origin.getX()-1.5,origin.getY(),origin.getZ()+0.5);
        // Remote full chunks may not yet expose their item entities to queries. Give test terrain time to track entities.
        for(int x=(origin.getX()>>4)-1;x<=((origin.getX()+101)>>4)+1;x++)
            for(int z=(origin.getZ()>>4)-1;z<=(origin.getZ()>>4)+1;z++)h.getLevel().setChunkForced(x,z,true);
        h.runAfterDelay(20,()->{
            try{runCases(h,p,origin);}catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private static void runCases(GameTestHelper h,ServerPlayer p,BlockPos origin)throws Exception{
        var f=new Fixture(h,p,origin);
        for(int x=0;x<100;x++)f.log(x,0,0);
        for(int y=1;y<96;y++)f.log(0,y,0);
        for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=1;z<4;z++)f.log(x,y,z);
        int count=f.logs.size();var separate=origin.offset(-4,0,5);h.getLevel().setBlock(separate,Blocks.OAK_LOG.defaultBlockState(),3);
        var birch=origin.offset(1,0,-1);h.getLevel().setBlock(birch,Blocks.BIRCH_LOG.defaultBlockState(),3);
        f.start();require(!f.jobs().isEmpty(),"reachable seed accepted");f.complete();
        for(var pos:f.logs)require(h.getLevel().getBlockState(pos).isAir(),"harvest entire tall/long/dense group");
        require(f.drops()==count,"exact stripped-log drops: expected "+count+", got "+f.drops()+", breaks "+breaks);require(breaks==count,"one vanilla break per log");
        require(p.getMainHandItem().getDamageValue()==2*count,"normal strip plus mine durability");
        require(h.getLevel().getBlockState(separate).is(Blocks.OAK_LOG),"separate oak left alone");
        require(h.getLevel().getBlockState(birch).is(Blocks.BIRCH_LOG),"other wood left alone");
        f.clear();h.getLevel().setBlock(separate,Blocks.AIR.defaultBlockState(),3);h.getLevel().setBlock(birch,Blocks.AIR.defaultBlockState(),3);
        System.out.println("PASS harvest: tall columns, long rows, dense piles, stripped drops, durability, isolated oak and other wood");

        f=new Fixture(h,p,origin);for(int x=0;x<6;x++)f.log(x,0,0);
        cascadeTargets=List.copyOf(f.logs.subList(1,f.logs.size()));cascade=true;
        f.start();f.complete();require(f.drops()==6&&breaks==6,"Timber cascade no duplicate drops");f.clear();
        System.out.println("PASS harvest: all logs stripped before Timber cascade; no duplicate drops");

        f=new Fixture(h,p,origin);f.log(0,0,0);denyUse=true;f.start();f.complete();
        require(h.getLevel().getBlockState(origin).is(Blocks.OAK_LOG)&&breaks==0,"denied use does not harvest");f.clear();
        f=new Fixture(h,p,origin);f.log(0,0,0);denyBreak=true;f.start();f.complete();
        require(h.getLevel().getBlockState(origin).is(Blocks.STRIPPED_OAK_LOG)&&f.drops()==0,"denied break produces no drops");f.clear();
        System.out.println("PASS harvest: Fabric use and break vetoes respected");

        f=new Fixture(h,p,origin);f.log(0,0,0);f.log(1,0,0);
        p.getMainHandItem().setDamageValue(p.getMainHandItem().getMaxDamage()-4);f.start();f.complete();
        require(h.getLevel().getBlockState(origin).is(Blocks.OAK_LOG)&&breaks==0,"durability preflight leaves group unchanged");f.clear();
        f=new Fixture(h,p,origin);f.log(0,0,0);f.start();
        var cancel=OakHarvestServer.class.getDeclaredMethod("request",ServerPlayer.class,Request.class);cancel.setAccessible(true);
        cancel.invoke(f.server,p,new Request(1,origin,0,true));f.tick(2);
        require(h.getLevel().getBlockState(origin).is(Blocks.OAK_LOG)&&f.jobs().isEmpty(),"second shortcut cancellation");f.clear();
        f=new Fixture(h,p,origin);f.log(0,0,0);f.start();p.getInventory().setSelectedSlot(1);f.complete();
        require(h.getLevel().getBlockState(origin).is(Blocks.OAK_LOG),"tool change cancellation");f.clear();p.getInventory().setSelectedSlot(0);
        f=new Fixture(h,p,origin);f.log(0,0,0);p.setPos(origin.getX()-15,origin.getY(),origin.getZ());f.start();
        require(f.jobs().isEmpty(),"unreachable seed rejected");f.clear();
        System.out.println("PASS harvest: insufficient durability, cancellation, tool change and unreachable seed");
        f=new Fixture(h,p,origin);for(int x=0;x<6;x++)f.log(x,0,0);
        var cap=OakHarvestServer.class.getDeclaredField("maxLogs");cap.setAccessible(true);cap.setInt(f.server,3);
        f.start();f.complete();for(var pos:f.logs)require(h.getLevel().getBlockState(pos).is(Blocks.OAK_LOG),"count cap preflight changes nothing");f.clear();
        f=new Fixture(h,p,origin);f.log(0,0,0);f.start();
        h.getLevel().setBlock(origin,Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,Direction.Axis.Z),3);
        f.complete();require(h.getLevel().getBlockState(origin).is(Blocks.OAK_LOG)&&breaks==0,"changed snapshot rejected");f.clear();
        f=new Fixture(h,p,origin);f.log(0,0,0);p.setGameMode(GameType.CREATIVE);f.start();f.complete();
        require(h.getLevel().getBlockState(origin).isAir()&&f.drops()==0&&p.getMainHandItem().getDamageValue()==0,"vanilla creative behavior");f.clear();
        System.out.println("PASS harvest: packet codecs, server count cap, changed snapshot and creative behavior");
        denyBreak=false;denyUse=false;cascade=false;h.succeed();
    }
}
