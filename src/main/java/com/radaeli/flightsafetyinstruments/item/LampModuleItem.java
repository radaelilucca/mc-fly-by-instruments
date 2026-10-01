package com.radaeli.flightsafetyinstruments.item;

import com.radaeli.flightsafetyinstruments.block.ModularTableBaseBlock;
import com.radaeli.flightsafetyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.flightsafetyinstruments.panel.PanelCoordinates;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public final class LampModuleItem extends Item {
    public LampModuleItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (!(level.getBlockEntity(pos) instanceof ModularTableBlockEntity table)) return InteractionResult.PASS;
        var facing = state.getValue(ModularTableBaseBlock.FACING);
        if (context.getClickedFace() != facing) return InteractionResult.FAIL;
        var point = PanelCoordinates.toPanel(facing, context.getClickLocation().subtract(Vec3.atLowerCornerOf(pos)));
        int column = PanelCoordinates.cell(point.x);
        int row = PanelCoordinates.cell(point.z);
        if (!table.grid().canFit(column, row, 1, 1)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            if (!table.placeLamp(column, row)) return InteractionResult.FAIL;
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
