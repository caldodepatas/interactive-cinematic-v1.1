package dev.ollasland.cinematics.client;

import dev.ollasland.cinematics.data.SceneData;
import dev.ollasland.cinematics.network.CinematicNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.UUID;

/** Full-window cinema. Rhythm notes have no panels, textures or backgrounds. */
public class CinematicScreen extends Screen {
    private final CinematicRuntime playback;
    private final CinematicStage stage = new CinematicStage();
    private final UUID session;
    private final Screen parent;
    private boolean reported;
    public CinematicScreen(SceneData scene, UUID session) {this(scene,session,null);}
    public CinematicScreen(SceneData scene,UUID session,Screen parent) {
        super(Component.literal("Interactive Cinematics"));
        this.playback=new CinematicRuntime(scene);this.session=session;this.parent=parent;
    }
    @Override public boolean isPauseScreen() {return false;}
    @Override public void tick() {
        playback.tick();
        if(playback.finished) {
            report();
            if(minecraft!=null) minecraft.setScreen(parent);
        }
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        stage.render(g,playback.scene,playback.tick+partialTick,0,0,width,height,false);
        int bar=Math.max(8,height/16);
        g.fill(0,0,width,bar,0xF4000000);
        g.fill(0,height-bar,width,height,0xF4000000);
        for(int i=0;i<playback.scene.events.size();i++) {
            if(playback.completed(i)) continue;
            SceneData.Beat e=playback.scene.events.get(i);
            if(e.kind.equals("RHYTHM") && playback.tick>=e.tick-e.lead && playback.tick<=e.tick+e.window) {
                drawRhythm(g,e,playback.tick+partialTick);
            } else if(e.kind.equals("QTE") && playback.tick>=e.tick && playback.tick<=e.tick+e.window) {
                drawNormalQte(g,e);
            }
        }
        int choice=playback.choiceIndex();
        if(choice>=0) drawChoice(g,playback.scene.events.get(choice));
        if(playback.feedbackTicks>0) {
            int col=playback.feedback.equals("PERFECT")?0xFF81FFF2:0xFFFFB477;
            g.drawCenteredString(font,playback.feedback,width/2,height/5,col);
        }
        g.drawString(font,"ESC: salir",6,height-12,0xFF8C93A8);
    }
    private void drawRhythm(GuiGraphics g,SceneData.Beat beat,float time) {
        int x=Math.round(width*beat.x),y=Math.round(height*beat.y);
        // White target circle plus approaching ring. No box/fill behind the note.
        drawCircle(g,x,y,19,0xCCFFFFFF);
        float relative=(beat.tick-time)/(float)Math.max(1,beat.lead);
        int radius=Math.max(10,19+Math.round(Math.max(0,relative)*45));
        int color=time>beat.tick+beat.window/2f?0xFFFF7777:0xFF61D9FF;
        drawCircle(g,x,y,radius,color);
        String key=CinematicRuntime.displayKey(beat.key);
        int strWidth=font.width(key);
        // Letter, not a numbered osu! hit-circle.
        g.drawString(font,key,x-strWidth/2,y-4,0xFFFFFFFF,true);
    }
    public static void drawCircle(GuiGraphics g,int cx,int cy,int radius,int color) {
        final int SEGMENTS=64;
        for(int i=0;i<SEGMENTS;i++) {
            double a=i*2*Math.PI/SEGMENTS, b=(i+1)*2*Math.PI/SEGMENTS;
            CinematicStage.drawLine(g,cx+(int)(radius*Math.cos(a)),cy+(int)(radius*Math.sin(a)),
                cx+(int)(radius*Math.cos(b)),cy+(int)(radius*Math.sin(b)),color);
        }
    }
    private void drawNormalQte(GuiGraphics g,SceneData.Beat e) {
        int x=(int)(width*e.x),y=(int)(height*e.y);
        g.fill(x-75,y-19,x+75,y+22,0xD9101525);
        g.drawCenteredString(font,e.label,x,y-12,0xFFFFFFFF);
        g.drawCenteredString(font,"[ "+CinematicRuntime.displayKey(e.key)+" ]",x,y+4,0xFF6FE8FF);
    }
    private void drawChoice(GuiGraphics g,SceneData.Beat e) {
        int x=width/2,y=height*2/3;
        g.fill(x-120,y-25,x+120,y+38,0xD90B1424);
        g.drawCenteredString(font,e.label,x,y-17,0xFFFFFFFF);
        g.drawCenteredString(font,"["+e.keyA+"] "+e.optionA,x,y,0xFF7DE2FA);
        g.drawCenteredString(font,"["+e.keyB+"] "+e.optionB,x,y+16,0xFFFFD29A);
    }
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers) {
        if(keyCode==256) {onClose();return true;}
        if(playback.input(keyCode)) return true;
        return super.keyPressed(keyCode,scanCode,modifiers);
    }
    private void report() {
        if(reported) return;
        reported=true;
        if(!session.equals(new UUID(0,0))) CinematicNetwork.CHANNEL.sendToServer(
            new CinematicNetwork.Result(session,playback.finishReason+" | Aciertos="+playback.hits+", fallos="+playback.misses));
    }
    @Override public void onClose() {
        playback.finishReason="salida";
        report();
        if(minecraft!=null) minecraft.setScreen(parent);
    }
    @Override public void removed() {stage.clear();}
}
