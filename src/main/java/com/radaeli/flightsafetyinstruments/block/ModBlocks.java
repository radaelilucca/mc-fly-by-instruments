package com.radaeli.flightsafetyinstruments.block;

import com.radaeli.flightsafetyinstruments.FlightSafetyInstruments;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import com.radaeli.flightsafetyinstruments.item.LampModuleItem;
import com.radaeli.flightsafetyinstruments.panel.ModularTableBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(FlightSafetyInstruments.MOD_ID);
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(FlightSafetyInstruments.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FlightSafetyInstruments.MOD_ID);

    public static final DeferredBlock<ModularTableBaseBlock> MODULAR_TABLE_BASE =
            BLOCKS.register("modular_table_base", () -> new ModularTableBaseBlock(
                    BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().dynamicShape()));
    public static final DeferredItem<BlockItem> MODULAR_TABLE_BASE_ITEM =
            ITEMS.registerSimpleBlockItem(MODULAR_TABLE_BASE);
    public static final DeferredItem<LampModuleItem> LAMP_MODULE =
            ITEMS.register("lamp_module", () -> new LampModuleItem(new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ModularTableBlockEntity>> MODULAR_TABLE_ENTITY =
            BLOCK_ENTITIES.register("modular_table", () -> BlockEntityType.Builder.of(
                    ModularTableBlockEntity::new, MODULAR_TABLE_BASE.get()).build(null));

    private ModBlocks() {}
}
