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
    // v2 model (docs/visual-design-guide.md), source faces NORTH: andesite body plus a stepped
    // nozzle whose lens front centre (8, 8, 0) faces the cast origin just outside the cell.
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(0, 0, 4, 16, 16, 16),
            Block.box(3, 3, 2, 13, 13, 4),
            Block.box(4, 4, 1, 12, 12, 2),
            Block.box(5, 5, 0, 11, 11, 1)).optimize();
    private static final Map<Direction, VoxelShape> SHAPES = shapes(NORTH_SHAPE);

    public TerrainScannerBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return housingShape(state, SHAPES);
    }

    /** Rotations of a NORTH-facing source shape for the four horizontal facings. */
    static Map<Direction, VoxelShape> shapes(VoxelShape north) {
        return Map.of(Direction.NORTH, north,
                Direction.EAST, rotateShape(north, Direction.EAST),
                Direction.SOUTH, rotateShape(north, Direction.SOUTH),
                Direction.WEST, rotateShape(north, Direction.WEST));
    }

    // The variants differ in their front optics but share the body volume and sockets.
    static VoxelShape housingShape(BlockState state, Map<Direction, VoxelShape> shapes) {
        VoxelShape shape = shapes.get(state.getValue(FACING));
        return state.getValue(UPSIDE_DOWN) ? invertAroundFacing(shape, state.getValue(FACING)) : shape;
    }

    @Override
    public Vec3 cableSocket(BlockState state, Direction face) {
        return housingSocket(state, face);
    }

    static Vec3 housingSocket(BlockState state, Direction face) {
        Direction facing = state.getValue(FACING);
        Direction socketFace = state.getValue(UPSIDE_DOWN) ? invertFaceAroundFacing(face, facing) : face;
        Direction localFace = toLocalFace(socketFace, facing);
        Vec3 p = switch (localFace) {
            case NORTH -> new Vec3(8, 8, 0);   // Lens front centre (both variants).
            case SOUTH -> new Vec3(8, 8, 16);  // Rear data socket, flush.
            case WEST -> new Vec3(0, 8, 10);   // Body sides span z 4..16.
            case EAST -> new Vec3(16, 8, 10);
            case DOWN -> new Vec3(8, 0, 10);
            case UP -> new Vec3(8, 16, 10);
        };
        p = transformPoint(p, facing);
        if (state.getValue(UPSIDE_DOWN)) p = invertAroundFacing(p, facing);
        return p.scale(1.0 / 16);
    }

    private static Direction invertFaceAroundFacing(Direction face, Direction facing) {
        if (face.getAxis().isVertical()) return face.getOpposite();
        if (facing.getAxis() == Direction.Axis.Z && face.getAxis() == Direction.Axis.X
                || facing.getAxis() == Direction.Axis.X && face.getAxis() == Direction.Axis.Z) {
            return face.getOpposite();
        }
        return face;
    }

    private static Direction toLocalFace(Direction worldFace, Direction facing) {
        return switch (facing) {
            case EAST -> worldFace.getAxis().isHorizontal() ? worldFace.getCounterClockWise() : worldFace;
            case SOUTH -> worldFace.getAxis().isHorizontal() ? worldFace.getOpposite() : worldFace;
            case WEST -> worldFace.getAxis().isHorizontal() ? worldFace.getClockWise() : worldFace;
            default -> worldFace;
        };
    }

    private static Vec3 transformPoint(Vec3 p, Direction facing) {
        return switch (facing) {
            case EAST -> new Vec3(16 - p.z, p.y, p.x);
            case SOUTH -> new Vec3(16 - p.x, p.y, 16 - p.z);
            case WEST -> new Vec3(p.z, p.y, 16 - p.x);
            default -> p;
        };
    }

    private static Vec3 invertAroundFacing(Vec3 point, Direction facing) {
        return switch (facing) {
            case NORTH, SOUTH -> new Vec3(16 - point.x, 16 - point.y, point.z);
            case EAST, WEST -> new Vec3(point.x, 16 - point.y, 16 - point.z);
            default -> point;
        };
    }

    private static VoxelShape invertAroundFacing(VoxelShape shape, Direction facing) {
        return shape.toAabbs().stream().map(box -> switch (facing) {
            case NORTH, SOUTH -> Shapes.box(1 - box.maxX, 1 - box.maxY, box.minZ,
                    1 - box.minX, 1 - box.minY, box.maxZ);
            case EAST, WEST -> Shapes.box(box.minX, 1 - box.maxY, 1 - box.maxZ,
                    box.maxX, 1 - box.minY, 1 - box.minZ);
            default -> throw new IllegalArgumentException("Expected a horizontal scanner facing");
        }).reduce(Shapes.empty(), Shapes::or).optimize();
    }

    private static VoxelShape rotateShape(VoxelShape north, Direction facing) {
        return north.toAabbs().stream().map(box -> switch (facing) {
            case EAST -> Shapes.box(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX);
            case SOUTH -> Shapes.box(1 - box.maxX, box.minY, 1 - box.maxZ,
                    1 - box.minX, box.maxY, 1 - box.minZ);
            case WEST -> Shapes.box(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY, 1 - box.minX);
            default -> throw new IllegalArgumentException("Expected a rotated horizontal facing");
        }).reduce(Shapes.empty(), Shapes::or).optimize();
    }
}
