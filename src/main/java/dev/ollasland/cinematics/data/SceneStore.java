package dev.ollasland.cinematics.data;

import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

public final class SceneStore {
    private SceneStore() {}
    private static Path folder() {return FMLPaths.CONFIGDIR.get().resolve("interactivecinematics").resolve("scenes");}
    private static Path path(String id) {
        if(!id.matches("[a-z0-9_\\-]{1,48}")) throw new IllegalArgumentException("ID de escena invalido");
        return folder().resolve(id+".json");
    }
    public static void save(SceneData data) throws IOException {
        data.validate(); Files.createDirectories(folder());
        Path file=path(data.id);
        Files.writeString(file, data.toJson(), StandardCharsets.UTF_8);
    }
    public static SceneData load(String id) throws IOException {
        Path file=path(id);
        if(!Files.exists(file) && id.equals("templo_escape")) return SceneData.demo();
        return SceneData.fromJson(Files.readString(file, StandardCharsets.UTF_8));
    }
}
