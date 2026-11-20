package dev.ollasland.cinematics.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ollasland.cinematics.data.SceneStore;
import dev.ollasland.cinematics.network.CinematicNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

public final class CinematicCommands {
    private CinematicCommands() {}
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cine")
            .requires(src->src.hasPermission(2))
            .then(Commands.literal("editor").executes(ctx->{
                ServerPlayer player=ctx.getSource().getPlayerOrException();
                CinematicNetwork.editor(player);return 1;
            }))
            .then(Commands.literal("start")
                .then(Commands.argument("scene",StringArgumentType.word())
                    .then(Commands.argument("targets",EntityArgument.players())
                        .executes(ctx->{
                            try {
                                var d=SceneStore.load(StringArgumentType.getString(ctx,"scene"));
                                var players=EntityArgument.getPlayers(ctx,"targets");
                                for(ServerPlayer p:players) CinematicNetwork.start(p,d);
                                ctx.getSource().sendSuccess(()->Component.literal("Cinematica "+d.id+" para "+players.size()+" jugador(es)"),true);
                                return players.size();
                            } catch(java.io.IOException | IllegalArgumentException ex) {
                                ctx.getSource().sendFailure(Component.literal("No se pudo iniciar la escena: "+ex.getMessage()));
                                return 0;
                            }
                        }))))
            .then(Commands.literal("preview")
                .then(Commands.argument("scene",StringArgumentType.word())
                    .executes(ctx->{
                        try {
                            ServerPlayer p=ctx.getSource().getPlayerOrException();
                            CinematicNetwork.preview(p,SceneStore.load(StringArgumentType.getString(ctx,"scene")));
                            return 1;
                        } catch(java.io.IOException | IllegalArgumentException ex) {
                            ctx.getSource().sendFailure(Component.literal("Error de preview: "+ex.getMessage()));
                            return 0;
                        }
                    })))
            .then(Commands.literal("stop").then(Commands.argument("targets",EntityArgument.players())
                .executes(ctx->{var players=EntityArgument.getPlayers(ctx,"targets");
                    for(ServerPlayer p:players) CinematicNetwork.stop(p);return players.size();}))));
    }
}
