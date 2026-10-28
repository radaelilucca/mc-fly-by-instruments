package com.radaeli.aeronauticsflybyinstruments.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Level coordinates (plot coordinates inside a Sable sublevel) to where they are drawn this frame. */
final class WireSpace {
    private WireSpace() {}

    @Nullable
    static ClientSubLevel containing(BlockPos pos) {
        return Sable.HELPER.getContainingClient((net.minecraft.core.Vec3i) pos);
    }

    static Vec3 toWorld(@Nullable ClientSubLevel subLevel, Vec3 point, float partialTick) {
        return subLevel == null ? point : subLevel.renderPose(partialTick).transformPosition(point);
    }
}
