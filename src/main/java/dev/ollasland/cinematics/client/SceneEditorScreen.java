package dev.ollasland.cinematics.client;

import dev.ollasland.cinematics.data.SceneData;
import dev.ollasland.cinematics.data.SceneStore;
import dev.ollasland.cinematics.network.CinematicNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.UUID;
import java.util.function.Consumer;

/** Visual timeline editor. Select an actor/note in the viewport and drag to position. */
public class SceneEditorScreen extends Screen {
    private SceneData scene;
    private final CinematicStage stage=new CinematicStage();
    private int cursor=60;
    private int actorIndex=0,eventIndex=-1,cameraIndex=-1;
    private int inspectorScroll;
    private int panelX,viewW,viewH;
    private boolean dragging;
    private String status="Click en actores/notas para seleccionarlos. Rueda sobre timeline: mover tiempo.";
    private int fieldY;
    private final java.util.List<String> labels=new java.util.ArrayList<>();
    private final java.util.List<Integer> labelYs=new java.util.ArrayList<>();

    public SceneEditorScreen(SceneData scene) {
        super(Component.literal("Editor de Cinematicas"));this.scene=scene;
    }
    public void addMob(String entity) {
        SceneData.Actor a=new SceneData.Actor();a.id="mob_"+scene.actors.size();a.entity=entity;
        a.x=.66f;a.y=.74f;scene.actors.add(a);
        actorIndex=scene.actors.size()-1;eventIndex=-1;cameraIndex=-1;
        stage.clear();status="Agregado: "+entity;
    }
    @Override protected void init() {rebuildControls();}
    private void button(int x,int y,int w,String text,java.util.function.Consumer<Button> click) {
        addRenderableWidget(Button.builder(Component.literal(text),click::accept).bounds(x,y,w,18).build());
    }
    private void rebuildControls() {
        clearWidgets();labels.clear();labelYs.clear();
        panelX=Math.max(170,width-194);
        viewW=Math.max(100,panelX-11);
        viewH=Math.max(70,height-128);
        int x=6;
        button(x,4,43,"+ Skin",b->addHero());x+=45;
        button(x,4,43,"+ Mob",b->minecraft.setScreen(new MobPickerScreen(this)));x+=45;
        button(x,4,50,"+ Ritmo",b->addEvent("RHYTHM"));x+=52;
        button(x,4,44,"+ QTE",b->addEvent("QTE"));x+=46;
        button(x,4,54,"+ Elegir",b->addEvent("CHOICE"));x+=56;
        if(x+48<panelX) button(x,4,46,"+ Cam",b->addCamera());
        x=6;
        button(x,25,57,"Probar",b->preview());x+=59;
        button(x,25,61,"Guardar",b->save());x+=63;
        button(x,25,65,"Cargar ID",b->load());x+=67;
        button(x,25,43,"+ KF",b->addMotion());x+=45;
        button(x,25,54,"Borrar",b->removeSelected());
        // Inspector: scene values, then selected element fields.
        fieldY=54-inspectorScroll*21;
        property("ID",scene.id,s->scene.id=s);
        property("Titulo",scene.title,s->scene.title=s);
        property("Duracion",Integer.toString(scene.duration),s->scene.duration=toInt(s,scene.duration));
        property("Color sup",String.format("%08X",scene.skyTop),s->scene.skyTop=toHex(s,scene.skyTop));
        property("Color inf",String.format("%08X",scene.skyBottom),s->scene.skyBottom=toHex(s,scene.skyBottom));
        if(actorIndex>=0 && actorIndex<scene.actors.size()) {
            SceneData.Actor a=scene.actors.get(actorIndex);
            property("Actor ID",a.id,s->a.id=s);
            property("Entidad",a.entity,s->a.entity=s);
            property("Pos X",f(a.x),s->a.x=toFloat(s,a.x));
            property("Pos Y",f(a.y),s->a.y=toFloat(s,a.y));
            property("Escala",f(a.scale),s->a.scale=toFloat(s,a.scale));
        } else if(eventIndex>=0 && eventIndex<scene.events.size()) {
            SceneData.Beat e=scene.events.get(eventIndex);
            property("Tipo",e.kind,s->e.kind=s.toUpperCase(java.util.Locale.ROOT));
            property("Tiempo",Integer.toString(e.tick),s->e.tick=toInt(s,e.tick));
            property("Tecla",e.key,s->e.key=s.toUpperCase(java.util.Locale.ROOT));
            property("Pos X",f(e.x),s->e.x=toFloat(s,e.x));
            property("Pos Y",f(e.y),s->e.y=toFloat(s,e.y));
            property("Margen",Integer.toString(e.window),s->e.window=toInt(s,e.window));
            property("Anticip.",Integer.toString(e.lead),s->e.lead=toInt(s,e.lead));
            property("Acierto ->",Integer.toString(e.successJump),s->e.successJump=toInt(s,e.successJump));
            property("Fallo ->",Integer.toString(e.failureJump),s->e.failureJump=toInt(s,e.failureJump));
            property("Texto",e.label,s->e.label=s);
            property("Opcion A",e.optionA,s->e.optionA=s);
            property("Tecla A",e.keyA,s->e.keyA=s);
            property("Salto A",Integer.toString(e.choiceAJump),s->e.choiceAJump=toInt(s,e.choiceAJump));
            property("Opcion B",e.optionB,s->e.optionB=s);
            property("Tecla B",e.keyB,s->e.keyB=s);
            property("Salto B",Integer.toString(e.choiceBJump),s->e.choiceBJump=toInt(s,e.choiceBJump));
        } else if(cameraIndex>=0 && cameraIndex<scene.cameras.size()) {
            SceneData.CameraKey c=scene.cameras.get(cameraIndex);
            property("Tiempo",Integer.toString(c.tick),s->c.tick=toInt(s,c.tick));
            property("Pan X",f(c.panX),s->c.panX=toFloat(s,c.panX));
            property("Pan Y",f(c.panY),s->c.panY=toFloat(s,c.panY));
            property("Zoom",f(c.zoom),s->c.zoom=toFloat(s,c.zoom));
        }
    }
    private static String f(float v) {return String.format(java.util.Locale.US,"%.3f",v);}
    private static float toFloat(String s,float original) {
        try {return Float.parseFloat(s.replace(',','.'));}catch(NumberFormatException e){return original;}
    }
    private static int toInt(String s,int original) {
        try {return Integer.parseInt(s);}catch(NumberFormatException e){return original;}
    }
    private static int toHex(String s,int original) {
        try {return (int)Long.parseLong(s.replace("#",""),16);}catch(NumberFormatException e){return original;}
    }
    private void property(String label,String initial,Consumer<String> changed) {
        int y=fieldY;fieldY+=21;
        if(y<51 || y>height-95) return;
        labels.add(label);labelYs.add(y+5);
        EditBox edit=new EditBox(font,panelX+72,y,Math.max(90,width-panelX-79),18,Component.literal(label));
        edit.setMaxLength(180);edit.setValue(initial==null?"":initial);
        edit.setResponder(changed);addRenderableWidget(edit);
    }
    private void addHero() {
        SceneData.Actor a=new SceneData.Actor();a.id="skin_"+scene.actors.size();a.entity="viewer";
        a.x=.45f;a.y=.75f;scene.actors.add(a);
        actorIndex=scene.actors.size()-1;eventIndex=-1;cameraIndex=-1;rebuildControls();
    }
    private void addEvent(String kind) {
        SceneData.Beat e=new SceneData.Beat();e.kind=kind;e.tick=cursor;
        e.key=kind.equals("RHYTHM")?"A":"SPACE";e.label="¡Pulsa la tecla!";
        scene.events.add(e);eventIndex=scene.events.size()-1;
        actorIndex=-1;cameraIndex=-1;inspectorScroll=0;rebuildControls();
    }
    private void addCamera() {
        var c=scene.camera(cursor);
        scene.cameras.add(new SceneData.CameraKey(cursor,c.panX,c.panY,c.zoom));
        cameraIndex=scene.cameras.size()-1;actorIndex=-1;eventIndex=-1;rebuildControls();
    }
    private void addMotion() {
        if(actorIndex<0 || actorIndex>=scene.actors.size()) return;
        var a=scene.actors.get(actorIndex);
        var p=scene.position(a,cursor);
        a.motion.add(new SceneData.MotionKey(cursor,p.x(),p.y(),p.scale()));
        status="Keyframe de movimiento anadido en tick "+cursor;
    }
    private void removeSelected() {
        if(eventIndex>=0 && eventIndex<scene.events.size())scene.events.remove(eventIndex);
        else if(cameraIndex>=0 && cameraIndex<scene.cameras.size())scene.cameras.remove(cameraIndex);
        else if(actorIndex>=0 && actorIndex<scene.actors.size())scene.actors.remove(actorIndex);
        eventIndex=-1;cameraIndex=-1;actorIndex=-1;inspectorScroll=0;stage.clear();rebuildControls();
    }
    private void preview() {
        try {scene.validate();minecraft.setScreen(new CinematicScreen(scene,new UUID(0,0),this));}
        catch(Exception e) {status="Error: "+e.getMessage();}
    }
    private void save() {
        try {
            scene.validate();SceneStore.save(scene);
            if(minecraft.player!=null) CinematicNetwork.CHANNEL.sendToServer(new CinematicNetwork.Upload(scene.toJson()));
            status="Guardado local y enviado al servidor (si tienes OP): "+scene.id;
        } catch(Exception e) {status="No se pudo guardar: "+e.getMessage();}
    }
    private void load() {
        try {scene=SceneStore.load(scene.id);stage.clear();actorIndex=-1;eventIndex=-1;cameraIndex=-1;inspectorScroll=0;rebuildControls();status="Escena cargada";}
        catch(Exception e) {status="Error al cargar: "+e.getMessage();}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float pt) {
        g.fill(0,0,width,height,0xFF0C111E);
        g.fill(0,46,width,47,0xFF53647E);
        stage.render(g,scene,cursor,6,48,viewW,viewH,true);
        g.fill(panelX-2,47,width,height-75,0xFF171F32);
        for(int i=0;i<labels.size();i++)g.drawString(font,labels.get(i),panelX+4,labelYs.get(i),0xFFCCD8EA);
        // Overlay draggable event handles, independent of visibility during playback.
        for(int i=0;i<scene.events.size();i++) {
            var e=scene.events.get(i);
            int x=6+(int)(e.x*viewW),y=48+(int)(e.y*viewH);
            int c=i==eventIndex?0xFFFFFF55:(e.kind.equals("RHYTHM")?0xFF64E8FC:0xFFFFAE79);
            CinematicScreen.drawCircle(g,x,y,11,c);
            g.drawCenteredString(font,e.kind.equals("RHYTHM")?CinematicRuntime.displayKey(e.key):e.kind.equals("CHOICE")?"?":"Q",x,y-4,c);
        }
        // Timeline: camera points on upper rail; events on lower rail.
        int timelineY=height-73;
        g.fill(5,timelineY,width-5,height-17,0xFF161F31);
        g.drawString(font,"TIMELINE  "+cursor+" / "+scene.duration+" ticks  (20 ticks = 1 s)",10,timelineY+3,0xFFB4C6DF);
        int a=12,b=width-12,timelineBottom=height-26;
        g.fill(a,timelineBottom,b,timelineBottom+2,0xFF66738B);
        for(int i=0;i<scene.events.size();i++) {
            var e=scene.events.get(i);int x=a+(int)((long)e.tick*(b-a)/Math.max(1,scene.duration));
            g.fill(x-2,timelineBottom-13,x+3,timelineBottom-3,i==eventIndex?0xFFFFFF77:0xFF5FDBF4);
        }
        for(int i=0;i<scene.cameras.size();i++) {
            var c=scene.cameras.get(i);int x=a+(int)((long)c.tick*(b-a)/Math.max(1,scene.duration));
            g.fill(x-2,timelineBottom-21,x+3,timelineBottom-16,i==cameraIndex?0xFFFFFF77:0xFFA8B9FF);
        }
        int scrub=a+(int)((long)cursor*(b-a)/Math.max(1,scene.duration));
        g.fill(scrub-1,timelineBottom-22,scrub+1,timelineBottom+5,0xFFFFFFFF);
        g.drawString(font,status,7,height-12,0xFF98AFC7);
        super.render(g,mx,my,pt);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(super.mouseClicked(mx,my,button)) return true;
        if(button!=0) return false;
        if(my>=height-63 && my<=height-19) {
            int a=12,b=width-12;
            int bestEvent=-1,bestEventDistance=9,bestCam=-1,bestCamDistance=9;
            for(int i=0;i<scene.events.size();i++) {
                int x=a+(int)((long)scene.events.get(i).tick*(b-a)/Math.max(1,scene.duration));
                int distance=(int)Math.abs(mx-x);
                if(distance<bestEventDistance){bestEventDistance=distance;bestEvent=i;}
            }
            for(int i=0;i<scene.cameras.size();i++) {
                int x=a+(int)((long)scene.cameras.get(i).tick*(b-a)/Math.max(1,scene.duration));
                int distance=(int)Math.abs(mx-x);
                if(distance<bestCamDistance){bestCamDistance=distance;bestCam=i;}
            }
            if(bestCam>=0 && my<height-43) {cameraIndex=bestCam;eventIndex=-1;actorIndex=-1;}
            else if(bestEvent>=0) {eventIndex=bestEvent;cameraIndex=-1;actorIndex=-1;}
            cursor=(int)SceneData.clamp((float)((mx-a)*scene.duration/(b-a)),0,scene.duration);
            inspectorScroll=0;rebuildControls();return true;
        }
        if(mx>=6 && mx<=6+viewW && my>=48 && my<=48+viewH) {
            float fx=(float)((mx-6)/viewW),fy=(float)((my-48)/viewH);
            float best=.07f* .07f;int ev=-1,actor=-1;
            for(int i=0;i<scene.events.size();i++) {
                var e=scene.events.get(i);
                float dist=(e.x-fx)*(e.x-fx)+(e.y-fy)*(e.y-fy);
                if(dist<best){best=dist;ev=i;actor=-1;}
            }
            for(int i=0;i<scene.actors.size();i++) {
                var p=scene.position(scene.actors.get(i),cursor);
                float dist=(p.x()-fx)*(p.x()-fx)+(p.y()-fy)*(p.y()-fy);
                if(dist<best){best=dist;actor=i;ev=-1;}
            }
            eventIndex=ev;actorIndex=actor;cameraIndex=-1;
            dragging=eventIndex>=0 || actorIndex>=0;
            inspectorScroll=0;rebuildControls();return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if(dragging && button==0 && mx>=6 && mx<=6+viewW && my>=48 && my<=48+viewH) {
            float x=SceneData.clamp((float)((mx-6)/viewW),.02f,.98f);
            float y=SceneData.clamp((float)((my-48)/viewH),.02f,.98f);
            if(eventIndex>=0 && eventIndex<scene.events.size()){
                SceneData.Beat e=scene.events.get(eventIndex);e.x=x;e.y=y;
            } else if(actorIndex>=0 && actorIndex<scene.actors.size()) {
                SceneData.Actor a=scene.actors.get(actorIndex);
                if(cursor==0 || a.motion.isEmpty()) {a.x=x;a.y=y;}
                else {
                    SceneData.MotionKey target=null;
                    for(var k:a.motion) if(k.tick==cursor){target=k;break;}
                    if(target==null){var p=scene.position(a,cursor);target=new SceneData.MotionKey(cursor,x,y,p.scale());a.motion.add(target);}
                    target.x=x;target.y=y;
                }
            }
            return true;
        }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button) {
        if(dragging) {dragging=false;rebuildControls();return true;}
        return super.mouseReleased(mx,my,button);
    }
    @Override public boolean mouseScrolled(double mx,double my,double delta) {
        if(mx>=panelX && my>=50 && my<height-75) {
            inspectorScroll=Math.max(0,Math.min(22,inspectorScroll-(int)Math.signum(delta)));
            rebuildControls();return true;
        }
        if(my>=height-75) {
            cursor=Math.max(0,Math.min(scene.duration,cursor+(delta>0?-10:10)));return true;
        }
        return super.mouseScrolled(mx,my,delta);
    }
    @Override public boolean keyPressed(int key,int scan,int mods) {
        if(key==GLFW.GLFW_KEY_DELETE) {removeSelected();return true;}
        if(key==GLFW.GLFW_KEY_ESCAPE) {onClose();return true;}
        return super.keyPressed(key,scan,mods);
    }
    @Override public void onClose() {minecraft.setScreen(null);}
    @Override public boolean isPauseScreen() {return false;}
    @Override public void removed() {stage.clear();}
}
