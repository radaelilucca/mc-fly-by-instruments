package com.radaeli.aeronauticsflybyinstruments.item;

import com.radaeli.aeronauticsflybyinstruments.block.ModularTableBaseBlock;
import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.panel.PanelCoordinates;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public final class LampModuleItem extends Item {
    public LampModuleItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return place(context, "lamp", 1, 1);
    }

    static InteractionResult place(UseOnContext context, String type, int width, int height) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (!(level.getBlockEntity(pos) instanceof ModularTableBlockEntity table)) return InteractionResult.PASS;
        var facing = state.getValue(ModularTableBaseBlock.FACING);
        if (context.getClickedFace() != facing) return InteractionResult.FAIL;
        var point = PanelCoordinates.toPanel(facing, context.getClickLocation().subtract(Vec3.atLowerCornerOf(pos)));
        int column = PanelCoordinates.cell(point.x);
        int row = PanelCoordinates.cell(point.z);
        if (!table.grid().canFit(column, row, width, height)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            boolean placed = type.equals("lamp") ? table.placeLamp(column, row)
                    : table.placeLamp2x1(column, row, height > width);
            if (!placed) return InteractionResult.FAIL;
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
