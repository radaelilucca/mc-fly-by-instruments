package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsLocation;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsModuleData;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsWaypoint;
import com.radaeli.aeronauticsflybyinstruments.item.FmsNavigatorItem;
import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.UUID;

public final class FmsNetwork {
    private FmsNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(FmsActionPayload.TYPE, FmsActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> handle(payload, context.player())));
        registrar.playToClient(FmsScreenPayload.TYPE, FmsScreenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        com.radaeli.aeronauticsflybyinstruments.client.FmsClient.openOrUpdate(payload);
                    }
                }));
    }

    public static void open(ServerPlayer player, BlockPos position, int column, int row,
                            ModularTableBlockEntity table) {
        if (!position.equals(table.getBlockPos())
                || !com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction.canReach(player, table)) return;
        sendScreen(player, position, column, row, table);
    }

    private static void sendScreen(ServerPlayer player, BlockPos position, int column, int row,
                                   ModularTableBlockEntity table) {
        FmsModuleData data = table.fmsAt(column, row).map(ModularTableBlockEntity.FmsModuleAt::data).orElse(null);
        if (data == null) return;
        var bound = navigatorInInventory(player.getInventory());
        Vec3 current = FmsLocation.forEntity(player);
        PacketDistributor.sendToPlayer(player, new FmsScreenPayload(position, column, row, data,
                player.level().dimension().location().toString(), current.x, current.y, current.z,
                bound.displaySlot(), bound.id() == null ? "" : bound.id().toString(), table.moduleToken(column, row)));
    }

    private static void handle(FmsActionPayload payload, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (payload.column() < 0 || payload.column() >= 3 || payload.row() < 0 || payload.row() >= 3) return;
        BlockPos position = payload.position();
        if (!serverPlayer.level().hasChunkAt(position)
                || !(serverPlayer.level().getBlockEntity(position) instanceof ModularTableBlockEntity table)
                || !com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction.canReach(serverPlayer, table)
                || !payload.interactionToken().equals(table.moduleToken(payload.column(), payload.row()))
                || table.fmsAt(payload.column(), payload.row()).isEmpty()) return;

        FmsModuleData data = table.fmsAt(payload.column(), payload.row()).orElseThrow().data();
        UUID waypointId;
        boolean accepted = switch (payload.action()) {
            case "create" -> table.addFmsWaypoint(payload.column(), payload.row(), payload.name(),
                    payload.x(), payload.y(), payload.z());
            case "update" -> {
                waypointId = parseId(payload.waypointId());
                yield waypointId != null && table.updateFmsWaypoint(payload.column(), payload.row(), waypointId,
                        payload.name(), payload.x(), payload.y(), payload.z());
            }
            case "delete" -> {
                waypointId = parseId(payload.waypointId());
                yield waypointId != null && table.deleteFmsWaypoint(payload.column(), payload.row(), waypointId);
            }
            case "select" -> {
                waypointId = parseId(payload.waypointId());
                yield waypointId != null && table.selectFmsWaypoint(payload.column(), payload.row(), waypointId);
            }
            case "clear_target" -> table.selectFmsWaypoint(payload.column(), payload.row(), null);
            case "bind_navigator" -> bindNavigator(serverPlayer, payload, data.id());
            default -> false;
        };
        if (!accepted && (payload.action().equals("create") || payload.action().equals("update"))) {
            serverPlayer.displayClientMessage(Component.translatable("message.aeronautics_fly_by_instruments.fms.invalid_waypoint"), true);
        }
        sendScreen(serverPlayer, position, payload.column(), payload.row(), table);
    }

    private static boolean bindNavigator(ServerPlayer player, FmsActionPayload payload, UUID targetId) {
        int actualSlot = actualInventorySlot(payload.navigatorDisplaySlot());
        if (actualSlot < 0 || actualSlot >= Inventory.INVENTORY_SIZE) return false;
        ItemStack stack = player.getInventory().getItem(actualSlot);
        if (!stack.is(ModBlocks.FMS_NAVIGATOR.get())) return false;
        UUID previous = FmsNavigatorItem.boundFmsId(stack);
        if (previous != null && !previous.equals(targetId) && !payload.confirmRebind()) {
            player.displayClientMessage(Component.translatable(
                    "message.aeronautics_fly_by_instruments.fms.confirm_rebind"), true);
            return false;
        }
        FmsNavigatorItem.bind(stack, targetId);
        player.getInventory().setChanged();
        return true;
    }

    private static int actualInventorySlot(int displaySlot) {
        if (displaySlot < 0 || displaySlot >= 36) return -1;
        return displaySlot < 27 ? displaySlot + 9 : displaySlot - 27;
    }

    private static UUID parseId(String id) {
        try { return id == null || id.isBlank() ? null : UUID.fromString(id); }
        catch (IllegalArgumentException invalid) { return null; }
    }

    private static NavigatorSelection navigatorInInventory(Inventory inventory) {
        for (int actual = 0; actual < 36; actual++) {
            ItemStack stack = inventory.getItem(actual);
            if (!stack.is(ModBlocks.FMS_NAVIGATOR.get())) continue;
            int display = actual >= 9 ? actual - 9 : actual + 27;
            return new NavigatorSelection(display, FmsNavigatorItem.boundFmsId(stack));
        }
        return new NavigatorSelection(-1, null);
    }

    private record NavigatorSelection(int displaySlot, UUID id) {}
}
