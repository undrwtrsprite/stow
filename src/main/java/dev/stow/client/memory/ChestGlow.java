package dev.stow.client.memory;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.stow.client.mixin.GameRendererAccessor;
import net.minecraft.client.renderer.GameRenderer;

/** Project the remembered box onto the HUD, after world/shader rendering. No scene-depth writes. */
public final class ChestGlow {
    public record Target(AABB box,String label) {}
    private static final float NEAR=0.05f;
    private ChestGlow(){}
    public static void register(){
        HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR,Identifier.fromNamespaceAndPath("stow","chest_locator"),(graphics,delta)->{
            Minecraft mc=Minecraft.getInstance();Target target=ChestMemory.glowTarget();
            if(target==null||mc.level==null||!mc.gameRenderer.mainCamera().isInitialized())return;
            var camera=mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
            if(!camera.initialized||mc.player==null)return;
            double distance=mc.player.getPosition(camera.cameraEntityPartialTicks).distanceTo(target.box().getCenter());
            Target labelled=new Target(target.box(),target.label()+" · "+Math.round(distance)+" m");
            drawProjected(graphics,labelled,camera.pos,worldProjection(mc.gameRenderer));
        });
    }
    /** Match renderLevel's per-frame camera, walking, damage and portal transforms. */
    public static Matrix4f worldProjection(GameRenderer renderer){
        var frame=renderer.gameRenderState();
        var camera=frame.levelRenderState.cameraRenderState;
        var pose=new PoseStack();
        var vanilla=(GameRendererAccessor)renderer;
        vanilla.stow$bobHurt(camera,pose);
        if(frame.optionsRenderState.bobView)vanilla.stow$bobView(camera,pose);
        Matrix4f projection=new Matrix4f(camera.projectionMatrix).mul(pose.last().pose());
        var player=frame.levelRenderState.playerRenderState;
        float effect=Math.max(player.portalEffectIntensity,player.nauseaEffectIntensity)
            *frame.optionsRenderState.screenEffectScale*frame.optionsRenderState.screenEffectScale;
        if(effect>0){
            float squash=5/(effect*effect+5)-effect*0.04f;squash*=squash;
            var axis=new org.joml.Vector3f(0,net.minecraft.util.Mth.SQRT_OF_TWO/2,net.minecraft.util.Mth.SQRT_OF_TWO/2);
            float angle=player.spinningEffectAngle*((float)Math.PI/180);
            projection.rotate(angle,axis).scale(1/squash,1,1).rotate(-angle,axis);
        }
        return projection.mul(camera.viewRotationMatrix);
    }
    /** Compact far labels while retaining at least eight physical pixels of text height. */
    public static float labelScale(double distance,double guiScale){
        float scale=(float)Math.sqrt(12/Math.max(12,distance));
        float minimum=(float)Math.min(1,Math.max(0.4,8/(9*Math.max(1,guiScale))));
        return Math.max(minimum,scale);
    }
    /** Public pure projection entry point for the real-client geometry harness. */
    public static void drawProjected(GuiGraphicsExtractor g,Target target,Vec3 camera,Matrix4f projection){
        Vector4f[] corners=new Vector4f[8];AABB box=target.box();
        for(int i=0;i<8;i++)corners[i]=project(new Vec3((i&1)==0?box.minX:box.maxX,(i&2)==0?box.minY:box.maxY,(i&4)==0?box.minZ:box.maxZ),camera,projection);
        float pulse=0.5f+0.5f*(float)Math.sin(System.currentTimeMillis()/420.0);
        int halo=((int)(30+30*pulse)<<24)|0x4EF2D0;
        for(int i=0;i<8;i++)for(int bit:new int[]{1,2,4})if((i&bit)==0){
            Vector4f a=new Vector4f(corners[i]),b=new Vector4f(corners[i|bit]);
            if(a.w<=NEAR&&b.w<=NEAR)continue;
            if(a.w<NEAR)a.lerp(b,(NEAR-a.w)/(b.w-a.w));
            if(b.w<NEAR)b.lerp(a,(NEAR-b.w)/(a.w-b.w));
            double[] line={(a.x/a.w+1)*g.guiWidth()/2.0,(1-a.y/a.w)*g.guiHeight()/2.0,(b.x/b.w+1)*g.guiWidth()/2.0,(1-b.y/b.w)*g.guiHeight()/2.0};
            if(!clip(line,g.guiWidth(),g.guiHeight()))continue;
            stroke(g,line,2.5f,halo);stroke(g,line,0.75f,0xFF69FFE0);
        }
        Vec3 center=box.getCenter();Vector4f label=project(new Vec3(center.x,box.maxY+0.4,center.z),camera,projection);
        if(label.w<=NEAR)return;
        float x=(label.x/label.w+1)*g.guiWidth()/2,y=(1-label.y/label.w)*g.guiHeight()/2;
        float scale=labelScale(camera.distanceTo(center),Minecraft.getInstance().getWindow().getGuiScale());
        y-=9*scale;
        if(x<0||x>=g.guiWidth()||y<0||y>=g.guiHeight()-9*scale)return;
        var font=Minecraft.getInstance().font;
        String text=font.plainSubstrByWidth(target.label(),Math.max(20,(int)((g.guiWidth()-16)/scale)));
        float width=font.width(text)*scale;
        x=Math.max(8,Math.min(g.guiWidth()-width-8,x-width/2));
        var pose=g.pose();pose.pushMatrix();pose.translate(x,y);pose.scale(scale,scale);
        g.text(font,text,0,0,0xFFB5FFEE);pose.popMatrix();
    }
    private static Vector4f project(Vec3 point,Vec3 camera,Matrix4f matrix){return matrix.transform(new Vector4f((float)(point.x-camera.x),(float)(point.y-camera.y),(float)(point.z-camera.z),1));}
    private static void stroke(GuiGraphicsExtractor g,double[] line,float radius,int color){
        float dx=(float)(line[2]-line[0]),dy=(float)(line[3]-line[1]);float length=(float)Math.hypot(dx,dy);
        if(!Float.isFinite(length)||length<0.1f)return;
        var pose=g.pose();pose.pushMatrix();pose.translate((float)line[0],(float)line[1]);pose.rotate((float)Math.atan2(dy,dx));
        pose.scale(1,radius);g.fill(0,-1,(int)Math.ceil(length),1,color);pose.popMatrix();
    }
    /** Liang-Barsky clips projected edges, including near-plane crossings, to the HUD bounds. */
    private static boolean clip(double[] line,int width,int height){
        double x=line[0],y=line[1],dx=line[2]-x,dy=line[3]-y,t0=0,t1=1;
        if(!Double.isFinite(x+y+dx+dy))return false;
        double[] p={-dx,dx,-dy,dy},q={x,width-1-x,y,height-1-y};
        for(int i=0;i<4;i++){
            if(p[i]==0){if(q[i]<0)return false;continue;}
            double t=q[i]/p[i];if(p[i]<0)t0=Math.max(t0,t);else t1=Math.min(t1,t);
            if(t0>t1)return false;
        }
        line[0]=x+t0*dx;line[1]=y+t0*dy;line[2]=x+t1*dx;line[3]=y+t1*dy;return true;
    }
}
