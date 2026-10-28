package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig.Channel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Redstone Link frequency of one sensor channel, opened from a Link slot: the pair's two slots on
 * top, the player's inventory and hotbar below. Clicking an item copies it (never consumed) into
 * the armed slot, dragging it onto a slot puts it there; changes apply at once through the sensor
 * screen, which this one returns to.
 */
public final class FrequencyPickerScreen extends HudScreen {
    private static final String GUI = SensorOutputConfigScreen.GUI;
    private static final int WIDTH = 9 * (HudPanel.SLOT + 1) + 2 * HudPanel.PAD + 2;
    private static final int HOTBAR = 9;

    private final SensorOutputConfigScreen parent;
    private final Channel channel;

    FrequencyPickerScreen(SensorOutputConfigScreen parent, Channel channel) {
        super(Component.translatable(GUI + "picker"));
        this.parent = parent;
        this.channel = channel;
    }

    @Override
    protected HudPanel build() {
        var pair = parent.config().frequencies(channel);
        String slotTooltip = tr(GUI + "picker_slot_tooltip");
        HudPanel panel = new HudPanel(WIDTH)
                .header(tr(GUI + "picker"), parent.channelLabel(channel))
                .items(tr(GUI + "link"), List.of(
                        new HudPanel.Slot(pair.first(), SensorOutputConfigScreen.FIRST_FREQUENCY_COLOR,
                                parent.selected(channel, true), () -> parent.selectSlot(channel, true),
                                () -> parent.clearSlot(channel, true), slotTooltip,
                                stack -> parent.dropFrequency(channel, true, stack)),
                        new HudPanel.Slot(pair.second(), SensorOutputConfigScreen.SECOND_FREQUENCY_COLOR,
                                parent.selected(channel, false), () -> parent.selectSlot(channel, false),
                                () -> parent.clearSlot(channel, false), slotTooltip,
                                stack -> parent.dropFrequency(channel, false, stack))),
                        tr(parent.linkStatusKey(channel)), HudPanel.palette().mutedText());
        panel.tooltip(tr(GUI + "frequency_picker_tooltip"));

        List<ItemStack> items = parent.inventory();
        panel.section(tr(GUI + "inventory"));
        panel.grid(slots(items, HOTBAR, items.size()), 9);
        panel.grid(slots(items, 0, Math.min(HOTBAR, items.size())), 9);
        panel.buttons(List.of(tr(GUI + "done")), List.of(this::onClose));
        return panel;
    }

    private List<HudPanel.Slot> slots(List<ItemStack> items, int from, int to) {
        List<HudPanel.Slot> result = new ArrayList<>();
        for (int index = from; index < to; index++) {
            ItemStack stack = items.get(index).copyWithCount(1);
            result.add(new HudPanel.Slot(stack, 0, false, stack.isEmpty() ? null : () -> parent.pick(channel, stack),
                    null, ""));
        }
        return result;
    }

    /** ESC and Done go back to the sensor screen, not out of the game menu. */
    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
