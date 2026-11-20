package dev.ollasland.cinematics.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure data. No Minecraft classes: scene files can be edited outside the game. */
public final class SceneData {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String id = "templo_escape";
    public String title = "Escape del templo";
    public int duration = 400;
    public int skyTop = 0xFF0A1025;
    public int skyBottom = 0xFF263454;
    public final List<Actor> actors = new ArrayList<>();
    public final List<Beat> events = new ArrayList<>();
    public final List<CameraKey> cameras = new ArrayList<>();

    public static final class Actor {
        public String id = "actor";
        public String entity = "viewer"; // viewer or registry entity id such as minecraft:zombie
        public float x = .50f;
        public float y = .77f;
        public float scale = 1.0f;
        public final List<MotionKey> motion = new ArrayList<>();
    }
    public static final class MotionKey {
        public int tick;
        public float x, y, scale = 1;
        public MotionKey() {}
        public MotionKey(int tick, float x, float y, float scale) {
            this.tick=tick; this.x=x; this.y=y; this.scale=scale;
        }
    }
    public static final class CameraKey {
        public int tick;
        public float panX, panY, zoom=1;
        public CameraKey() {}
        public CameraKey(int tick, float panX, float panY, float zoom) {
            this.tick=tick; this.panX=panX; this.panY=panY; this.zoom=zoom;
        }
    }
    /** RHYTHM no background; QTE has a window; CHOICE pauses progression. */
    public static final class Beat {
        public String kind = "RHYTHM"; // RHYTHM, QTE, CHOICE
        public int tick = 100;
        public String key = "SPACE";
        public float x = .5f, y = .5f;
        public int window = 5;
        public int lead = 28;
        public int successJump = -1, failureJump = -1;
        public String label = "¡Reacciona!";
        public String optionA = "Saltar", optionB = "Esquivar";
        public String keyA = "A", keyB = "D";
        public int choiceAJump = -1, choiceBJump = -1;
    }
    public record Pose(float x, float y, float scale) {}

