package dev.stow.client.hud;

import dev.stow.Stow;
import dev.stow.client.inventory.HandRefill;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;

/** One compact stock count beside the hotbar; chest snapshots are not spendable reserves. */
public final class BuildingStockHud {
    public record Bounds(int x,int y,float scale) {}
    private BuildingStockHud(){}
    public static void register(){HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,Identifier.fromNamespaceAndPath("stow","building_stock"),(g,delta)->{
        var mc=Minecraft.getInstance();if(mc.player==null||mc.gui.screen()!=null||mc.gui.hud.isHidden()||mc.player.getAbilities().instabuild)return;
        var item=HandRefill.stockItem(mc.player);draw(g,item,mc.player.getInventory(),mc.player.getMainArm()==HumanoidArm.LEFT&&!mc.player.getOffhandItem().isEmpty());
    });}
    public static long count(Inventory inventory,ItemStack item){long total=0;if(item.isEmpty())return 0;for(int i=0;i<36;i++){var stack=inventory.getItem(i);if(ItemStack.isSameItemSameComponents(stack,item))total+=stack.getCount();}return total;}
    public static Bounds bounds(int width,int height,int contentWidth,float guiScale,boolean offhandRight){
        float scale=HudStyle.scale(guiScale,100);int x=width/2+91+(offhandRight?36:6);int y=height-11-(int)Math.ceil(8*scale);
        if(x+contentWidth*scale>width-4||16*scale>22){x=width/2+91-(int)Math.ceil(contentWidth*scale);y=height-38-(int)Math.ceil(16*scale);}
        return new Bounds(Math.clamp(x,4,Math.max(4,width-(int)Math.ceil(contentWidth*scale)-4)),Math.max(4,y),scale);
    }
    public static void draw(GuiGraphicsExtractor g,ItemStack item,Inventory inventory,boolean offhandRight){
        if(!Stow.config.buildingStock||item.isEmpty()||!(item.getItem() instanceof BlockItem))return;
        var mc=Minecraft.getInstance();long amount=count(inventory,item);String label=Long.toString(amount);var box=bounds(g.guiWidth(),g.guiHeight(),HudStyle.TEXT_X+mc.font.width(label),(float)mc.getWindow().getGuiScale(),offhandRight);
        g.pose().pushMatrix();g.pose().translate(box.x(),box.y());g.pose().scale(box.scale(),box.scale());g.item(item.copyWithCount(1),0,0);g.text(mc.font,label,HudStyle.TEXT_X,HudStyle.TEXT_Y,amount==0?0xFFFFAA91:0xFFFFFFFF);g.pose().popMatrix();
    }
}
