package com.radaeli.aeronauticsflybyinstruments.block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Visual attachment only: every face still joins the same avionics network. */
public interface CableSocketProvider {
    /** Device-local point in block units; face points from the device toward the cable. */
    Vec3 cableSocket(BlockState state, Direction face);
}
