package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.mojang.serialization.MapCodec;
import com.radaeli.aeronauticsflybyinstruments.block.CableSocketProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/** Fixed 9-ray Terrain Scanner block. */
public final class TerrainScannerBlock extends AbstractTerrainScannerBlock implements CableSocketProvider {
    public static final MapCodec<TerrainScannerBlock> CODEC = simpleCodec(TerrainScannerBlock::new);
    // Source model faces NORTH. Its antenna is recessed by 0.25/16 inside the
    // front rim; the scanner's existing cast origin remains just outside the cell.
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(1, 2, 3, 15, 14, 15),
            Block.box(2, 3, 1, 14, 13, 3),
            Block.box(2, 3, 0.25, 14, 13, 1),
            Block.box(1, 13, 0, 15, 14, 3),
            Block.box(1, 2, 0, 15, 3, 3),
            Block.box(1, 3, 0, 2, 13, 3),
            Block.box(14, 3, 0, 15, 13, 3),
            Block.box(0.5, 14, 0, 15.5, 15, 15.5),
            Block.box(0, 0, 6, 3, 2, 14),
            Block.box(13, 0, 6, 16, 2, 14),
            Block.box(5, 15, 8, 11, 15.25, 13),
            Block.box(5, 5, 15, 11, 11, 15.5),
            Block.box(6, 6, 15.5, 10, 10, 16)).optimize();
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, NORTH_SHAPE,
            Direction.EAST, rotateShape(Direction.EAST),
            Direction.SOUTH, rotateShape(Direction.SOUTH),
            Direction.WEST, rotateShape(Direction.WEST));

    public TerrainScannerBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return housingShape(state);
    }

    // Both variants use exactly the same saved housing geometry and sockets.
    static VoxelShape housingShape(BlockState state) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public Vec3 cableSocket(BlockState state, Direction face) {
        return housingSocket(state, face);
    }

    static Vec3 housingSocket(BlockState state, Direction face) {
        Direction facing = state.getValue(FACING);
        Direction localFace = face;
        if (face.getAxis().isHorizontal()) {
            localFace = switch (facing) {
                case EAST -> face.getCounterClockWise();
                case SOUTH -> face.getOpposite();
                case WEST -> face.getClockWise();
                default -> face;
            };
        }
        Vec3 p = switch (localFace) {
            case NORTH -> new Vec3(8, 8, .25);
            case SOUTH -> new Vec3(8, 8, 16); // Existing rear data socket is flush.
            case WEST -> new Vec3(1, 8, 8);
            case EAST -> new Vec3(15, 8, 8);
            case DOWN -> new Vec3(8, 2, 8);
            case UP -> new Vec3(8, 15, 4); // Lid, clear of the raised emblem.
        };
        return (switch (facing) {
            case EAST -> new Vec3(16 - p.z, p.y, p.x);
            case SOUTH -> new Vec3(16 - p.x, p.y, 16 - p.z);
            case WEST -> new Vec3(p.z, p.y, 16 - p.x);
            default -> p;
        }).scale(1.0 / 16);
    }

    private static VoxelShape rotateShape(Direction facing) {
        return NORTH_SHAPE.toAabbs().stream().map(box -> switch (facing) {
            case EAST -> Shapes.box(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX);
            case SOUTH -> Shapes.box(1 - box.maxX, box.minY, 1 - box.maxZ,
                    1 - box.minX, box.maxY, 1 - box.minZ);
            case WEST -> Shapes.box(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY, 1 - box.minX);
            default -> throw new IllegalArgumentException("Expected a rotated horizontal facing");
        }).reduce(Shapes.empty(), Shapes::or).optimize();
    }
}
