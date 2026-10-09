package dev.stow.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.*;

/** One vanilla item-model family for actions, with native-sized symbols for basic controls. */
public final class UIIcons {
    public static final int SIZE=16;
    public enum Kind { STORAGE,MATERIALS,SORT,COMMAND,DEPOSIT,EYE,EYE_OFF,STAR,COPY,GRID,BOTH,CHECK,CLOSE,PLUS,MINUS,LEFT,RIGHT,PROJECT,KEEP,KEYBOARD,SETTINGS,EQUIPMENT,TRACK,FIND }
    private UIIcons(){}
    public static Item actionItem(Kind kind){return switch(kind){
        case STORAGE->Items.CHEST;case MATERIALS,TRACK->Items.WRITABLE_BOOK;
        case SORT->Items.COMPARATOR;case COMMAND->Items.COMMAND_BLOCK;case DEPOSIT->Items.HOPPER;
        case EYE->Items.ENDER_EYE;case EYE_OFF->Items.ENDER_PEARL;
        case COPY->Items.PAPER;case GRID->Items.SHULKER_BOX;case BOTH->Items.BUNDLE;
        case PROJECT->Items.MAP;case KEEP->Items.SHIELD;case KEYBOARD->Items.LEVER;
        case SETTINGS->Items.REPEATER;case EQUIPMENT->Items.IRON_PICKAXE;case FIND->Items.SPYGLASS;
        default->null;
    };}
    public static void draw(GuiGraphicsExtractor g,Kind kind,int x,int y,int color){
        if(kind==Kind.STAR){dev.stow.client.inventory.SlotMarkers.star(g,x+(SIZE-7)/2,y+(SIZE-7)/2,color);return;}
        var item=actionItem(kind);
        if(item!=null){if(item.builtInRegistryHolder().areComponentsBound())g.item(new ItemStack(item),x,y);if(kind==Kind.TRACK)symbol(g,"..X../..X../XXXXX/..X../..X..",x+10,y+10,0xFF80FF80);return;}
        String pattern=switch(kind){
            case PLUS->"...X.../...X.../...X.../XXXXXXX/...X.../...X.../...X...";
            case MINUS->"XXXXXXX";
            case CHECK->"......X/.....XX/X...XX./XX.XX../.XXX.../..X....";
            case CLOSE->"X.....X/.X...X./..X.X../...X.../..X.X../.X...X./X.....X";
            case LEFT->"...X../..X.../.X..../X...../.X..../..X.../...X..";
            case RIGHT->"..X.../...X../....X./.....X/....X./...X../..X...";
            default->"";
        };
        String[] rows=pattern.split("/");int width=0;for(String row:rows)width=Math.max(width,row.length());symbol(g,pattern,x+(SIZE-width)/2,y+(SIZE-rows.length)/2,color);
    }
    private static void symbol(GuiGraphicsExtractor g,String pattern,int x,int y,int color){String[] rows=pattern.split("/");for(int pass=1;pass>=0;pass--)for(int row=0;row<rows.length;row++)for(int col=0;col<rows[row].length();col++)if(rows[row].charAt(col)=='X')g.fill(x+col+pass,y+row+pass,x+col+pass+1,y+row+pass+1,pass==1?0xFF252525:color);}
}
