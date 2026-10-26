package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsModuleData;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsWaypoint;
import com.radaeli.aeronauticsflybyinstruments.network.FmsActionPayload;
import com.radaeli.aeronauticsflybyinstruments.network.FmsScreenPayload;
import com.radaeli.aeronauticsflybyinstruments.item.FmsNavigatorItem;
import com.radaeli.ruilib.ui.Alignment;
import com.radaeli.ruilib.ui.Bounds;
import com.radaeli.ruilib.ui.Button;
import com.radaeli.ruilib.ui.Column;
import com.radaeli.ruilib.ui.InventoryGrid;
import com.radaeli.ruilib.ui.Insets;
import com.radaeli.ruilib.ui.ItemSlot;
import com.radaeli.ruilib.ui.Label;
import com.radaeli.ruilib.ui.Modal;
import com.radaeli.ruilib.ui.Panel;
import com.radaeli.ruilib.ui.Row;
import com.radaeli.ruilib.ui.ScrollView;
import com.radaeli.ruilib.ui.Spacer;
import com.radaeli.ruilib.ui.Tabs;
import com.radaeli.ruilib.ui.TextField;
import com.radaeli.ruilib.ui.Toggle;
import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiContext;
import com.radaeli.ruilib.ui.UiPaint;
import com.radaeli.ruilib.ui.UiScreen;
import com.radaeli.ruilib.ui.UiSize;
import com.radaeli.ruilib.ui.UiTheme;
import com.radaeli.ruilib.ui.UiText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

/** Two separate FMS flows: waypoint CRUD and navigation/link configuration. */
public final class FmsScreen extends UiScreen {
    private static final int CONTENT_WIDTH = 9 * 16 + 8;
    private static final int PANEL_WIDTH = CONTENT_WIDTH + 16;
    private final State state;

    public FmsScreen(FmsScreenPayload payload) {
        this(payload, new State(payload));
    }

    private FmsScreen(FmsScreenPayload payload, State state) {
        super(Component.translatable("gui.aeronautics_fly_by_instruments.fms.title"),
                createRoot(payload, state), UiTheme.DARK);
        this.state = state;
        state.screen = this;
    }

    private static UiComponent createRoot(FmsScreenPayload payload, State state) {
        Column content = new Column(5, Alignment.START, Alignment.STRETCH);
        content.add(new Label(Component.translatable("gui.aeronautics_fly_by_instruments.fms.title")));

        state.waypointList = waypointList(state.data.waypoints(), 74, state::beginEdit);
        state.waypointPage = new Column(4, Alignment.START, Alignment.STRETCH);
        state.waypointPage.add(Spacer.fixed(1, 6));
        state.waypointPage.add(state.waypointList);
        state.waypointPage.add(Spacer.fixed(1, 7));
        Button newWaypoint = new Button(Component.translatable(
                "gui.aeronautics_fly_by_instruments.fms.new_waypoint"), state::beginCreate);
        newWaypoint.setPreferredSize(CONTENT_WIDTH, UiTheme.DARK.tokens().buttonMinimumHeight());
        state.waypointPage.add(newWaypoint);

        state.form = createWaypointForm(state);

        Column navigatorPage = new Column(5, Alignment.START, Alignment.STRETCH);
        navigatorPage.add(Spacer.fixed(1, 6));
        state.targetButton = new Button(state.targetButtonLabel(), state::targetButtonPressed);
        state.targetButton.setPreferredSize(CONTENT_WIDTH, UiTheme.DARK.tokens().buttonMinimumHeight());
        state.targetStatus = new MutableLabel(Component.empty(), CONTENT_WIDTH, 12);
        state.targetCoordinates = new MutableLabel(Component.empty(), CONTENT_WIDTH, 12);
        state.targetDistance = new MutableLabel(Component.empty(), CONTENT_WIDTH, 12);
        state.navigatorDetails = new Column(3, Alignment.START, Alignment.STRETCH);
        state.navigatorDetails.add(state.targetStatus);
        state.navigatorContent = new Column(4, Alignment.START, Alignment.STRETCH);
        state.updateTargetActionOrder();
        state.refreshTargetPresentation();
        navigatorPage.add(state.navigatorContent);

        state.linkerStatus = new MutableLabel(Component.empty(), CONTENT_WIDTH, 12);
        state.linkerStatus.setText(state.linkerStatus());
        navigatorPage.add(state.linkerStatus);
        Button openLinker = new Button(Component.translatable(
                "gui.aeronautics_fly_by_instruments.fms.open_linker"), state::openLinker);
        openLinker.setPreferredSize(CONTENT_WIDTH, UiTheme.DARK.tokens().buttonMinimumHeight());
        navigatorPage.add(openLinker);

        Tabs tabs = new Tabs()
                .setTabAlignment(Alignment.CENTER)
                .addTab(Component.translatable("gui.aeronautics_fly_by_instruments.fms.waypoints"), state.waypointPage)
                .addTab(Component.translatable("gui.aeronautics_fly_by_instruments.fms.navigator"), navigatorPage);
        content.add(tabs);
        content.add(Spacer.fixed(1, 2));
        Panel panel = new Panel(content, Insets.all(8));
        panel.setPreferredSize(PANEL_WIDTH, null);
        state.panel = panel;
        return panel;
    }

