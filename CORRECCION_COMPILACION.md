# Corrección de compilación — Forge 1.20.1 (v0.1.1)

En GitHub Actions el error `:compileJava` ocurrió por acceso directo a tres campos privados de `SceneData.Pose` y por llamar a una variante de `InventoryScreen.renderEntityInInventoryFollowsMouse` que no existe en las dependencias usadas.

## Cambios concretos

Archivo `src/main/java/dev/ollasland/cinematics/client/CinematicStage.java`:

- `pose.x` -> `pose.x()`
- `pose.y` -> `pose.y()`
- `pose.scale` -> `pose.scale()`
- Firma antigua de `InventoryScreen` con 10 argumentos -> `InventoryScreen.renderEntityInInventoryFollowsMouse(g, x, y, size, 0.0f, 0.0f, entity);` (7 argumentos).
- `new ResourceLocation(actor.entity)` -> `ResourceLocation.tryParse(actor.entity)` con comprobación nula.

Archivo `src/main/java/dev/ollasland/cinematics/network/CinematicNetwork.java`:

- `new ResourceLocation(InteractiveCinematics.MODID, "main")` -> `ResourceLocation.tryBuild(InteractiveCinematics.MODID, "main")`.

## Compilar en GitHub

Reemplaza el contenido del repositorio con el de la carpeta `interactivecinematics-forge-1.20.1` de este ZIP. Después ve a **Actions > Compilar Forge 1.20.1 > Run workflow**.

Si lo haces en tu PC con Java 17 y Gradle 8.8:

```sh
gradle --no-daemon clean build
```

El artefacto, cuando el trabajo termine correctamente, se encuentra en `build/libs/`.

**La compilación no ha podido ejecutarse en el entorno de esta revisión.**
