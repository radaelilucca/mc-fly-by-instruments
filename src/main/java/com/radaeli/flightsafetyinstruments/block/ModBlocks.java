package com.radaeli.flightsafetyinstruments.block;

import com.radaeli.flightsafetyinstruments.AeronauticsFlyByInstruments;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import com.radaeli.flightsafetyinstruments.item.LampModuleItem;
import com.radaeli.flightsafetyinstruments.item.LampModule2x1Item;
import com.radaeli.flightsafetyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.flightsafetyinstruments.avionics.NetworkBridgeBlockEntity;
import com.radaeli.flightsafetyinstruments.sensor.RadarAltimeterBlockEntity;
import com.radaeli.flightsafetyinstruments.sensor.RadarAltimeterBlock;
import com.radaeli.flightsafetyinstruments.sensor.TerrainScannerBlock;
import com.radaeli.flightsafetyinstruments.sensor.AdvancedTerrainScannerBlock;
import com.radaeli.flightsafetyinstruments.sensor.TerrainScannerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(AeronauticsFlyByInstruments.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(AeronauticsFlyByInstruments.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AeronauticsFlyByInstruments.MOD_ID);

    public static final DeferredBlock<ModularTableBaseBlock> MODULAR_TABLE_BASE =
            BLOCKS.register("modular_table_base", () -> new ModularTableBaseBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().dynamicShape()));
    public static final DeferredItem<BlockItem> MODULAR_TABLE_BASE_ITEM =
            ITEMS.registerSimpleBlockItem(MODULAR_TABLE_BASE);
    public static final DeferredBlock<AvionicsCableBlock> AVIONICS_CABLE =
            BLOCKS.register("avionics_cable", () -> new AvionicsCableBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).noOcclusion().dynamicShape()));
    public static final DeferredItem<BlockItem> AVIONICS_CABLE_ITEM =
            ITEMS.registerSimpleBlockItem(AVIONICS_CABLE);
    public static final DeferredBlock<NetworkBridgeBlock> NETWORK_BRIDGE =
            BLOCKS.register("network_bridge", () -> new NetworkBridgeBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0f)));
    public static final DeferredItem<BlockItem> NETWORK_BRIDGE_ITEM =
            ITEMS.registerSimpleBlockItem(NETWORK_BRIDGE);
    public static final DeferredBlock<RadarAltimeterBlock> RADAR_ALTIMETER =
            BLOCKS.register("radar_altimeter", () -> new RadarAltimeterBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(2.0f)));
    public static final DeferredItem<BlockItem> RADAR_ALTIMETER_ITEM =
            ITEMS.registerSimpleBlockItem(RADAR_ALTIMETER);
    public static final DeferredBlock<TerrainScannerBlock> TERRAIN_SCANNER =
            BLOCKS.register("terrain_scanner", () -> new TerrainScannerBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OBSERVER).strength(2.0f)));
    public static final DeferredItem<BlockItem> TERRAIN_SCANNER_ITEM = ITEMS.registerSimpleBlockItem(TERRAIN_SCANNER);
    public static final DeferredBlock<AdvancedTerrainScannerBlock> TERRAIN_SCANNER_ADVANCED =
            BLOCKS.register("terrain_scanner_advanced", () -> new AdvancedTerrainScannerBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OBSERVER).strength(2.0f)));
    public static final DeferredItem<BlockItem> TERRAIN_SCANNER_ADVANCED_ITEM =
            ITEMS.registerSimpleBlockItem(TERRAIN_SCANNER_ADVANCED);
    public static final DeferredItem<LampModuleItem> LAMP_MODULE =
            ITEMS.register("lamp_module", () -> new LampModuleItem(new Item.Properties()));
    public static final DeferredItem<LampModule2x1Item> LAMP_MODULE_2X1 =
            ITEMS.register("lamp_module_2x1", () -> new LampModule2x1Item(new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ModularTableBlockEntity>> MODULAR_TABLE_ENTITY =
            BLOCK_ENTITIES.register("modular_table", () -> BlockEntityType.Builder.of(
                    ModularTableBlockEntity::new, MODULAR_TABLE_BASE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkBridgeBlockEntity>> NETWORK_BRIDGE_ENTITY =
            BLOCK_ENTITIES.register("network_bridge", () -> BlockEntityType.Builder.of(
                    NetworkBridgeBlockEntity::new, NETWORK_BRIDGE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadarAltimeterBlockEntity>> RADAR_ALTIMETER_ENTITY =
            BLOCK_ENTITIES.register("radar_altimeter", () -> BlockEntityType.Builder.of(
                    RadarAltimeterBlockEntity::new, RADAR_ALTIMETER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TerrainScannerBlockEntity>> TERRAIN_SCANNER_ENTITY =
            BLOCK_ENTITIES.register("terrain_scanner", () -> BlockEntityType.Builder.of(
                    TerrainScannerBlockEntity::new, TERRAIN_SCANNER.get(), TERRAIN_SCANNER_ADVANCED.get()).build(null));

    private ModBlocks() {}
}
