package com.radaeli.aeronauticsflybyinstruments.item;

import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class LampModuleItem extends Item {
    public LampModuleItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return place(context, "lamp", 1, 1);
    }

    static InteractionResult place(UseOnContext context, String type, int width, int height) {
        if (!com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction.validPlacement(context, type))
            return InteractionResult.FAIL;
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof ModularTableBlockEntity table)) return InteractionResult.PASS;
        var target = com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction.target(table,
                context.getClickedFace(), context.getClickLocation());
        if (!target.exposed()) return InteractionResult.FAIL;
        var held = com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction.heldModule(context.getPlayer()).orElse(null);
        if (held == null || !held.type().equals(type) || held.width() != width || held.height() != height)
            return InteractionResult.FAIL;
        int column = target.column();
        int row = target.row();
        if (!table.grid().canFit(column, row, width, height)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            boolean placed = switch (type) {
                case "lamp" -> table.placeLamp(column, row);
                case "lamp_2x1" -> table.placeLamp2x1(column, row, height > width);
                case "buzzer" -> table.placeBuzzer(column, row);
                case "fms" -> table.placeFmsModule(column, row, FmsModuleItem.load(context.getItemInHand()));
                default -> false;
            };
            if (!placed) return InteractionResult.FAIL;
            context.getItemInHand().consume(1, context.getPlayer());
            context.getPlayer().getInventory().setChanged();
            context.getPlayer().containerMenu.broadcastChanges();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
