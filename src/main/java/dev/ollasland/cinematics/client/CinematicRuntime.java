package dev.ollasland.cinematics.client;

import dev.ollasland.cinematics.data.SceneData;
import org.lwjgl.glfw.GLFW;
import java.util.HashSet;
import java.util.Set;

/** All input and animation timeline state is per-client, never shared globally. */
public final class CinematicRuntime {
    public final SceneData scene;
    public int tick, hits, misses;
    public boolean finished;
    public String finishReason="completo";
    public int feedbackTicks;
    public String feedback="";
    private final Set<Integer> resolved = new HashSet<>();
    private int choiceIndex=-1;
    private int choiceWait;
    public CinematicRuntime(SceneData scene) {this.scene=scene;}
    public int choiceIndex() {return choiceIndex;}
    public boolean completed(int i) {return resolved.contains(i);}
    public void tick() {
        if(finished) return;
        if(feedbackTicks>0) feedbackTicks--;
        if(choiceIndex>=0) {
            choiceWait++;
            if(choiceWait>200) decide(false);
            return;
        }
        for(int i=0;i<scene.events.size();i++) {
            SceneData.Beat b=scene.events.get(i);
            if(resolved.contains(i)) continue;
            if(b.kind.equals("CHOICE") && tick>=b.tick) {choiceIndex=i;choiceWait=0;return;}
            if(b.kind.equals("RHYTHM") && tick>b.tick+b.window) {
                resolved.add(i);misses++;feedback("MISS"); jump(b.failureJump);
            }
            if(b.kind.equals("QTE") && tick>b.tick+b.window) {
                resolved.add(i);misses++;feedback("FAIL");jump(b.failureJump);
            }
        }
        tick++;
        if(tick>=scene.duration) {finished=true;finishReason="final";}
    }
    public boolean input(int keyCode) {
        if(finished) return false;
        if(choiceIndex>=0) {
            SceneData.Beat b=scene.events.get(choiceIndex);
            if(matches(keyCode,b.keyA)) {decide(true);return true;}
            if(matches(keyCode,b.keyB)) {decide(false);return true;}
            return false;
        }
        // Closest note wins (osu!-style timing judge).
        int best=-1, bestDistance=Integer.MAX_VALUE;
        for(int i=0;i<scene.events.size();i++) {
            SceneData.Beat b=scene.events.get(i);
            if(resolved.contains(i) || !matches(keyCode,b.key)) continue;
            int distance;
            if(b.kind.equals("RHYTHM")) distance=Math.abs(tick-b.tick);
            else if(b.kind.equals("QTE")) distance=tick>=b.tick?tick-b.tick:Integer.MAX_VALUE;
            else continue;
            if(distance<=b.window && distance<bestDistance) {best=i;bestDistance=distance;}
        }
        if(best>=0) {
            SceneData.Beat b=scene.events.get(best);
            resolved.add(best);hits++;
            feedback(bestDistance<=2?"PERFECT":bestDistance<=4?"GREAT":"GOOD");
            jump(b.successJump);
            return true;
        }
        return false;
    }
    private void decide(boolean a) {
        if(choiceIndex<0) return;
        SceneData.Beat b=scene.events.get(choiceIndex);
        resolved.add(choiceIndex);choiceIndex=-1;
        jump(a?b.choiceAJump:b.choiceBJump);
    }
    private void feedback(String message) {feedback=message;feedbackTicks=12;}
    private void jump(int target) {
        if(target>tick && target<scene.duration) {
            // A branch skipping a QTE must not count that QTE as a failure.
            for(int i=0;i<scene.events.size();i++) {
                SceneData.Beat e=scene.events.get(i);
                if(e.tick>tick && e.tick<target) resolved.add(i);
            }
            tick=target;
        }
    }
    public static boolean matches(int code,String value) {return code==toKeyCode(value);}
    public static int toKeyCode(String value) {
        if(value==null) return -1;
        String k=value.toUpperCase(java.util.Locale.ROOT).trim();
        return switch(k) {
            case "SPACE", "ESPACIO" -> GLFW.GLFW_KEY_SPACE;
            case "ENTER" -> GLFW.GLFW_KEY_ENTER;
            case "SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case "CTRL" -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case "UP" -> GLFW.GLFW_KEY_UP;
            case "DOWN" -> GLFW.GLFW_KEY_DOWN;
            case "LEFT" -> GLFW.GLFW_KEY_LEFT;
            case "RIGHT" -> GLFW.GLFW_KEY_RIGHT;
            default -> k.length()==1 ? (int)k.charAt(0) : -1;
        };
    }
    public static String displayKey(String key) {
        if(key==null) return "?";
        return key.equalsIgnoreCase("SPACE")?"ESPACIO":key.toUpperCase(java.util.Locale.ROOT);
    }
}
