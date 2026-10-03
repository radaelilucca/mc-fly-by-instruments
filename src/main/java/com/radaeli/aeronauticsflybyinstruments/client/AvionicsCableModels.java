package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import com.radaeli.aeronauticsflybyinstruments.block.AvionicsCableBlock;
import com.radaeli.aeronauticsflybyinstruments.block.CableConnection;
import com.radaeli.aeronauticsflybyinstruments.block.CableSocketProvider;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Chunk-baked leads: no block entity, per-frame renderer, ticker or network payload. */
public final class AvionicsCableModels {
    private static final ModelResourceLocation CORE = model("avionics_cable_core");
    private static final ModelResourceLocation FLEX = model("avionics_cable_flex");
    private static final ModelResourceLocation TERMINAL = model("avionics_cable_terminal");
    private static final ModelResourceLocation ARM = model("avionics_cable_south");
    private static final ModelProperty<List<BakedQuad>> QUADS = new ModelProperty<>();

    private AvionicsCableModels() {}

    public static void register(IEventBus bus) {
        bus.addListener(AvionicsCableModels::registerAdditional);
        bus.addListener(AvionicsCableModels::wrapModels);
    }

    private static ModelResourceLocation model(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
                AeronauticsFlyByInstruments.MOD_ID, "block/" + name));
    }

    private static void registerAdditional(ModelEvent.RegisterAdditional event) {
        for (var id : List.of(CORE, ARM, FLEX, TERMINAL)) event.register(id);
    }

    private static List<BakedQuad> quads(BakedModel model) {
        return List.copyOf(model.getQuads(null, null, RandomSource.create(0), ModelData.EMPTY, null));
    }

    private static void wrapModels(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        var core = quads(models.get(CORE));
        var arm = quads(models.get(ARM));
        var flex = quads(models.get(FLEX));
        var terminal = quads(models.get(TERMINAL));
        var cableId = ModBlocks.AVIONICS_CABLE.getId();
        models.replaceAll((id, baked) -> id.id().equals(cableId) && !id.getVariant().equals("inventory")
                ? new CableModel(baked, core, arm, flex, terminal) : baked);
    }

    private static final class CableModel extends BakedModelWrapper<BakedModel> {
        private final List<BakedQuad> core, arm, flex, terminal;

        private CableModel(BakedModel original, List<BakedQuad> core, List<BakedQuad> arm,
                           List<BakedQuad> flex, List<BakedQuad> terminal) {
            super(original);
            this.core = core;
            this.arm = arm;
            this.flex = flex;
            this.terminal = terminal;
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            var result = new ArrayList<>(core);
            for (Direction direction : Direction.values()) {
                if (!state.getValue(AvionicsCableBlock.CONNECTIONS.get(direction))) continue;
                BlockState neighbor = level.getBlockState(pos.relative(direction));
                if (neighbor.getBlock() instanceof CableSocketProvider sockets) {
                    var connection = new CableConnection(direction, sockets.cableSocket(neighbor, direction.getOpposite()));
                    append(result, flex, connection, false);
                    append(result, terminal, connection, true);
                } else {
                    // Other endpoints and cable-to-cable joins retain the standard straight arm.
                    // Device-local face center is (0.5,0.5,0.5) - direction * 0.5.
                    var connection = new CableConnection(direction, new Vec3(.5 - .5 * direction.getStepX(),
                            .5 - .5 * direction.getStepY(), .5 - .5 * direction.getStepZ()));
                    append(result, arm, connection, true);
                }
            }
            return data.derive().with(QUADS, List.copyOf(result)).build();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                        ModelData data, @Nullable RenderType renderType) {
            var quads = data.get(QUADS);
            if (quads == null) return super.getQuads(state, side, random, data, renderType);
            return side == null ? quads : List.of();
        }

        private static void append(List<BakedQuad> output, List<BakedQuad> template,
                                   CableConnection connection, boolean terminal) {
            for (var quad : template) {
                int[] vertices = quad.getVertices().clone();
                for (int i = 0; i < 4; i++) {
                    int o = i * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
                    var p = connection.transform(new Vec3(Float.intBitsToFloat(vertices[o]),
                            Float.intBitsToFloat(vertices[o + 1]), Float.intBitsToFloat(vertices[o + 2])), terminal);
                    vertices[o] = Float.floatToRawIntBits((float) p.x);
                    vertices[o + 1] = Float.floatToRawIntBits((float) p.y);
                    vertices[o + 2] = Float.floatToRawIntBits((float) p.z);
                }
                // Recompute both normals and cardinal lighting face after rotating/shearing the lead.
                var a = vertex(vertices, 0).sub(vertex(vertices, 1));
                var b = vertex(vertices, 2).sub(vertex(vertices, 1));
                var normal = b.cross(a).normalize();
                int packed = ((byte) (normal.x * 127) & 255) | (((byte) (normal.y * 127) & 255) << 8)
                        | (((byte) (normal.z * 127) & 255) << 16);
                for (int i = 0; i < 4; i++) {
                    int o = i * IQuadTransformer.STRIDE + IQuadTransformer.NORMAL;
                    vertices[o] = (vertices[o] & 0xff000000) | packed;
                }
                output.add(new BakedQuad(vertices, quad.getTintIndex(), FaceBakery.calculateFacing(vertices),
                        quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
            }
        }

        private static Vector3f vertex(int[] data, int vertex) {
            int o = vertex * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
            return new Vector3f(Float.intBitsToFloat(data[o]), Float.intBitsToFloat(data[o + 1]),
                    Float.intBitsToFloat(data[o + 2]));
        }
    }
}
