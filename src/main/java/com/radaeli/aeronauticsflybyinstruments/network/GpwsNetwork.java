package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.avionics.GpwsComputerBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.avionics.GpwsMode;
import com.radaeli.aeronauticsflybyinstruments.avionics.GpwsProfile;
import com.radaeli.aeronauticsflybyinstruments.avionics.GpwsSensitivity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Locale;

/** Server-validated control and screen synchronization for the physical GPWS block. */
public final class GpwsNetwork {
    private GpwsNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(GpwsControlPayload.TYPE, GpwsControlPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    BlockPos pos = payload.position();
                    if (!player.level().hasChunkAt(pos)
                            || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0) return;
                    if (!(player.level().getBlockEntity(pos) instanceof GpwsComputerBlockEntity computer)) return;
                    apply(computer, payload.action(), payload.value());
                    sendScreen(player, pos, computer);
                }));
        registrar.playToClient(GpwsScreenPayload.TYPE, GpwsScreenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        com.radaeli.aeronauticsflybyinstruments.client.GpwsClient.openOrUpdate(payload);
                    }
                }));
    }

    public static void openConfiguration(ServerPlayer player, BlockPos position,
                                         GpwsComputerBlockEntity computer) {
        sendScreen(player, position, computer);
    }

    private static void sendScreen(ServerPlayer player, BlockPos position, GpwsComputerBlockEntity computer) {
        var evaluation = computer.evaluation();
        PacketDistributor.sendToPlayer(player, new GpwsScreenPayload(position.immutable(),
                lower(computer.configuration().profile()), lower(computer.configuration().sensitivity()),
                lower(computer.mode()), lower(evaluation.phase()), lower(computer.latestSample().networkStatus())));
    }

    private static void apply(GpwsComputerBlockEntity computer, String action, String value) {
        if (action == null || value == null) return;
        switch (action) {
            case "profile" -> parse(GpwsProfile.class, value).ifPresent(computer::setProfile);
            case "sensitivity" -> parse(GpwsSensitivity.class, value).ifPresent(computer::setSensitivity);
            case "mode" -> parse(GpwsMode.class, value).ifPresent(computer::setMode);
            case "enable" -> computer.setMode(GpwsMode.STANDBY);
            case "disable" -> computer.setMode(GpwsMode.OFF);
            default -> { }
        }
    }

    private static <E extends Enum<E>> java.util.Optional<E> parse(Class<E> type, String value) {
        try {
            return java.util.Optional.of(Enum.valueOf(type, value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private static String lower(Enum<?> value) { return value.name().toLowerCase(Locale.ROOT); }
}
