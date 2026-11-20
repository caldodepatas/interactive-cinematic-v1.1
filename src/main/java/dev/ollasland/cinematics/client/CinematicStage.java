package dev.ollasland.cinematics.client;

import dev.ollasland.cinematics.data.SceneData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.IdentityHashMap;
import java.util.Map;

/** Client-only scene compositor: parallax set + real entity 3D renderers. */
public final class CinematicStage {
    private record CachedEntity(String registryId, LivingEntity entity) {}
    private final Map<SceneData.Actor,CachedEntity> mobCache=new IdentityHashMap<>();
    public void clear() {mobCache.clear();}
    private LivingEntity resolve(SceneData.Actor actor) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.level==null) return null;
        if(actor.entity.equals("viewer")) return mc.player; // genuine local skin and model
        CachedEntity cached=mobCache.get(actor);
        if(cached!=null && cached.registryId.equals(actor.entity)) return cached.entity;
        try {
            ResourceLocation id=ResourceLocation.tryParse(actor.entity);
            if(id==null) return null;
            EntityType<?> type=ForgeRegistries.ENTITY_TYPES.getValue(id);
            if(type==null || type==EntityType.PLAYER) return null;
            Entity entity=type.create(mc.level); // NEVER added to a world or server
            if(entity instanceof LivingEntity living) {
                if(mc.player!=null) living.setPos(mc.player.getX(),mc.player.getY(),mc.player.getZ());
                mobCache.put(actor,new CachedEntity(actor.entity,living));return living;
            }
        } catch(Exception ignored) { /* unsupported or unavailable entity type */ }
        return null;
    }
    public void render(GuiGraphics g,SceneData d,float tick,int left,int top,int w,int h,boolean editor) {
        int right=left+w, bottom=top+h;
        g.enableScissor(left,top,right,bottom);
        g.fillGradient(left,top,right,bottom,d.skyTop,d.skyBottom);
        SceneData.CameraKey cam=d.camera(tick);
        // Static geometric stage is cheap and deterministic (no world chunk rendering).
        int horizon=top+(int)(h*.65);
        g.fill(left,horizon,right,bottom,0xFF101726);
        g.fill(left,horizon,right,horizon+2,0xFF53618C);
        int vanishingX=left+w/2-(int)(cam.panX*w*.12);
        for(int i=-6;i<=6;i++) {
            int target=vanishingX+i*w/4;
            drawLine(g,vanishingX,horizon,target,bottom,0xFF2C3855);
        }
        for(int i=1;i<=5;i++) {
            int yy=horizon+(int)(h*.35*i*i/25.0);
            g.fill(left,yy,right,yy+1,0xFF334260);
        }
        // Architectural columns create depth without loading a game dimension.
        for(int i=0;i<4;i++) {
            int x=left+(int)(w*(.07+i*.28));
            int colW=Math.max(3,w/75);
            g.fill(x,top+(int)(h*.13),x+colW,horizon,0xFF192239);
            g.fill(x-colW,top+(int)(h*.13),x+2*colW,top+(int)(h*.15),0xFF4D5775);
        }
        // Entity renderer can render registered LivingEntity mod types, without server AI.
        for(SceneData.Actor a:d.actors) {
            SceneData.Pose pose=d.position(a,tick);
            float zoom=SceneData.clamp(cam.zoom,.4f,3f);
            int x=left+Math.round((.5f+(pose.x()-.5f-cam.panX)*zoom)*w);
            int y=top+Math.round((.5f+(pose.y()-.5f-cam.panY)*zoom)*h);
            if(x<left-180 || x>right+180 || y<top-180 || y>bottom+180) continue;
            LivingEntity entity=resolve(a);
            if(entity==null) {
                g.drawCenteredString(Minecraft.getInstance().font,"[Entidad no disponible]",x,y,0xFFFF9999);
                continue;
            }
            int size=Math.max(12,Math.min(170,Math.round((h/5.0f)*pose.scale()*zoom)));
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, x, y, size, 0.0f, 0.0f, entity);
            } catch(Exception ex) {
                g.drawCenteredString(Minecraft.getInstance().font,"[Error render: "+a.id+"]",x,y,0xFFFF7777);
            }
            if(editor) g.drawCenteredString(Minecraft.getInstance().font,a.id,x,y+11,0xFFE0E5F5);
        }
        g.disableScissor();
    }
    public static void drawLine(GuiGraphics g,int x1,int y1,int x2,int y2,int color) {
        int steps=Math.max(Math.abs(x2-x1),Math.abs(y2-y1));
        if(steps==0) return;
        for(int i=0;i<=steps;i++) {
            int x=x1+(x2-x1)*i/steps,y=y1+(y2-y1)*i/steps;
            g.fill(x,y,x+1,y+1,color);
        }
    }
}
