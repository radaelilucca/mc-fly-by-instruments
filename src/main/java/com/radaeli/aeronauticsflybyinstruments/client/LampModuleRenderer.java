package com.radaeli.aeronauticsflybyinstruments.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;

/** Reuses the authored models, keeping the opaque housing out of the emissive pass. */
final class LampModuleRenderer {
    static final RenderType EMISSIVE = RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS, false);
    private static final ResourceLocation LENS = texture("lamp_lens_red");
    private static final ResourceLocation EMITTER_ON = texture("lamp_emitter_on");
    private static final ResourceLocation EMITTER_OFF = texture("lamp_emitter_off");

    private final ItemStack offStack;
    private final ItemStack onStack;
    private Layers offLayers;
    private Layers onLayers;

    LampModuleRenderer(Item item) {
        offStack = new ItemStack(item);
        onStack = new ItemStack(item);
        onStack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
    }

    void render(int intensity, Level level, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        boolean lit = intensity > 0;
        var renderer = Minecraft.getInstance().getItemRenderer();
        var stack = lit ? onStack : offStack;
        var model = renderer.getModel(stack, level, null, 0);
        var layers = lit ? onLayers : offLayers;
        // Model identity changes on resource reload: never retain quads from the old atlas.
        if (layers == null || layers.model() != model) {
            layers = Layers.from(model);
            if (lit) onLayers = layers;
            else offLayers = layers;
        }

        var opaque = buffers.getBuffer(Sheets.cutoutBlockSheet());
        renderer.renderQuadList(poses, opaque, layers.housing(), stack, light, overlay);
        if (lit) {
            // The core and tinted lens share a sorted translucent pass. This preserves
            // visibility through the glass while the shader ignores ambient light.
            var emissive = buffers.getBuffer(EMISSIVE);
            float brightness = intensity / 15.0f;
            for (var quad : layers.luminous()) {
                emissive.putBulkData(poses.last(), quad, brightness, brightness, brightness, 1.0f,
                        LightTexture.FULL_BRIGHT, overlay);
            }
        } else {
            renderer.renderQuadList(poses, opaque, layers.emitter(), stack, light, overlay);
            renderer.renderQuadList(poses, buffers.getBuffer(Sheets.translucentCullBlockSheet()),
                    layers.lens(), stack, light, overlay);
        }
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(AeronauticsFlyByInstruments.MOD_ID, "block/" + name);
    }

    private record Layers(BakedModel model, List<BakedQuad> housing, List<BakedQuad> lens,
                          List<BakedQuad> emitter, List<BakedQuad> luminous) {
        static Layers from(BakedModel model) {
            var quads = new ArrayList<BakedQuad>();
            var random = RandomSource.create(42);
            for (var direction : Direction.values()) {
                random.setSeed(42);
                quads.addAll(model.getQuads(null, direction, random, ModelData.EMPTY, null));
            }
            random.setSeed(42);
            quads.addAll(model.getQuads(null, null, random, ModelData.EMPTY, null));

            var housing = new ArrayList<BakedQuad>();
            var lens = new ArrayList<BakedQuad>();
            var emitter = new ArrayList<BakedQuad>();
            for (var quad : quads) {
                var name = quad.getSprite().contents().name();
                if (name.equals(LENS)) lens.add(quad);
                else if (name.equals(EMITTER_ON) || name.equals(EMITTER_OFF)) emitter.add(quad);
                else housing.add(quad);
            }
            var luminous = new ArrayList<>(emitter);
            luminous.addAll(lens);
            return new Layers(model, List.copyOf(housing), List.copyOf(lens), List.copyOf(emitter), List.copyOf(luminous));
        }
    }
}
