package com.radaeli.aeronauticsflybyinstruments.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Connected-texture rules of the modular table top (compact CTM, five tiles): each quarter of the
 * top picks a tile from its two edge neighbours and the diagonal between them. Tables join only
 * with tables mounted on the same face (same {@code FACING}).
 */
public final class TableConnections {
    /** Top tiles; the sprite is {@code block/modular_table_top<suffix>}. */
    public enum Tile {
        FRAME(""), HORIZONTAL("_horizontal"), VERTICAL("_vertical"), INNER("_inner"), WOOD("_wood");

        public final String suffix;

        Tile(String suffix) {
            this.suffix = suffix;
        }
    }

    private TableConnections() {}

    /**
     * @param side     a table continues past the quarter's edge along the texture U axis
     * @param vertical a table continues past the quarter's edge along the texture V axis
     * @param diagonal a table sits on the diagonal between those two
     */
    public static Tile tile(boolean side, boolean vertical, boolean diagonal) {
        if (!side) return vertical ? Tile.VERTICAL : Tile.FRAME;
        if (!vertical) return Tile.HORIZONTAL;
        return diagonal ? Tile.WOOD : Tile.INNER;
    }

    public static boolean joins(BlockState table, BlockState neighbor) {
        return neighbor.getBlock() instanceof ModularTableBaseBlock
                && neighbor.getValue(ModularTableBaseBlock.FACING) == table.getValue(ModularTableBaseBlock.FACING);
    }

    /** Bit of an offset in {@link #mask}: (dx + 1) * 9 + (dy + 1) * 3 + (dz + 1). */
    public static int bit(int dx, int dy, int dz) {
        return 1 << ((dx + 1) * 9 + (dy + 1) * 3 + (dz + 1));
    }

    /** Joined neighbours in the plane of the table (the eight around it), as {@link #bit} flags. */
    public static int mask(BlockGetter level, BlockPos pos, BlockState table) {
        var axis = table.getValue(ModularTableBaseBlock.FACING).getAxis();
        int mask = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
            int along = switch (axis) { case X -> dx; case Y -> dy; case Z -> dz; };
            if (along != 0 || (dx == 0 && dy == 0 && dz == 0)) continue;
            if (joins(table, level.getBlockState(pos.offset(dx, dy, dz)))) mask |= bit(dx, dy, dz);
        }
        return mask;
    }

    public static boolean joined(int mask, int dx, int dy, int dz) {
        return (mask & bit(dx, dy, dz)) != 0;
    }

    public static boolean joined(int mask, Direction direction) {
        return joined(mask, direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }
}
