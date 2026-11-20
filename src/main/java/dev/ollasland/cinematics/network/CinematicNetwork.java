package dev.ollasland.cinematics.network;

import dev.ollasland.cinematics.InteractiveCinematics;
import dev.ollasland.cinematics.data.SceneData;
import dev.ollasland.cinematics.data.SceneStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class CinematicNetwork {
    private static final String PROTOCOL="1";
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(
        ResourceLocation.tryBuild(InteractiveCinematics.MODID,"main"),()->PROTOCOL,PROTOCOL::equals,PROTOCOL::equals);
    private static final Map<UUID, UUID> ACTIVE=new ConcurrentHashMap<>();
    private static int index;
    private CinematicNetwork() {}
    public static void init() {
        CHANNEL.registerMessage(index++, Start.class, (m,b)->{b.writeUtf(m.json,65535);b.writeUUID(m.session);},
            b->new Start(b.readUtf(65535),b.readUUID()),Start::handle);
        CHANNEL.registerMessage(index++, Editor.class,(m,b)->{},b->new Editor(),Editor::handle);
        CHANNEL.registerMessage(index++, Stop.class,(m,b)->{},b->new Stop(),Stop::handle);
        CHANNEL.registerMessage(index++, Result.class,(m,b)->{b.writeUUID(m.session);b.writeUtf(m.result,128);},
            b->new Result(b.readUUID(),b.readUtf(128)),Result::handle);
        CHANNEL.registerMessage(index++, Upload.class,(m,b)->b.writeUtf(m.json,65535),
            b->new Upload(b.readUtf(65535)),Upload::handle);
    }
    public static void clear(UUID playerId) {ACTIVE.remove(playerId);}
    public static void start(ServerPlayer player,SceneData data) {
        UUID session=UUID.randomUUID(); ACTIVE.put(player.getUUID(),session);
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Start(data.toJson(),session));
    }
    public static void preview(ServerPlayer player,SceneData data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Start(data.toJson(),new UUID(0,0)));
    }
    public static void editor(ServerPlayer player) {CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Editor());}
    public static void stop(ServerPlayer player) {ACTIVE.remove(player.getUUID());CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Stop());}
    public record Start(String json,UUID session) {
        private static void handle(Start m,Supplier<NetworkEvent.Context> supplied) {
            NetworkEvent.Context ctx=supplied.get();
            ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->
                dev.ollasland.cinematics.client.ClientEntrypoint.start(m.json,m.session)));
            ctx.setPacketHandled(true);
        }
    }
    public record Editor() {
        private static void handle(Editor m,Supplier<NetworkEvent.Context> supplied) {
            NetworkEvent.Context ctx=supplied.get();
            ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->
                dev.ollasland.cinematics.client.ClientEntrypoint.openEditor()));ctx.setPacketHandled(true);
        }
    }
    public record Stop() {
        private static void handle(Stop m,Supplier<NetworkEvent.Context> supplied) {
            NetworkEvent.Context ctx=supplied.get();
            ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->
                dev.ollasland.cinematics.client.ClientEntrypoint.stop()));ctx.setPacketHandled(true);
        }
    }
    public record Result(UUID session,String result) {
        private static void handle(Result m,Supplier<NetworkEvent.Context> supplied) {
            NetworkEvent.Context ctx=supplied.get();
            ctx.enqueueWork(()->{
                ServerPlayer p=ctx.getSender();
                if(p==null) return;
                UUID expected=ACTIVE.get(p.getUUID());
                if(expected!=null && expected.equals(m.session)) {
                    ACTIVE.remove(p.getUUID());
                    p.sendSystemMessage(Component.literal("Cinematica terminada: "+m.result));
                    // Never execute commands or give rewards based on unchecked client data.
                }
            });ctx.setPacketHandled(true);
        }
    }
    public record Upload(String json) {
        private static void handle(Upload m,Supplier<NetworkEvent.Context> supplied) {
            NetworkEvent.Context ctx=supplied.get();
            ctx.enqueueWork(()->{
                ServerPlayer p=ctx.getSender();
                if(p==null || !p.hasPermissions(2)) return;
                try {
                    SceneData scene=SceneData.fromJson(m.json);
                    SceneStore.save(scene);
                    p.sendSystemMessage(Component.literal("Escena guardada en el servidor: "+scene.id));
                } catch(Exception e) {p.sendSystemMessage(Component.literal("Error de escena: "+e.getMessage()));}
            });ctx.setPacketHandled(true);
        }
    }
}
