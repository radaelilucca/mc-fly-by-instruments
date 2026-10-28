package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.avionics.AlertChannel;
import com.radaeli.aeronauticsflybyinstruments.avionics.AlertReading;
import com.radaeli.aeronauticsflybyinstruments.avionics.AvionicsComputerBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.avionics.AvionicsComputerStatus;
import com.radaeli.aeronauticsflybyinstruments.avionics.AvionicsSensor;
import com.radaeli.aeronauticsflybyinstruments.avionics.GpwsSensitivity;
import com.radaeli.aeronauticsflybyinstruments.panel.PanelSpatial;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Server-validated control and live screen synchronization for the Avionics Computer. */
public final class AvionicsComputerNetwork {
    private AvionicsComputerNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(AvionicsComputerControlPayload.TYPE, AvionicsComputerControlPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    BlockPos pos = payload.position();
                    if (!player.level().hasChunkAt(pos)) return;
                    if (!(player.level().getBlockEntity(pos) instanceof AvionicsComputerBlockEntity computer)
                            || !PanelSpatial.canReach(player, computer)) return;
                    apply(computer, payload.action(), payload.value());
                    open(player, pos, computer);
                }));
        registrar.playToClient(AvionicsComputerScreenPayload.TYPE, AvionicsComputerScreenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        com.radaeli.aeronauticsflybyinstruments.client.AvionicsComputerClient.openOrUpdate(payload);
                    }
                }));
    }

    public static void open(ServerPlayer player, BlockPos position, AvionicsComputerBlockEntity computer) {
        PacketDistributor.sendToPlayer(player, snapshot(position, computer));
    }

    static AvionicsComputerScreenPayload snapshot(BlockPos position, AvionicsComputerBlockEntity computer) {
        AvionicsComputerStatus status = computer.status();
        var evaluation = computer.evaluation();
        List<AvionicsComputerScreenPayload.SensorLine> sensors = new ArrayList<>();
        for (AvionicsSensor sensor : computer.sensors()) {
            sensors.add(new AvionicsComputerScreenPayload.SensorLine(sensor.type(), sensor.role(), sensor.status(),
                    sensor.value().orElse(Double.NaN), sensor.issue()));
        }
        List<AvionicsComputerScreenPayload.FunctionLine> functions = new ArrayList<>();
        for (AlertChannel channel : AlertChannel.values()) {
            AlertReading reading = evaluation.alerts().get(channel);
            if (reading == null) continue;
            functions.add(new AvionicsComputerScreenPayload.FunctionLine(channel.id(), lower(reading.state()),
                    lower(reading.severity()), evaluation.reasons().getOrDefault(channel, "")));
        }
        return new AvionicsComputerScreenPayload(position.immutable(), status.level().id(), status.channelId(),
                lower(computer.configuration().sensitivity()), lower(evaluation.phase()),
                lower(computer.latestSample().networkStatus()), sensors, functions);
    }

    private static void apply(AvionicsComputerBlockEntity computer, String action, String value) {
        if (action == null || value == null) return;
        switch (action) {
            case "test" -> computer.startTest();
            case "sensitivity" -> {
                GpwsSensitivity[] values = GpwsSensitivity.values();
                computer.setSensitivity(values[(computer.configuration().sensitivity().ordinal() + 1) % values.length]);
            }
            case "inhibit" -> {
                for (AlertChannel channel : AlertChannel.values()) {
                    if (channel.id().equals(value)) {
                        computer.setInhibited(channel, !computer.configuration().inhibited(channel));
                    }
                }
            }
            default -> { } // "refresh" only resends the snapshot.
        }
    }

    private static String lower(Enum<?> value) { return value.name().toLowerCase(Locale.ROOT); }
}
