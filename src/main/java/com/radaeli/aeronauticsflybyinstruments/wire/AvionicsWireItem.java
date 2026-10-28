package com.radaeli.aeronauticsflybyinstruments.wire;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.config.ModConfig;
import dev.ryanhcode.sable.Sable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Spool of Avionics Wire. Click a connector, then supports in order, then another connector. The route in
 * progress lives on the stack ({@link WireRouteInProgress}); the server is the authority, the client only
 * previews. Sneak-click in the air to undo the last point.
 */
public final class AvionicsWireItem extends Item {
    private static final String MESSAGE = "message.aeronautics_fly_by_instruments.wire.";

    public AvionicsWireItem(Properties properties) {
        super(properties);
    }

    public static WireRouteInProgress route(ItemStack stack) {
        return stack.get(ModBlocks.WIRE_ROUTE.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof WireNodeBlock)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !(level.getBlockEntity(pos) instanceof WireNodeBlockEntity node)) return InteractionResult.FAIL;

        WireRouteInProgress route = route(stack);
        if (route != null && !connectorAt(level, route.positions().getFirst())) {
            stack.remove(ModBlocks.WIRE_ROUTE.get());
            route = null;
        }
        if (route == null) {
            if (!node.isConnector()) return refuse(player, "start_at_connector");
            if (node.isFull()) return refuse(player, "full");
            stack.set(ModBlocks.WIRE_ROUTE.get(), WireRouteInProgress.start(pos));
            tell(player, Component.translatable(MESSAGE + "started"));
            click(level, pos, 1.2f);
            return InteractionResult.SUCCESS;
        }
        if (pos.equals(route.positions().getFirst()) && route.positions().size() == 1) {
            stack.remove(ModBlocks.WIRE_ROUTE.get());
            tell(player, Component.translatable(MESSAGE + "cancelled"));
            return InteractionResult.SUCCESS;
        }

        var check = WireRules.canAppend(route.positions(), pos, ModConfig.WIRE_MAX_SPAN.get(), ModConfig.WIRE_MAX_POINTS.get());
        if (check == WireRules.Check.OK && !sameStructure(level, route.positions().getFirst(), pos)) {
            check = WireRules.Check.OTHER_STRUCTURE;
        }
        if (check != WireRules.Check.OK) return refuse(player, check, ModConfig.WIRE_MAX_SPAN.get());
        if (node.isFull()) return refuse(player, "full");

        if (!node.isConnector()) {
            int lane = WireRules.freeLane(node.usedLanes());
            var extended = route.append(pos, lane);
            stack.set(ModBlocks.WIRE_ROUTE.get(), extended);
            tell(player, Component.translatable(MESSAGE + "point_added", extended.positions().size() - 1,
                    meters(WireRoute.length(extended.positions()))));
            click(level, pos, 1.0f);
            return InteractionResult.SUCCESS;
        }
        return finish(level, player, stack, route.append(pos, 0));
    }

    private InteractionResult finish(Level level, Player player, ItemStack stack, WireRouteInProgress route) {
        var positions = route.positions();
        double length = WireRoute.length(positions);
        int cost = WireRules.cost(length, ModConfig.WIRE_BLOCKS_PER_ITEM.get());
        if (!player.getAbilities().instabuild && countWire(player) < cost) {
            return refuse(player, Component.translatable(MESSAGE + "not_enough", cost));
        }
        // Nodes may have changed since they were clicked.
        for (int i = 0; i < positions.size(); i++) {
            if (!(level.getBlockEntity(positions.get(i)) instanceof WireNodeBlockEntity node)
                    || node.isConnector() != (i == 0 || i == positions.size() - 1) || node.isFull()
                    || (!node.isConnector() && node.usedLanes().contains(route.lanes().get(i)))) {
                stack.remove(ModBlocks.WIRE_ROUTE.get());
                return refuse(player, "changed");
            }
        }

        var wire = WireRoute.of(UUID.randomUUID(), positions, route.lanes());
        for (int i = 0; i < positions.size(); i++) {
            var node = (WireNodeBlockEntity) Objects.requireNonNull(level.getBlockEntity(positions.get(i)));
            node.addRoute(wire.rebasedTo(wire.anchors().get(i).offset()));
        }
        stack.remove(ModBlocks.WIRE_ROUTE.get());
        if (!player.getAbilities().instabuild) consumeWire(player, stack, cost);
        level.playSound(null, positions.getLast(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 1.1f);
        tell(player, Component.translatable(MESSAGE + "finished", meters(length), cost));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        WireRouteInProgress route = route(stack);
        if (route == null || !player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            var undone = route.undo();
            if (undone == null) {
                stack.remove(ModBlocks.WIRE_ROUTE.get());
                tell(player, Component.translatable(MESSAGE + "cancelled"));
            } else {
                stack.set(ModBlocks.WIRE_ROUTE.get(), undone);
                tell(player, Component.translatable(MESSAGE + "undone", undone.positions().size() - 1));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var route = route(stack);
        if (route != null) {
            tooltip.add(Component.translatable(MESSAGE + "routing", route.positions().size() - 1,
                    meters(WireRoute.length(route.positions()))).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.aeronautics_fly_by_instruments.avionics_wire.hint").withStyle(ChatFormatting.GRAY));
    }

    public static boolean sameStructure(Level level, BlockPos a, BlockPos b) {
        return Sable.HELPER.getContaining(level, (Vec3i) a) == Sable.HELPER.getContaining(level, (Vec3i) b);
    }

    public static String meters(double length) {
        return String.format(Locale.ROOT, "%.1f", length);
    }

    private static boolean connectorAt(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockState(pos).getBlock() instanceof WireConnectorBlock;
    }

    private static int countWire(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().items) if (item.getItem() instanceof AvionicsWireItem) count += item.getCount();
        for (ItemStack item : player.getInventory().offhand) if (item.getItem() instanceof AvionicsWireItem) count += item.getCount();
        return count;
    }

    private static void consumeWire(Player player, ItemStack held, int cost) {
        int fromHeld = Math.min(cost, held.getCount());
        held.shrink(fromHeld);
        int remaining = cost - fromHeld;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == held || !(item.getItem() instanceof AvionicsWireItem)) continue;
            int taken = Math.min(remaining, item.getCount());
            item.shrink(taken);
            remaining -= taken;
        }
        inventory.setChanged();
    }

    private static void click(Level level, BlockPos pos, float pitch) {
        level.playSound(null, pos, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.7f, pitch);
    }

    private static void tell(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    private static InteractionResult refuse(Player player, String key) {
        return refuse(player, Component.translatable(MESSAGE + key));
    }

    private static InteractionResult refuse(Player player, WireRules.Check check, int maxSpan) {
        return refuse(player, Component.translatable(check.translationKey(), maxSpan));
    }

    private static InteractionResult refuse(Player player, Component message) {
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.RED), true);
        return InteractionResult.FAIL;
    }
}
