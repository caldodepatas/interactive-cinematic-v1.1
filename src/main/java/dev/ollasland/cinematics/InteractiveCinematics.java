package dev.ollasland.cinematics;

import dev.ollasland.cinematics.command.CinematicCommands;
import dev.ollasland.cinematics.network.CinematicNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

@Mod(InteractiveCinematics.MODID)
public class InteractiveCinematics {
    public static final String MODID="interactivecinematics";
    public InteractiveCinematics() {
        CinematicNetwork.init();
        MinecraftForge.EVENT_BUS.addListener(CinematicCommands::register);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) ->
            CinematicNetwork.clear(event.getEntity().getUUID()));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> dev.ollasland.cinematics.client.ClientEntrypoint.initialize());
    }
}
