package dev.stow.client.network;

import java.util.*;
import net.minecraft.world.inventory.ContainerInput;

/** Collect upstream Mouse Wheelie's clicks into a plan, without touching live slots. */
public final class InteractionManager {
    public interface InteractionEvent {}
    public record ClickEvent(int menuId,int slotId,int button,ContainerInput input) implements InteractionEvent {}
    public enum TriggerType { GUI_CONFIRM }
    private static final ThreadLocal<List<ClickEvent>> collecting=new ThreadLocal<>();
    public static List<ClickEvent> plan(Runnable sort){
        if(collecting.get()!=null)throw new IllegalStateException("Nested sort plan");
        var clicks=new ArrayList<ClickEvent>();collecting.set(clicks);
        try{sort.run();return List.copyOf(clicks);}finally{collecting.remove();}
    }
    public static void push(InteractionEvent event){
        if(collecting.get()==null)throw new IllegalStateException("No sort plan");
        collecting.get().add((ClickEvent)event);
    }
    public static void pushAll(Collection<? extends InteractionEvent> events){events.forEach(InteractionManager::push);}
    public static void triggerSend(TriggerType type){}
    private InteractionManager(){}
}