    private static ScrollView waypointList(List<FmsWaypoint> waypoints, int height,
                                           Consumer<FmsWaypoint> onSelected) {
        ScrollView list = new ScrollView(2);
        list.setPreferredSize(null, height);
        populateWaypointList(list, waypoints, onSelected);
        return list;
    }

    private static void populateWaypointList(ScrollView list, List<FmsWaypoint> waypoints,
                                             Consumer<FmsWaypoint> onSelected) {
        for (UiComponent child : List.copyOf(list.children())) list.remove(child);
        for (FmsWaypoint waypoint : waypoints) {
            WaypointListItem item = new WaypointListItem(waypoint.name(), waypointCoordinatesLabel(waypoint),
                    () -> onSelected.accept(waypoint));
            item.setPreferredSize(null, UiTheme.DARK.tokens().buttonMinimumHeight());
            item.tooltip(Component.literal(waypointTooltipLabel(waypoint)));
            list.add(item);
        }
    }

    static String waypointCoordinatesLabel(FmsWaypoint waypoint) {
        return "x: " + formatCoordinate(waypoint.x()) + ", z: " + formatCoordinate(waypoint.z());
    }

    static String waypointTooltipLabel(FmsWaypoint waypoint) {
        return waypoint.name() + " - " + waypointCoordinatesLabel(waypoint)
                + ", y: " + formatCoordinate(waypoint.y());
    }

    private static Column createWaypointForm(State state) {
        Column form = new Column(4, Alignment.START, Alignment.CENTER);

        state.nameField = new TextField(Component.translatable("gui.aeronautics_fly_by_instruments.fms.name_hint"));
        state.nameField.setMaxLength(FmsWaypoint.MAX_NAME_LENGTH);
        state.nameField.setPreferredSize(CONTENT_WIDTH, 22);
        form.add(state.nameField);

        Row coordinates = new Row(4, Alignment.START, Alignment.CENTER);
        state.xField = coordinateField("X", "x");
        state.yField = coordinateField("Y", "y");
        state.zField = coordinateField("Z", "z");
        coordinates.add(state.xField);
        coordinates.add(state.yField);
        coordinates.add(state.zField);
        form.add(coordinates);

        Row positionAndSave = new Row(4, Alignment.CENTER, Alignment.CENTER);
        Button currentPosition = new Button(Component.translatable(
                "gui.aeronautics_fly_by_instruments.fms.current_position"), state::useCurrentPosition);
        currentPosition.setPreferredSize(74, UiTheme.DARK.tokens().buttonMinimumHeight());
        positionAndSave.add(currentPosition);
        Button saveWaypoint = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.fms.save_waypoint"),
                state::saveWaypoint);
        saveWaypoint.setPreferredSize(74, UiTheme.DARK.tokens().buttonMinimumHeight());
        positionAndSave.add(saveWaypoint);
        form.add(positionAndSave);

