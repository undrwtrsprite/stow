package dev.stow.client.inventory;

/** GUI-pixel positions, outside vanilla slots at every supported GUI scale. */
public record InventoryExtrasLayout(int modeX,int modeY,int modeWidth,int memoryX,int memoryY,int memoryWidth,
        int searchX,int searchY,int searchWidth,int needX,int needY,int needWidth,int sortX,int sortY,
        int trackerX,int trackerY,int trackerWidth,int trackerHeight,int paletteX,int paletteY,int depositX,int depositY,int glowX,int glowY) {
    public static InventoryExtrasLayout of(int width,int height,int left,int top,int imageWidth,int imageHeight){
        int right=left+imageWidth,bottom=top+imageHeight;
        int toolbarY=top>=26?top-24:bottom+24<=height-4?bottom+4:-1;
        int mx,my,cx,cy,nx,ny,ox,oy,sx,sy,sw,px,py,dx,dy,gx,gy,ty=Math.max(4,top+4);
        int tx=right+8,tw=Math.max(0,Math.min(140,width-tx-4));
        if(toolbarY>=0){mx=left;my=toolbarY;cx=left+22;cy=my;nx=left+44;ny=my;ox=left+66;oy=my;px=left+88;py=my;dx=left+110;dy=my;gx=left+imageWidth-20;gy=my;sx=left+132;sy=my;sw=Math.max(1,imageWidth-156);}
        else {mx=right+4;my=Math.max(4,top);cx=mx;cy=my+24;nx=mx;ny=my+48;ox=mx;oy=my+72;px=mx;py=my+96;dx=mx;dy=my+120;gx=mx;gy=my+144;sx=4;sy=Math.max(4,top+4);sw=Math.max(1,left-8);ty=my+172;}
        int th=Math.max(0,Math.min(bottom-ty-4,height-ty-4));
        return new InventoryExtrasLayout(mx,my,20,cx,cy,20,sx,sy,sw,nx,ny,20,ox,oy,tx,ty,tw,th,px,py,dx,dy,gx,gy);
    }
}
