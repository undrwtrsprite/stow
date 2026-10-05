package dev.stow.client.memory;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import static dev.stow.client.memory.ChestMemoryStore.*;

public final class MaterialPlanner {
    private static Screen pendingScreen;
    public record Progress(long inventory,long chests,int target,int containers) {
        public long total(){return inventory+chests;}
        public long counted(){return chests+(dev.stow.Stow.config.materialsIncludeInventory?inventory:0);}
        public String label(){return counted()+" / "+target;}
        public long missing(){return Math.max(0,target-counted());}
        public boolean enough(){return counted()>=target;}
        public int color(){return enough()?0xFF8DE8B2:counted()*5>=target*2L?0xFFFFD486:0xFFFF967D;}
    }
    public static long inventoryCount(Inventory inventory,ItemStack carried,String itemId) {
        long count=0;
        if(inventory!=null) for(int i=0;i<inventory.getContainerSize();i++)count+=stackCount(inventory.getItem(i),itemId);
        return count+stackCount(carried,itemId);
    }
    private static int stackCount(ItemStack stack,String itemId) {
        return stack!=null && !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)?stack.getCount():0;
    }
    public static Progress progress(ChestMemoryStore store,MaterialGoal goal) {
        Minecraft mc=Minecraft.getInstance();
        ChestMemory.refreshSnapshot();
        long inventory=mc.player==null?0:inventoryCount(mc.player.getInventory(),mc.player.containerMenu.getCarried(),goal.itemId());
        return new Progress(inventory,store.chestCount(goal.itemId()),goal.target(),store.includedChestCount());
    }
    public static void toggleHud(ChestMemoryStore store,String itemId){
        if(store==null)return;
        if(store.toggleHudGoal(itemId,dev.stow.Stow.config.dockRows))ChestMemory.saveLater(store);
        else Minecraft.getInstance().gui.hud.setOverlayMessage(Component.translatable("stow.hud.full",dev.stow.Stow.config.dockRows),false);
    }
    public static void quickAdd(Screen parent,ChestMemoryStore store,ItemStack stack) {
        if(stack.isEmpty() || store==null)return;
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int amount=store.goals().stream().filter(g -> g.itemId().equals(id)).mapToInt(MaterialGoal::target).findFirst().orElse(1000);
        Minecraft.getInstance().gui.setScreen(new MaterialPickerScreen(parent,store,id,amount));
    }
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher,buildContext) -> dispatcher.register(
            ClientCommands.literal("need").executes(context -> {
                ChestMemoryStore store=ChestMemory.currentStore();
                if(store==null){context.getSource().sendError(Component.translatable("stow.memory.no-world"));return 0;}
                pendingScreen=new MaterialsScreen(null,store);return 1;
            }).then(ClientCommands.argument("amount",IntegerArgumentType.integer(1,1_000_000))
                .then(ClientCommands.argument("item",StringArgumentType.greedyString())
                    .suggests((context,builder) -> {
                        String input=builder.getRemainingLowerCase();
                        for(var entry:MemoryItems.catalogue()) if(entry.id().contains(input))builder.suggest(entry.id());
                        return builder.buildFuture();
                    }).executes(context -> {
                        ChestMemoryStore store=ChestMemory.currentStore();
                        if(store==null){context.getSource().sendError(Component.translatable("stow.memory.no-world"));return 0;}
                        int amount=IntegerArgumentType.getInteger(context,"amount");
                        String query=StringArgumentType.getString(context,"item");
                        var entry=MemoryItems.resolve(MemoryItems.catalogue(),query);
                        if(entry==null) {
                            pendingScreen=new MaterialPickerScreen(new MaterialsScreen(null,store),store,query,amount);
                            context.getSource().sendFeedback(Component.translatable("stow.need.choose-exact"));return 1;
                        }
                        try{store.setGoal(entry.id(),amount);}catch(IllegalArgumentException e){context.getSource().sendError(Component.translatable("stow.need.limit"));return 0;}
                        ChestMemory.saveLater(store);
                        context.getSource().sendFeedback(Component.translatable("stow.need.added",amount,entry.name()));
                        pendingScreen=new MaterialsScreen(null,store);return 1;
                    })))));
    }
    public static void tick(Minecraft mc) {
        if(pendingScreen==null)return;
        Screen screen=pendingScreen;pendingScreen=null;
        if(mc.player!=null && mc.player.containerMenu.getCarried().isEmpty())mc.gui.setScreen(screen);
    }
    public static void disconnect(){pendingScreen=null;}
}
