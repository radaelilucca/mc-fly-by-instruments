package com.radaeli.aeronauticsflybyinstruments.fms;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class FmsLocation {
    private FmsLocation() {}

    public static Vec3 forEntity(Entity entity) {
        var containing = Sable.HELPER.getContaining(entity);
        return containing instanceof SubLevel subLevel
                ? subLevel.logicalPose().transformPosition(entity.position()) : entity.position();
    }

    public static Vec3 forBlockEntity(BlockEntity blockEntity) {
        Vec3 local = Vec3.atCenterOf(blockEntity.getBlockPos());
        if (blockEntity.getLevel() == null) return local;
        var containing = Sable.HELPER.getContaining(blockEntity);
        return containing instanceof SubLevel subLevel
                ? subLevel.logicalPose().transformPosition(local) : local;
    }

    public static SubLevel subLevel(BlockEntity blockEntity) {
        var containing = Sable.HELPER.getContaining(blockEntity);
        return containing instanceof SubLevel subLevel ? subLevel : null;
    }

    public static Vec3 blockCenter(BlockPos position) { return Vec3.atCenterOf(position); }
}
