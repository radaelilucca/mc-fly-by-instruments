package com.radaeli.aeronauticsflybyinstruments.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/** Uses Create's standard wrench pickup on sneak-right-click without adding rotation behavior. */
public interface AvionicsWrenchable extends IWrenchable {
    @Override
    default BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        return state;
    }

    @Override
    default InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }
}
