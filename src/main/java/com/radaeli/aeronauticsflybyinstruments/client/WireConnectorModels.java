package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.wire.WireNodeBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Seats a Wire Connector on its device's cable socket (same points as the legacy cable), at chunk bake time. */
final class WireConnectorModels {
    private static final ModelProperty<Vec3> OFFSET = new ModelProperty<>();

    private WireConnectorModels() {}

    static void wrapModels(ModelEvent.ModifyBakingResult event) {
        var connectorId = ModBlocks.WIRE_CONNECTOR.getId();
        event.getModels().replaceAll((id, baked) -> id.id().equals(connectorId) && !id.getVariant().equals("inventory")
                ? new SocketModel(baked) : baked);
    }

    private static final class SocketModel extends BakedModelWrapper<BakedModel> {
        private SocketModel(BakedModel original) {
            super(original);
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            Vec3 offset = WireNodeBlock.socketOffset(level, pos, state);
            return offset.lengthSqr() < 1e-8 ? data : data.derive().with(OFFSET, offset).build();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                        ModelData data, @Nullable RenderType renderType) {
            Vec3 offset = data.get(OFFSET);
            if (offset == null) return super.getQuads(state, side, random, data, renderType);
            // Shifted quads no longer sit on the block boundary, so none may be face-culled.
            if (side != null) return List.of();
            var result = new ArrayList<BakedQuad>();
            for (Direction face : Direction.values()) {
                for (var quad : super.getQuads(state, face, random, data, renderType)) result.add(shift(quad, offset));
            }
            for (var quad : super.getQuads(state, null, random, data, renderType)) result.add(shift(quad, offset));
            return result;
        }

        private static BakedQuad shift(BakedQuad quad, Vec3 offset) {
            int[] vertices = quad.getVertices().clone();
            for (int i = 0; i < 4; i++) {
                int o = i * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
                vertices[o] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[o]) + (float) offset.x);
                vertices[o + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[o + 1]) + (float) offset.y);
                vertices[o + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[o + 2]) + (float) offset.z);
            }
            return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(),
                    quad.hasAmbientOcclusion());
        }
    }
}
