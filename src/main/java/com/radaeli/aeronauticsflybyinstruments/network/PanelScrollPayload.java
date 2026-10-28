package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Wrench scroll over a lamp or buzzer (plan §5.4). The server resolves the module from the hit
 * again, so the client never names a module it may not reach.
 */
public record PanelScrollPayload(BlockPos position, Direction face, Vec3 hit, int direction)
        implements CustomPacketPayload {
    public static final Type<PanelScrollPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "panel_scroll"));
    public static final StreamCodec<ByteBuf, PanelScrollPayload> STREAM_CODEC = StreamCodec.of(
            PanelScrollPayload::encode, PanelScrollPayload::decode);

    public PanelScrollPayload { position = position.immutable(); }

    private static void encode(ByteBuf buf, PanelScrollPayload p) {
        BlockPos.STREAM_CODEC.encode(buf, p.position);
        Direction.STREAM_CODEC.encode(buf, p.face);
        buf.writeDouble(p.hit.x); buf.writeDouble(p.hit.y); buf.writeDouble(p.hit.z);
        ByteBufCodecs.VAR_INT.encode(buf, p.direction);
    }

    private static PanelScrollPayload decode(ByteBuf buf) {
        return new PanelScrollPayload(BlockPos.STREAM_CODEC.decode(buf), Direction.STREAM_CODEC.decode(buf),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), ByteBufCodecs.VAR_INT.decode(buf));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
