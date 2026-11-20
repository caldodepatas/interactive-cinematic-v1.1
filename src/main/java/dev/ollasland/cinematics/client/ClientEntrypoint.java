package dev.ollasland.cinematics.client;

import dev.ollasland.cinematics.data.SceneData;
import dev.ollasland.cinematics.data.SceneStore;
import net.minecraft.client.Minecraft;
import java.util.UUID;

public final class ClientEntrypoint {
    private ClientEntrypoint() {}
    public static void initialize() { /* no client-only event listeners required */ }
    public static void start(String json, UUID session) {
        try {Minecraft.getInstance().setScreen(new CinematicScreen(SceneData.fromJson(json),session));}
        catch(RuntimeException e) {
            if(Minecraft.getInstance().player!=null)
                Minecraft.getInstance().player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Escena invalida: "+e.getMessage()));
        }
    }
    public static void stop() {
        if(Minecraft.getInstance().screen instanceof CinematicScreen) Minecraft.getInstance().setScreen(null);
    }
    public static void openEditor() {
        SceneData data;
        try {data=SceneStore.load("templo_escape");}
        catch(Exception ex) {data=SceneData.demo();}
        Minecraft.getInstance().setScreen(new SceneEditorScreen(data));
    }
}
