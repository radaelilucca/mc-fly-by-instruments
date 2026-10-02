package com.radaeli.aeronauticsflybyinstruments.panel;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.avionics.AvionicsEndpoint;
import com.radaeli.aeronauticsflybyinstruments.integration.TablePeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashMap;
import java.util.Map;

public final class ModularTableBlockEntity extends BlockEntity implements AvionicsEndpoint {
    private PanelGrid grid = new PanelGrid();
    private final Map<String, Double> testSignals = new HashMap<>();
    private final TablePeripheral peripheral = new TablePeripheral(this);

    public ModularTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MODULAR_TABLE_ENTITY.get(), pos, state);
    }

    public PanelGrid grid() { return grid; }
    public TablePeripheral peripheral() { return peripheral; }
    @Override public String avionicsType() { return "modular_table"; }
    public boolean configureModule(int column, int row, String channel, double threshold) {
        if (channel == null || channel.isBlank() || channel.length() > 64 || !Double.isFinite(threshold)) return false;
        if (grid.at(column, row).filter(module -> module.type().equals("lamp")).isEmpty()) return false;
        if (!grid.configureAt(column, row, channel, threshold)) return false;
        var module = grid.at(column, row).orElseThrow();
        changed("configured", module);
        return true;
    }

    public boolean setTestSignal(String channel, double value) {
        if (channel == null || channel.isBlank() || channel.length() > 64 || !Double.isFinite(value)) return false;
        testSignals.put(channel, value);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            peripheral.testSignalChanged(channel, value);
        }
        return true;
    }

    public double testSignal(String channel) { return testSignals.getOrDefault(channel, 0.0); }
    public boolean isModuleActive(PanelGrid.Module module) {
        return module.type().equals("lamp") && testSignal(module.channel()) >= module.threshold();
    }

    public boolean placeLamp(int column, int row) {
        return placeModule(new PanelGrid.Module("lamp", column, row, 1, 1));
    }

    public boolean placeLamp2x1(int column, int row, boolean vertical) {
        var module = vertical
                ? new PanelGrid.Module("lamp_2x1", column, row, 1, 2)
                : new PanelGrid.Module("lamp_2x1", column, row, 2, 1);
        return placeModule(module);
    }

    private boolean placeModule(PanelGrid.Module module) {
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
            entry.putString("Channel", module.channel());
            entry.putDouble("Threshold", module.threshold());
            list.add(entry);
        }
        tag.put("Modules", list);
        var signals = new CompoundTag();
        testSignals.forEach(signals::putDouble);
        tag.put("TestSignals", signals);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        grid = new PanelGrid();
        var list = tag.getList("Modules", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            var entry = list.getCompound(i);
            // Only registered module types and their real footprints may be restored.
            var type = entry.getString("Type");
            int width = entry.getInt("Width");
            int height = entry.getInt("Height");
            boolean validLamp = type.equals("lamp") && width == 1 && height == 1;
            boolean validLamp2x1 = type.equals("lamp_2x1")
                    && (width == 2 && height == 1 || width == 1 && height == 2);
            if (validLamp || validLamp2x1) {
                String channel = entry.contains("Channel", Tag.TAG_STRING) ? entry.getString("Channel") : "test";
                double threshold = entry.contains("Threshold", Tag.TAG_DOUBLE) ? entry.getDouble("Threshold") : 0.5;
                if (!channel.isBlank() && channel.length() <= 64 && Double.isFinite(threshold)) {
                    grid.place(new PanelGrid.Module(type, entry.getInt("Column"), entry.getInt("Row"),
                            width, height, channel, threshold));
                }
            }
        }
        testSignals.clear();
        if (tag.contains("TestSignals", Tag.TAG_COMPOUND)) {
            var signals = tag.getCompound("TestSignals");
            for (String channel : signals.getAllKeys()) {
                double value = signals.getDouble(channel);
                if (!channel.isBlank() && channel.length() <= 64 && Double.isFinite(value)) testSignals.put(channel, value);
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
