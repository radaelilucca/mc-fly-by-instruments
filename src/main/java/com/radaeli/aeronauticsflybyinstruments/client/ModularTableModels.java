package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.block.TableConnections;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Connected textures of the table base, baked into the chunk mesh: the top quad is split into
 * quarters that pick a tile from {@link TableConnections}, and sides facing a joined table are
 * dropped. The item model and unjoined tables keep the plain framed top.
 */
public final class ModularTableModels {
    private static final ResourceLocation TOP = texture("modular_table_top");
    private static final ResourceLocation SIDE = texture("modular_table_side");
    private static final ModelProperty<Integer> MASK = new ModelProperty<>();

    private ModularTableModels() {}

    public static void register(IEventBus bus) {
        bus.addListener(ModularTableModels::wrapModels);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AeronauticsFlyByInstruments.MOD_ID, "block/" + name);
    }

    private static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<TableConnections.Tile, TextureAtlasSprite> tiles = new EnumMap<>(TableConnections.Tile.class);
        for (var tile : TableConnections.Tile.values()) {
            tiles.put(tile, event.getTextureGetter().apply(new Material(InventoryMenu.BLOCK_ATLAS,
                    texture("modular_table_top" + tile.suffix))));
        }
        var tableId = ModBlocks.MODULAR_TABLE_BASE.getId();
        event.getModels().replaceAll((id, baked) -> id.id().equals(tableId) && !id.getVariant().equals("inventory")
                ? new TableModel(baked, tiles) : baked);
    }

    private static final class TableModel extends BakedModelWrapper<BakedModel> {
        private final Map<TableConnections.Tile, TextureAtlasSprite> tiles;

        private TableModel(BakedModel original, Map<TableConnections.Tile, TextureAtlasSprite> tiles) {
            super(original);
            this.tiles = tiles;
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            return data.derive().with(MASK, TableConnections.mask(level, pos, state)).build();
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                        ModelData data, @Nullable RenderType renderType) {
            var quads = super.getQuads(state, side, random, data, renderType);
            Integer mask = data.get(MASK);
            if (mask == null || mask == 0) return quads;
            var result = new ArrayList<BakedQuad>(quads.size() + 3);
            for (var quad : quads) {
                var sprite = quad.getSprite().contents().name();
                if (sprite.equals(SIDE) && TableConnections.joined(mask, quad.getDirection())) continue;
                if (sprite.equals(TOP)) split(quad, mask, result);
                else result.add(quad);
            }
            return result;
        }

        /** Replaces the top quad by four quarters, each mapped onto the same quarter of its tile. */
        private void split(BakedQuad quad, int mask, List<BakedQuad> output) {
            var sprite = quad.getSprite();
            int[] data = quad.getVertices();
            int[] s = new int[4], t = new int[4];
            float[][][] corner = new float[2][2][];
            for (int i = 0; i < 4; i++) {
                int o = i * IQuadTransformer.STRIDE;
                s[i] = Math.round((Float.intBitsToFloat(data[o + IQuadTransformer.UV0]) - sprite.getU0())
                        / (sprite.getU1() - sprite.getU0()));
                t[i] = Math.round((Float.intBitsToFloat(data[o + IQuadTransformer.UV0 + 1]) - sprite.getV0())
                        / (sprite.getV1() - sprite.getV0()));
                corner[s[i]][t[i]] = new float[]{Float.intBitsToFloat(data[o + IQuadTransformer.POSITION]),
                        Float.intBitsToFloat(data[o + IQuadTransformer.POSITION + 1]),
                        Float.intBitsToFloat(data[o + IQuadTransformer.POSITION + 2])};
            }
            // World directions of the texture axes, whatever rotation the blockstate applied.
            var u = step(corner[0][0], corner[1][0]);
            var v = step(corner[0][0], corner[0][1]);
            var picked = new TableConnections.Tile[2][2];
            boolean plain = true;
            for (int qs = 0; qs < 2; qs++) for (int qt = 0; qt < 2; qt++) {
                int su = qs * 2 - 1, sv = qt * 2 - 1;
                var a = u.getNormal().multiply(su);
                var b = v.getNormal().multiply(sv);
                var c = a.offset(b);
                picked[qs][qt] = TableConnections.tile(TableConnections.joined(mask, a.getX(), a.getY(), a.getZ()),
                        TableConnections.joined(mask, b.getX(), b.getY(), b.getZ()),
                        TableConnections.joined(mask, c.getX(), c.getY(), c.getZ()));
                plain &= picked[qs][qt] == TableConnections.Tile.FRAME;
            }
            if (plain) {
                output.add(quad);
                return;
            }
            for (int qs = 0; qs < 2; qs++) for (int qt = 0; qt < 2; qt++) {
                var tile = tiles.get(picked[qs][qt]);
                int[] vertices = data.clone();
                for (int i = 0; i < 4; i++) {
                    int o = i * IQuadTransformer.STRIDE;
                    float fs = (qs + s[i]) / 2f, ft = (qt + t[i]) / 2f;
                    for (int axis = 0; axis < 3; axis++) {
                        float top = corner[0][0][axis] + (corner[1][0][axis] - corner[0][0][axis]) * fs;
                        float bottom = corner[0][1][axis] + (corner[1][1][axis] - corner[0][1][axis]) * fs;
                        vertices[o + IQuadTransformer.POSITION + axis] = Float.floatToRawIntBits(top + (bottom - top) * ft);
                    }
                    vertices[o + IQuadTransformer.UV0] = Float.floatToRawIntBits(
                            tile.getU0() + (tile.getU1() - tile.getU0()) * fs);
                    vertices[o + IQuadTransformer.UV0 + 1] = Float.floatToRawIntBits(
                            tile.getV0() + (tile.getV1() - tile.getV0()) * ft);
                }
                output.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), tile,
                        quad.isShade(), quad.hasAmbientOcclusion()));
            }
        }

        private static Direction step(float[] from, float[] to) {
            return Direction.getNearest(to[0] - from[0], to[1] - from[1], to[2] - from[2]);
        }
    }
}
