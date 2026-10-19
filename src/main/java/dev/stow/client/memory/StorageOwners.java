package dev.stow.client.memory;
import dev.stow.companion.protocol.StorageOwnerProtocol;
import dev.stow.companion.protocol.StorageOwnerProtocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.*;
import static dev.stow.client.memory.ChestMemoryStore.*;
public final class StorageOwners {
 private record Pending(ChestMemoryStore store,Location location,int action,long time) {}
 private static final Map<String,Pending> pending=new HashMap<>();
 private static final Map<String,Result> replies=new HashMap<>();
 public static void register(){StorageOwnerProtocol.register();ClientPlayNetworking.registerGlobalReceiver(Result.TYPE,(r,c)->c.client().execute(()->{
  String key=new Location(r.dimension(),r.pos().getX(),r.pos().getY(),r.pos().getZ(),"").key();var p=pending.remove(key);
  if(p==null||p.store!=ChestMemory.currentStore()||System.nanoTime()-p.time>5_000_000_000L)return;
  if(replies.size()>=256)replies.clear();replies.put(key,r);
  if(r.status()!=StorageOwnerProtocol.UNAVAILABLE){p.store.owner(p.location,r.owner());ChestMemory.saveLater(p.store);}
  if(p.action!=StorageOwnerProtocol.QUERY)dev.stow.client.ui.UiNotifications.show(r.status()==StorageOwnerProtocol.OK?"stow.owner.updated":"stow.owner.denied");
 }));}
 public static Owner known(Location l){var r=replies.get(l.key());return r==null||r.status()==StorageOwnerProtocol.UNAVAILABLE?null:r.owner();}
 public static void areaClaimed(ChestMemoryStore store,String dimension,List<net.minecraft.core.BlockPos> positions,Owner owner){
  if(positions.isEmpty())return;for(var pos:positions){var l=new Location(dimension,pos.getX(),pos.getY(),pos.getZ(),"");store.owner(l,owner);if(replies.size()>=256)replies.clear();replies.put(l.key(),new Result(dimension,pos,owner,false,StorageOwnerProtocol.OK));}ChestMemory.saveLater(store);
 }
 public static boolean available(){return Minecraft.getInstance().getConnection()!=null&&ClientPlayNetworking.canSend(Request.TYPE);}
 public static void clear(){pending.clear();replies.clear();}
 public static void request(ChestMemoryStore store,Location location,int action){
  var mc=Minecraft.getInstance();if(!available()||store==null||mc.level==null||!mc.level.dimension().identifier().toString().equals(location.dimension()))return;
  pending.entrySet().removeIf(e->System.nanoTime()-e.getValue().time>5_000_000_000L);
  var previous=pending.get(location.key());if(previous!=null)return;if(pending.size()>=32)return;
  pending.put(location.key(),new Pending(store,location,action,System.nanoTime()));ClientPlayNetworking.send(new Request(location.pos(),action));
 }
 public static boolean editable(Location l){var r=replies.get(l.key());return available()&&r!=null&&r.editable()&&r.status()==StorageOwnerProtocol.OK;}
 public static String label(SavedChest chest){var o=chest.owner();return o==null?Component.translatable("stow.owner.unobserved").getString():o.id().isEmpty()?Component.translatable("stow.owner.unclaimed").getString():o.id().equals("mixed")?Component.translatable("stow.owner.mixed").getString():o.shared()?Component.translatable("stow.owner.shared").getString():o.name();}
 public static String group(SavedChest chest){var o=chest.owner();return o==null||o.id().isEmpty()?"unknown":o.id().equals("mixed")?"mixed":o.shared()?"shared":o.id();}
 public static boolean matches(SavedChest chest,String filter){if(filter.equals("all"))return true;if(filter.equals("mine")){var p=Minecraft.getInstance().player;return p!=null&&group(chest).equals(p.getUUID().toString());}return group(chest).equals(filter);}
}