    public static float clamp(float x, float a, float b) { return Math.max(a, Math.min(b, x)); }
    // No per-frame list copies or sorting: seek nearest keyframes in-place.
    public Pose position(Actor actor, float t) {
        if(actor.motion == null || actor.motion.isEmpty()) return new Pose(actor.x,actor.y,actor.scale);
        MotionKey before=null, after=null;
        for(MotionKey k:actor.motion) {
            if(k.tick<=t && (before==null || k.tick>before.tick)) before=k;
            if(k.tick>=t && (after==null || k.tick<after.tick)) after=k;
        }
        if(before==null) return new Pose(after.x,after.y,after.scale);
        if(after==null || before.tick==after.tick) return new Pose(before.x,before.y,before.scale);
        float f=clamp((t-before.tick)/(float)(after.tick-before.tick),0,1);
        f=f*f*(3-2*f);
        return new Pose(before.x+(after.x-before.x)*f,before.y+(after.y-before.y)*f,
                        before.scale+(after.scale-before.scale)*f);
    }
    public CameraKey camera(float t) {
        if(cameras == null || cameras.isEmpty()) return new CameraKey(0,0,0,1);
        CameraKey before=null, after=null;
        for(CameraKey k:cameras) {
            if(k.tick<=t && (before==null || k.tick>before.tick)) before=k;
            if(k.tick>=t && (after==null || k.tick<after.tick)) after=k;
        }
        if(before==null) return after;
        if(after==null || before.tick==after.tick) return before;
        float f=clamp((t-before.tick)/(float)(after.tick-before.tick),0,1);
        f=f*f*(3-2*f);
        return new CameraKey((int)t,before.panX+(after.panX-before.panX)*f,
                            before.panY+(after.panY-before.panY)*f,
                            before.zoom+(after.zoom-before.zoom)*f);
    }
    public void validate() {
        if (id==null || !id.matches("[a-z0-9_\\-]{1,48}")) throw new IllegalArgumentException("ID: solo letras minusculas, digitos, _ y - (max. 48)");
        duration=Math.max(40,Math.min(72000,duration));
        if (title==null) title=id;
        if (actors==null || events==null || cameras==null) throw new IllegalArgumentException("Listas nulas");
        if (actors.size()>20 || events.size()>256 || cameras.size()>128) throw new IllegalArgumentException("Demasiados elementos");
        for (Actor a : actors) {
            if (a.entity==null || !(a.entity.equals("viewer") || a.entity.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))) throw new IllegalArgumentException("Entidad invalida");
            a.x=clamp(a.x,0,1); a.y=clamp(a.y,0,1); a.scale=clamp(a.scale,.2f,4f);
            if(a.motion==null || a.motion.size()>128) throw new IllegalArgumentException("Keyframes invalidos");
            for(MotionKey k:a.motion) {k.tick=Math.max(0,Math.min(duration,k.tick));k.x=clamp(k.x,0,1);k.y=clamp(k.y,0,1);k.scale=clamp(k.scale,.2f,4f);}
        }
        for (Beat e:events) {
            if(e.kind==null) throw new IllegalArgumentException("Evento sin tipo");
            e.kind=e.kind.toUpperCase(Locale.ROOT);
            if(!e.kind.equals("RHYTHM") && !e.kind.equals("QTE") && !e.kind.equals("CHOICE")) throw new IllegalArgumentException("Evento desconocido");
            e.tick=Math.max(0,Math.min(duration-1,e.tick));
            e.window=Math.max(1,Math.min(40,e.window)); e.lead=Math.max(5,Math.min(80,e.lead));
            e.x=clamp(e.x,.05f,.95f); e.y=clamp(e.y,.06f,.94f);
            if(e.key==null || e.key.length()>24 || e.keyA==null || e.keyB==null) throw new IllegalArgumentException("Tecla invalida");
            if(e.label==null || e.label.length()>200) throw new IllegalArgumentException("Texto invalido");
        }
        for(CameraKey k:cameras) {
            k.tick=Math.max(0,Math.min(duration,k.tick));
            k.panX=clamp(k.panX,-1,1);k.panY=clamp(k.panY,-1,1);k.zoom=clamp(k.zoom,.4f,3);
        }
    }
    public String toJson() { return GSON.toJson(this); }
    public static SceneData fromJson(String json) {
        if(json==null || json.length()>65535) throw new IllegalArgumentException("Escena demasiado grande");
        SceneData d=GSON.fromJson(json,SceneData.class);
        if(d==null) throw new IllegalArgumentException("JSON vacio");
        d.validate(); return d;
    }
    public static SceneData demo() {
        SceneData d=new SceneData();
        Actor hero=new Actor();hero.id="protagonista";hero.entity="viewer";hero.x=.36f;hero.y=.77f;hero.scale=1.5f;
        hero.motion.add(new MotionKey(0,.14f,.77f,1.4f));
        hero.motion.add(new MotionKey(85,.35f,.77f,1.5f));
        hero.motion.add(new MotionKey(155,.54f,.77f,1.65f));
        d.actors.add(hero);
        Actor zombie=new Actor();zombie.id="enemigo";zombie.entity="minecraft:zombie";zombie.x=.77f;zombie.y=.77f;zombie.scale=1.3f;
        zombie.motion.add(new MotionKey(0,.83f,.77f,1.3f));
        zombie.motion.add(new MotionKey(155,.62f,.77f,1.4f));
        d.actors.add(zombie);
        d.cameras.add(new CameraKey(0,0,0,.82f));
        d.cameras.add(new CameraKey(100,0,0,1.05f));
        d.cameras.add(new CameraKey(175,-.07f,0,1.2f));
        for (int i=0;i<6;i++) {
            Beat b=new Beat();b.kind="RHYTHM";b.tick=60+i*20;
            b.key=new String[]{"A","S","D","W","A","SPACE"}[i];b.x=.30f+(i%3)*.20f;b.y=.37f+(i%2)*.19f;
            b.window=5;b.lead=24;d.events.add(b);
        }
        Beat choice=new Beat(); choice.kind="CHOICE";choice.tick=200;choice.label="El zombie te bloquea el camino";
        choice.optionA="Atacar";choice.optionB="Esquivar";choice.choiceAJump=240;choice.choiceBJump=280;
        d.events.add(choice);
        Beat qte=new Beat();qte.kind="QTE";qte.tick=245;qte.key="SPACE";qte.window=20;qte.label="¡Pulsa ESPACIO!";
        qte.successJump=310;qte.failureJump=345;d.events.add(qte);
        return d;
    }
}
