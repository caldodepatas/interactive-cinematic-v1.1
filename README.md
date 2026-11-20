# Interactive Cinematics — Forge 1.20.1

**Estado: fuentes del prototipo v0.1.1 con corrección de los cuatro errores de `CinematicStage.java` encontrados en GitHub Actions. No se ha ejecutado una compilación real ni una prueba en Minecraft en este entorno.**

Objetivo: una misma escena para varios jugadores, cada uno ve su propia skin y toma decisiones independientes. Incluye un compositor 2.5D (escenario geométrico más entidades 3D reales), editor visual, keyframes de actores, keyframes de cámara y QTE de ritmo con teclas (sin panel de fondo). No es un motor 3D de escenarios importados desde Blockbench y aún no importa archivos `.bbmodel` o animaciones de GeckoLib. Las poses se interpolan de forma básica por keyframes de posición y escala; NO se animan todavía articulaciones individualmente.

## Requisitos
* Minecraft Java **1.20.1**, Forge **47.4.10** o compatible 47.4.x.
* JDK **17**, **Gradle 8.8** para generar el JAR (ForgeGradle resuelve las dependencias online).
* Mod instalado en **clientes y servidor**. No se necesita GeckoLib para este prototipo.

## Compilación
En Windows, abre PowerShell en la carpeta del proyecto y ejecuta:

```powershell
gradle --no-daemon clean build
```

El resultado estará en `build/libs/`. También puedes subir esta carpeta a GitHub: el archivo `.github/workflows/build.yml` configura GitHub Actions para construirlo desde **Actions > Compilar Forge 1.20.1 > Run workflow**. Si Gradle detecta errores en la compilación, deben corregirse antes de instalar el mod. Esta entrega no garantiza compilación en el primer intento.

## Uso
1. Instala el JAR compilado en `mods/` de clientes y servidor.
2. En el mundo, con permiso OP nivel 2, usa `/cine editor` para abrir la interfaz visual.
3. Añade `+ Skin` o `+ Mob`. El selector de mobs consulta las entidades registradas en Forge, incluidos los mods instalados que no sean de categoría MISC.
4. Arrastra actores en la vista. Usa `+ KF` y el cursor de la línea de tiempo para animar posiciones. Usa `+ Cam` para agregar una cámara; ajusta `Pan X`, `Pan Y` y `Zoom` en el inspector.
5. Añade `+ Ritmo`, `+ QTE` o `+ Elegir`. Selecciona el evento en la escena o en su marca temporal para editar `Tiempo`, `Tecla`, `Margen`, `Anticip.`, `Pos X`, `Pos Y` y saltos según acierto/fallo.
6. Desplaza la rueda sobre el inspector para ver propiedades adicionales. `Probar` ejecuta una vista previa local sin consecuencias de servidor.
7. `Guardar` escribe `config/interactivecinematics/scenes/<id>.json` en el cliente y solicita publicar la misma escena al servidor. **Solo los OP nivel 2** pueden publicarla.
8. Ejecuta `/cine start templo_escape @a[team=JUGADORES]` para iniciar la demostración, o `/cine start <id> <selectores>` para cualquier escena. `/cine preview <id>` prueba la escena en tu cliente; `/cine stop @a` finaliza sesiones.

Escena de demostración: `templo_escape` se genera en memoria como fallback sin necesidad de archivo. Contiene héroe con skin local, zombie, seis notas de ritmo, una decisión y un QTE normal.

## Controles del editor
- **Clic en actor o QTE**: seleccionar elemento; **arrastrar**: cambiar posición.
- **Clic en timeline**: mover el tiempo; clic cerca de marca de evento/cámara: seleccionarla.
- **Rueda en timeline**: avanzar o retroceder de 10 en 10 ticks.
- **Rueda sobre inspector**: más campos de edición.
- **Supr**: borrar elemento seleccionado.
- **+ KF**: insertar keyframe del actor en el momento actual; arrastrar el actor en ese momento actualiza su keyframe.
- **QTE de ritmo**: anillo que se reduce hasta coincidir con el anillo objetivo, pulsar la letra en el margen permitido. No hay panel opaco bajo el QTE de ritmo.

## Limitaciones importantes
- El compositor actualmente tiene un fondo geométrico **2.5D**, no importa entornos 3D de Blockbench ni sistemas avanzados de luces/sombras; usa renderizado 3D vanilla para skins/mobs.
- Los modelos de mods solo funcionan si su `EntityType` es instanciable en cliente y tiene renderer registrado. Los mobs se generan **solo en memoria local**, sin AI ni spawn en servidor. Algunos modelos pueden no renderizar correctamente porque sus animaciones dependen de estado/ticks del mundo.
- Las skins normales del cliente sí se muestran al utilizar su jugador real como actor, pero animaciones de articulaciones personalizadas y compatibilidad con rigs de Figura/CPM todavía no están implementadas.
- El criterio de acierto de QTE es calculado en cliente. Los resultados enviados al servidor son **informativos**, no deben usarse para recompensas, muertes o privilegios sin validación segura.
- El usuario continúa presente en el mundo real mientras ve la pantalla. Este prototipo **no aplica invulnerabilidad, congelación o teleport**. Si lo necesitas, implementa controles del lado servidor.
- La arquitectura prioriza no duplicar actores en el servidor. No existen todavía pruebas de rendimiento para 30–40 usuarios; el FPS de cada jugador depende principalmente de la complejidad de modelos y cantidad de actores activos.
- La red impone 65.535 caracteres de JSON por escena; el validador limita actores, eventos y keyframes.

## Organización
`SceneData` define escenas, actores, eventos, keyframes y cámaras. `SceneStore` maneja JSON local y servidor. `CinematicNetwork` coordina inicio/final y autoriza publicaciones. `CinematicRuntime` evalúa entradas QTE y elecciones localmente. `CinematicStage` dibuja escenas y entidades registradas. `CinematicScreen` reproduce QTE, ritmo y elecciones a pantalla completa. `SceneEditorScreen` es el editor visual; `MobPickerScreen` enumera mobs del registro de Forge.


## Correcciones v0.1.1 (20 de noviembre de 2026)
- `CinematicStage`: acceso a componentes de `SceneData.Pose` mediante `pose.x()`, `pose.y()`, `pose.scale()`.
- `CinematicStage`: adaptación a la firma de **7 parámetros** de `InventoryScreen.renderEntityInInventoryFollowsMouse` de Minecraft 1.20.1.
- `CinematicStage`, `CinematicNetwork`: reemplazo de constructores de `ResourceLocation` marcados como obsoletos por los métodos `tryParse` y `tryBuild` de Minecraft 1.20.1.
- Se ha aumentado la versión Gradle a `0.1.1`.

El log previo contiene suficientes datos para corregir estos cuatro errores. Esto no equivale a una compilación confirmada: cualquier nueva incidencia debe revisarse con el nuevo log.
