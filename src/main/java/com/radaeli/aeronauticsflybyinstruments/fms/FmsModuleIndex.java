package com.radaeli.aeronauticsflybyinstruments.fms;

import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Index of loaded FMS tables only. Lookups never request or load chunks. */
public final class FmsModuleIndex {
    private static final Map<Level, Set<ModularTableBlockEntity>> LOADED = new WeakHashMap<>();

    private FmsModuleIndex() {}

    public static void register(ModularTableBlockEntity table) {
        Level level = table.getLevel();
        if (level == null || level.isClientSide) return;
        synchronized (LOADED) {
            var tables = LOADED.computeIfAbsent(level, ignored -> new HashSet<>());
            tables.add(table);
            Set<UUID> localIdentities = new HashSet<>();
            for (var module : table.fmsModules()) {
                UUID id = module.data().id();
                boolean duplicate = !localIdentities.add(id) || tables.stream().filter(other -> other != table)
                        .flatMap(other -> other.fmsModules().stream())
                        .anyMatch(candidate -> candidate.data().id().equals(id));
                if (duplicate) {
                    UUID replacement;
                    do replacement = UUID.randomUUID();
                    while (localIdentities.contains(replacement));
                    table.replaceFmsIdentity(module.column(), module.row(), replacement);
                    localIdentities.add(replacement);
                }
            }
        }
    }

    public static void unregister(ModularTableBlockEntity table) {
        Level level = table.getLevel();
        if (level == null) return;
        synchronized (LOADED) {
            Set<ModularTableBlockEntity> tables = LOADED.get(level);
            if (tables != null) {
                tables.remove(table);
                if (tables.isEmpty()) LOADED.remove(level);
            }
        }
    }

    /** The loaded table holding the module, wherever it is (world or any sublevel). */
    public static ModularTableBlockEntity find(Level level, UUID id) {
        if (level == null || level.isClientSide || id == null) return null;
        synchronized (LOADED) {
            for (var table : LOADED.getOrDefault(level, Set.of())) {
                if (table.isRemoved() || table.getLevel() != level) continue;
                if (table.findFmsById(id).isPresent()) return table;
            }
        }
        return null;
    }

    public static boolean hasIdentity(Level level, UUID id) {
        if (level == null || level.isClientSide || id == null) return false;
        synchronized (LOADED) {
            return LOADED.getOrDefault(level, Set.of()).stream()
                    .filter(table -> !table.isRemoved() && table.getLevel() == level)
                    .flatMap(table -> table.fmsModules().stream())
                    .anyMatch(module -> module.data().id().equals(id));
        }
    }
}
