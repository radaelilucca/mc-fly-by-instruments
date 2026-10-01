package com.radaeli.flightsafetyinstruments.panel;

import com.radaeli.flightsafetyinstruments.block.ModBlocks;
import com.radaeli.flightsafetyinstruments.integration.TablePeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ModularTableBlockEntity extends BlockEntity {
    private PanelGrid grid = new PanelGrid();
    private final TablePeripheral peripheral = new TablePeripheral(this);

    public ModularTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MODULAR_TABLE_ENTITY.get(), pos, state);
    }

    public PanelGrid grid() { return grid; }
    public TablePeripheral peripheral() { return peripheral; }

    public boolean placeLamp(int column, int row) {
        var module = new PanelGrid.Module("lamp", column, row, 1, 1);
        if (!grid.place(module)) return false;
        changed("placed", module);
        return true;
    }

    public boolean removeModule(int column, int row) {
        var removed = grid.removeAt(column, row);
        removed.ifPresent(module -> changed("removed", module));
        return removed.isPresent();
    }

    private void changed(String action, PanelGrid.Module module) {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            peripheral.moduleChanged(action, module);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var list = new ListTag();
        for (var module : grid.modules()) {
            var entry = new CompoundTag();
            entry.putString("Type", module.type());
            entry.putInt("Column", module.column());
            entry.putInt("Row", module.row());
            entry.putInt("Width", module.width());
            entry.putInt("Height", module.height());
            list.add(entry);
        }
        tag.put("Modules", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        grid = new PanelGrid();
        var list = tag.getList("Modules", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            // Only registered module types and their real footprints may be restored.
            if (entry.getString("Type").equals("lamp") && entry.getInt("Width") == 1 && entry.getInt("Height") == 1) {
                grid.place(new PanelGrid.Module("lamp", entry.getInt("Column"), entry.getInt("Row"), 1, 1));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