        Row formActions = new Row(4, Alignment.CENTER, Alignment.CENTER);
        state.deleteButton = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.fms.delete"),
                state::deleteWaypoint);
        state.deleteButton.setPreferredSize(74, UiTheme.DARK.tokens().buttonMinimumHeight());
        formActions.add(state.deleteButton);
        Button closeForm = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.fms.close_form"),
                state::closeForm);
        closeForm.setPreferredSize(74, UiTheme.DARK.tokens().buttonMinimumHeight());
        formActions.add(closeForm);
        form.add(formActions);
        return form;
    }

    private static TextField coordinateField(String placeholder, String coordinate) {
        TextField field = new TextField(Component.literal(placeholder));
        field.setMaxLength(32);
        field.setPreferredSize((CONTENT_WIDTH - 8) / 3, 22);
        field.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.fms.coordinate_" + coordinate));
        return field;
    }

    private static Column createTargetPicker(State state) {
        Column content = new Column(4, Alignment.START, Alignment.STRETCH);
        state.targetPickerList = waypointList(state.data.waypoints(), 112, state::selectTarget);
        content.add(state.targetPickerList);
        Button close = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.fms.close_picker"),
                () -> state.screen.closeModal());
        content.add(close);
        return content;
    }

    private static Column createLinkerModal(State state) {
        Column content = new Column(6, Alignment.START, Alignment.CENTER);
        state.linkerModalStatus = new MutableLabel(Component.empty(), CONTENT_WIDTH, 12);
        state.linkerModalStatus.setText(state.linkerStatus());
        content.add(state.linkerModalStatus);

        Row linkSlotRow = new Row(6, Alignment.CENTER, Alignment.CENTER);
        linkSlotRow.add(new Label(Component.translatable(
                "gui.aeronautics_fly_by_instruments.fms.linker_slot")));
        state.linkSlot = new ItemSlot();
        state.linkSlot.setPreferredSize(24, 24);
        state.linkSlot.setBackgroundColor(0xFF9B3939);
        linkSlotRow.add(state.linkSlot);
        content.add(linkSlotRow);

        state.inventory = InventoryGrid.playerInventory().setSlotSize(16).setGap(1)
                .setOnItemPickedWithSlot((displaySlot, stack) -> {
                    state.pendingLinkSlot = displaySlot;
                    state.pendingLinkIsNavigator = stack.is(ModBlocks.FMS_NAVIGATOR.get());
                });
        content.add(state.inventory);

        state.rebindToggle = new Toggle(Component.translatable(
                "gui.aeronautics_fly_by_instruments.fms.confirm_rebind"), ignored -> { });
        content.add(state.rebindToggle);

        state.linkSlot.setOnDrop(stack -> {
            if (state.syncingLinkSlot) return;
            if (!stack.isEmpty() && state.pendingLinkIsNavigator && state.pendingLinkSlot >= 0) {
                state.bindNavigator(state.pendingLinkSlot);
            }
            state.pendingLinkSlot = -1;
            state.pendingLinkIsNavigator = false;
            state.refreshLinkSlot();
        });

        Button close = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.fms.close_linker"),
                () -> state.screen.closeModal());
        content.add(close);
        return content;
    }

    boolean matches(FmsScreenPayload payload) {
        return state.position.equals(payload.position()) && state.column == payload.column()
                && state.row == payload.row() && state.interactionToken.equals(payload.interactionToken());
    }

    void acceptServerState(FmsScreenPayload payload) {
        state.data = payload.data().copy();
        state.dimension = payload.dimension();
        state.currentX = payload.currentX();
        state.currentY = payload.currentY();
        state.currentZ = payload.currentZ();
        state.navigatorDisplaySlot = payload.navigatorDisplaySlot();
        state.navigatorBoundId = payload.navigatorBoundId();
        populateWaypointList(state.waypointList, state.data.waypoints(), state::beginEdit);
        if (state.targetPickerList != null) {
            populateWaypointList(state.targetPickerList, state.data.waypoints(), state::selectTarget);
        }
        state.refreshTargetPresentation();
        state.linkerStatus.setText(state.linkerStatus());
        if (state.linkerModalStatus != null) state.linkerModalStatus.setText(state.linkerStatus());
        state.refreshLinkSlot();
        relayoutCurrent();
    }

    private void relayoutCurrent() {
        if (font == null) return;
        layoutRoot(theme());
        if (modal() != null) modal().layoutInScreen(font, theme(), width, height);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public Bounds jeiGuiBounds() { return state.panel.bounds(); }

    @Override
    protected void layoutRoot(UiTheme theme) {
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(1, width - 24));
        state.panel.setPreferredSize(panelWidth, null);
        var measured = state.panel.measure(font, theme, panelWidth, Math.max(1, height - 12));
        state.panel.layout(font, theme, new Bounds((width - measured.width()) / 2,
                (height - measured.height()) / 2, measured.width(), measured.height()));
    }

    private static String formatCoordinate(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    static Double parseBlockCoordinate(String value) {
        try {
            double parsed = Double.parseDouble(value.trim());
            return Double.isFinite(parsed) ? Math.floor(parsed) : null;
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    private static final class MutableLabel extends UiComponent {
        private Component text;
        private final int width;
        private final int height;

        private MutableLabel(Component text, int width, int height) {
            this.text = text;
            this.width = width;
            this.height = height;
            setPreferredSize(width, height);
        }

        private void setText(Component text) { this.text = text; }

        @Override
        protected UiSize measureIntrinsic(net.minecraft.client.gui.Font font, UiTheme theme,
                                          int maxWidth, int maxHeight) {
            return new UiSize(width, height).constrain(maxWidth, maxHeight);
        }

        @Override
        public void render(UiContext context) {
            UiPaint.text(context, text, bounds().x(), bounds().y(), context.theme().palette().text());
        }
    }

    static final class WaypointListItem extends UiComponent {
        private final String name;
        private final String coordinates;
        private final Runnable action;
        private boolean pressed;

        private WaypointListItem(String name, String coordinates, Runnable action) {
            this.name = name;
            this.coordinates = coordinates;
            this.action = action;
        }

        @Override
        protected UiSize measureIntrinsic(net.minecraft.client.gui.Font font, UiTheme theme,
                                          int maxWidth, int maxHeight) {
            var padding = theme.tokens().buttonPadding();
            int width = UiText.width(font, name, theme) + UiText.width(font, coordinates, theme)
                    + padding.left() + padding.right() + theme.tokens().smallGap();
            int height = Math.max(theme.tokens().buttonMinimumHeight(),
                    UiText.height(font, theme) + padding.top() + padding.bottom());
            return new UiSize(width, height).constrain(maxWidth, maxHeight);
        }

        @Override
        public void render(UiContext context) {
            boolean hovered = isEnabled() && bounds().contains(context.mouseX(), context.mouseY());
            UiPaint.button(context, bounds(), animate(context, "hover", hovered ? 1 : 0),
                    animate(context, "press", pressed ? 1 : 0), isEnabled(),
                    animate(context, "focus", isFocused() ? 1 : 0));
            var padding = context.theme().tokens().buttonPadding();
            int textY = bounds().centerY() - UiText.height(context) / 2;
            int availableTextWidth = Math.max(0, bounds().width() - padding.left() - padding.right()
                    - UiText.width(context, coordinates) - context.theme().tokens().smallGap());
            float scale = context.theme().tokens().typography().scale(
                    com.radaeli.ruilib.ui.UiTextStyle.BODY);
            String visibleName = context.font().plainSubstrByWidth(name,
                    scale <= 0 ? 0 : (int) (availableTextWidth / scale));
            int color = isEnabled() ? context.theme().palette().text() : context.theme().palette().mutedText();
            UiPaint.text(context, visibleName, bounds().x() + padding.left(), textY, color);
            UiPaint.text(context, coordinates,
                    bounds().right() - padding.right() - UiText.width(context, coordinates), textY, color);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isEnabled() || button != 0 || !bounds().contains(mouseX, mouseY)) return false;
            pressed = true;
            setFocused(true);
            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            if (!pressed || button != 0) return false;
            pressed = false;
            if (isEnabled() && bounds().contains(mouseX, mouseY)) action.run();
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!isEnabled() || !isFocused() || (keyCode != 257 && keyCode != 335 && keyCode != 32)) {
                return false;
            }
            action.run();
            return true;
        }

        @Override
        public boolean isFocusable() { return true; }
    }

    private static final class State {
        private final net.minecraft.core.BlockPos position;
        private java.util.UUID interactionToken;
        private final int column;
        private final int row;
        private FmsModuleData data;
        private String dimension;
        private double currentX;
        private double currentY;
        private double currentZ;
        private int navigatorDisplaySlot;
        private String navigatorBoundId;
        private UUID editingWaypoint;
        private FmsScreen screen;
        private Column waypointPage;
        private Column navigatorContent;
        private Column navigatorDetails;
        private ScrollView waypointList;
        private ScrollView targetPickerList;
        private Column form;
        private Panel panel;
        private TextField nameField;
        private TextField xField;
        private TextField yField;
        private TextField zField;
        private Button deleteButton;
        private Button targetButton;
        private MutableLabel targetStatus;
        private MutableLabel targetCoordinates;
        private MutableLabel targetDistance;
        private MutableLabel linkerStatus;
        private MutableLabel linkerModalStatus;
        private InventoryGrid inventory;
        private ItemSlot linkSlot;
        private Toggle rebindToggle;
        private boolean syncingLinkSlot;
        private int pendingLinkSlot = -1;
        private boolean pendingLinkIsNavigator;

        private State(FmsScreenPayload payload) {
            position = payload.position();
            interactionToken = payload.interactionToken();
            column = payload.column();
            row = payload.row();
            data = payload.data().copy();
            dimension = payload.dimension();
            currentX = payload.currentX();
            currentY = payload.currentY();
            currentZ = payload.currentZ();
            navigatorDisplaySlot = payload.navigatorDisplaySlot();
            navigatorBoundId = payload.navigatorBoundId();
        }

        private void beginCreate() {
            editingWaypoint = null;
            clearFormFields();
            deleteButton.setEnabled(false);
            openWaypointEditor("create_waypoint");
        }

        private void beginEdit(FmsWaypoint waypoint) {
            editingWaypoint = waypoint.id();
            fillForm(waypoint);
            deleteButton.setEnabled(true);
            openWaypointEditor("edit_waypoint");
        }

        private void openWaypointEditor(String titleKey) {
            screen.showModal(new Modal(Component.translatable(
                    "gui.aeronautics_fly_by_instruments.fms." + titleKey), form, 190, 160)
                    .setCloseOnOutsideClick(true));
        }

        private void fillForm(FmsWaypoint waypoint) {
            nameField.setValue(waypoint.name());
            xField.setValue(formatCoordinate(waypoint.x()));
            yField.setValue(formatCoordinate(waypoint.y()));
            zField.setValue(formatCoordinate(waypoint.z()));
        }

        private void clearFormFields() {
            nameField.setValue("");
            xField.setValue("");
            yField.setValue("");
            zField.setValue("");
        }

        private void closeForm() {
            editingWaypoint = null;
            clearFormFields();
            screen.closeModal();
        }

        private void useCurrentPosition() {
            xField.setValue(formatCoordinate(Math.floor(currentX)));
            yField.setValue(formatCoordinate(Math.floor(currentY)));
            zField.setValue(formatCoordinate(Math.floor(currentZ)));
        }

        private void saveWaypoint() {
            Double x = parseBlockCoordinate(xField.value());
            Double y = parseBlockCoordinate(yField.value());
            Double z = parseBlockCoordinate(zField.value());
            if (x == null || y == null || z == null || nameField.value().trim().isEmpty()) {
                message("invalid_waypoint");
                return;
            }
            String action = editingWaypoint == null ? "create" : "update";
            send(action, editingWaypoint == null ? "" : editingWaypoint.toString(), nameField.value(),
                    x, y, z, -1, false);
            screen.closeModal();
        }

        private void deleteWaypoint() {
            if (editingWaypoint != null) {
                send("delete", editingWaypoint.toString(), "", 0, 0, 0, -1, false);
                screen.closeModal();
            }
        }

        private void targetButtonPressed() {
            if (data.selectedWaypoint().isPresent()) clearTarget();
            else openTargetPicker();
        }

        private void openTargetPicker() {
            Column pickerContent = createTargetPicker(this);
            screen.showModal(new Modal(Component.translatable(
                    "gui.aeronautics_fly_by_instruments.fms.select_destination"), pickerContent, 220, 180)
                    .setCloseOnOutsideClick(true));
        }

        private void selectTarget(FmsWaypoint waypoint) {
            send("select", waypoint.id().toString(), "", 0, 0, 0, -1, false);
            screen.closeModal();
        }

        private void clearTarget() { send("clear_target", "", "", 0, 0, 0, -1, false); }

        private Component targetButtonLabel() {
            return Component.translatable(data.selectedWaypoint().isPresent()
                    ? "gui.aeronautics_fly_by_instruments.fms.clear_target"
                    : "gui.aeronautics_fly_by_instruments.fms.select_destination");
        }

        private void refreshTargetPresentation() {
            if (targetButton != null) targetButton.setLabel(targetButtonLabel());
            FmsWaypoint target = data.selectedWaypoint().orElse(null);
            if (target == null) {
                targetStatus.setText(Component.translatable(
                        "gui.aeronautics_fly_by_instruments.fms.target_none"));
                targetCoordinates.setText(Component.empty());
                targetDistance.setText(Component.empty());
                navigatorDetails.remove(targetCoordinates);
                navigatorDetails.remove(targetDistance);
                updateTargetActionOrder();
                return;
            }
            if (!navigatorDetails.children().contains(targetCoordinates)) {
                navigatorDetails.add(targetCoordinates);
                navigatorDetails.add(targetDistance);
            }
            targetStatus.setText(Component.translatable(
                    "gui.aeronautics_fly_by_instruments.fms.target_current", target.name()));
            targetCoordinates.setText(Component.translatable(
                    "gui.aeronautics_fly_by_instruments.fms.target_coordinates",
                    formatCoordinate(target.x()), formatCoordinate(target.y()),
                    formatCoordinate(target.z())));
            if (!dimension.equals(target.dimension())) {
                targetDistance.setText(Component.translatable(
                        "gui.aeronautics_fly_by_instruments.fms.target_other_dimension"));
            } else {
                double distance = Math.sqrt(square(target.x() - currentX) + square(target.y() - currentY)
                        + square(target.z() - currentZ));
                targetDistance.setText(Component.translatable(
                        "gui.aeronautics_fly_by_instruments.fms.target_distance",
                        String.format(Locale.ROOT, "%.1f", distance)));
            }
            updateTargetActionOrder();
        }

        private void updateTargetActionOrder() {
            if (navigatorContent == null || targetButton == null || navigatorDetails == null) return;
            navigatorContent.remove(targetButton);
            navigatorContent.remove(navigatorDetails);
            if (data.selectedWaypoint().isPresent()) {
                navigatorContent.add(navigatorDetails);
                navigatorContent.add(targetButton);
            } else {
                navigatorContent.add(targetButton);
                navigatorContent.add(navigatorDetails);
            }
        }

        private static double square(double value) { return value * value; }

        private void openLinker() {
            Column linkerContent = createLinkerModal(this);
            refreshLinkSlot();
            screen.showModal(new Modal(Component.translatable(
                    "gui.aeronautics_fly_by_instruments.fms.linker_title"), linkerContent, 180, 210)
                    .setCloseOnOutsideClick(true));
        }

        private void refreshLinkSlot() {
            if (linkSlot == null) return;
            syncingLinkSlot = true;
            try {
                linkSlot.setItemStack(navigatorDisplaySlot >= 0
                        ? new ItemStack(ModBlocks.FMS_NAVIGATOR.get()) : ItemStack.EMPTY);
            } finally {
                syncingLinkSlot = false;
            }
        }

        private void bindNavigator(int displaySlot) {
            if (displaySlot < 0 || displaySlot >= 36) return;
            send("bind_navigator", "", "", 0, 0, 0, displaySlot,
                    rebindToggle != null && rebindToggle.isSelected());
        }

        private Component linkerStatus() {
            if (navigatorDisplaySlot < 0) return Component.translatable(
                    "gui.aeronautics_fly_by_instruments.fms.linker_missing");
            if (data.id().toString().equals(navigatorBoundId)) return Component.translatable(
                    "gui.aeronautics_fly_by_instruments.fms.linker_connected");
            return navigatorBoundId.isBlank() ? Component.translatable(
                    "gui.aeronautics_fly_by_instruments.fms.linker_unbound") : Component.translatable(
                    "gui.aeronautics_fly_by_instruments.fms.linker_other_link");
        }

        private void send(String action, String waypointId, String name, double x, double y, double z,
                          int displaySlot, boolean confirmRebind) {
            PacketDistributor.sendToServer(new FmsActionPayload(position, column, row, action, waypointId,
                    name, x, y, z, displaySlot, confirmRebind, interactionToken));
        }

        private void message(String key) {
            var player = Minecraft.getInstance().player;
            if (player != null) player.displayClientMessage(Component.translatable(
                    "message.aeronautics_fly_by_instruments.fms." + key), true);
        }

    }
}
