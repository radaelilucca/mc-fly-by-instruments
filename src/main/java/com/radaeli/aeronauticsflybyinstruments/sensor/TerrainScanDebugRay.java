package com.radaeli.aeronauticsflybyinstruments.sensor;

import net.minecraft.world.phys.Vec3;

/** One visualized ray path, captured alongside the debug scan. */
public record TerrainScanDebugRay(int rayIndex, Vec3 start, Vec3 end, CollisionRaycaster.Kind result) {}
