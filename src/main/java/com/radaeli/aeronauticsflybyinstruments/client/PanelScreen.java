package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.PanelActionPayload;
import com.radaeli.aeronauticsflybyinstruments.network.PanelScreenPayload;
import com.radaeli.aeronauticsflybyinstruments.panel.LampIndication;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Consumer;

/**
 * Table screens (plan §5.2–5.3): the general panel (computer state, lit lamps, test and silence)
 * and one module (what it indicates; buzzers also volume). Every choice applies immediately.
 */
public final class PanelScreen extends HudScreen {
    private static final String KEY = "gui.aeronautics_fly_by_instruments.panel.";
    private static final int WIDTH = 220;
    private final PanelScreenPayload payload;
    private final Consumer<PanelActionPayload> sender;
    private final Runnable closeAction;
    private int volume;
    private int sentVolume;

    public PanelScreen(PanelScreenPayload payload) {
        this(payload, PacketDistributor::sendToServer, null);
    }

    PanelScreen(PanelScreenPayload payload, Consumer<PanelActionPayload> sender, Runnable closeAction) {
        super(Component.translatable(KEY + "title"));
        this.payload = payload;
        this.sender = sender;
        this.closeAction = closeAction;
        volume = sentVolume = payload.volume();
    }

    boolean general() { return payload.column() < 0; }

    void send(String action) { send(action, ""); }

    void send(String action, String indication) {
        sender.accept(new PanelActionPayload(payload.position(), payload.column(), payload.row(), action, "gpws",
                indication, 2, volume, "test", 0.5, payload.interactionToken()));
    }

    void chooseIndication(String indication) { send("indication", indication); }

    void stepVolume(int delta) {
        volume = Math.clamp(volume + delta, 0, 100);
    }

    /** Volume goes to the server once, when the screen closes, not on every step. */
    @Override
    public void removed() {
        if (!general() && payload.moduleType().equals("buzzer") && volume != sentVolume) {
            sentVolume = volume;
            send("volume");
        }
        super.removed();
    }

    @Override
    public void onClose() {
        if (closeAction != null) closeAction.run();
        else super.onClose();
    }

    @Override
    protected HudPanel build() {
        return general() ? buildGeneral() : buildModule();
    }

    private HudPanel buildGeneral() {
        var palette = HudPanel.palette();
        String status = payload.status();
        int statusColor = status.equals("ok") ? palette.success() : palette.warning();
        HudPanel panel = new HudPanel(WIDTH).accent(statusColor)
                .header(tr(KEY + "title"), tr(KEY + "status." + status))
                .row(tr(KEY + "modules"), tr(KEY + "modules_value", payload.moduleCount(), payload.activeCount()))
                .row(tr(KEY + "buzzers"), tr(payload.muted() ? KEY + "buzzers_muted" : KEY + "buzzers_on"),
                        payload.muted() ? palette.warning() : palette.text(), null);
        List<String> lit = payload.litSummary().isBlank() ? List.of() : List.of(payload.litSummary().split(";"));
        if (!lit.isEmpty()) {
            panel.section(tr(KEY + "lit"));
            for (String entry : lit) {
                String[] data = entry.split(",", 5);
                if (data.length != 5) continue;
                panel.row(tr(KEY + "module." + data[0]) + " " + (Integer.parseInt(data[1]) + 1) + ", "
                                + (Integer.parseInt(data[2]) + 1), indicationLabel(data[3]),
                        data[4].equals("red") ? palette.error() : palette.warning(), null);
            }
        }
        panel.buttons(List.of(tr(KEY + "test"), tr(payload.muted() ? KEY + "unmute" : KEY + "mute")),
                List.of(() -> send("test"), () -> send(payload.muted() ? "unmute" : "mute")));
        return panel;
    }

    private HudPanel buildModule() {
        var palette = HudPanel.palette();
        boolean buzzer = payload.moduleType().equals("buzzer");
        boolean active = payload.activeCount() > 0;
        String title = tr(KEY + "module." + payload.moduleType()) + " " + (payload.column() + 1) + ", "
                + (payload.row() + 1);
        HudPanel panel = new HudPanel(WIDTH)
                .accent(colorOf(payload.indication()))
                .header(title, tr(active ? KEY + "state_on" : KEY + "state_off"));
        panel.section(tr(buzzer ? KEY + "sounds_when" : KEY + "lights_when"));
        String current = payload.indication();
        if (current.equals(LampIndication.TEST)) {
            panel.choice(tr(KEY + "indication.test"), tr(KEY + "lua"), true, true, null);
        }
        for (String choice : LampIndication.choices(buzzer ? "buzzer" : "lamp")) {
            if (choice.startsWith(LampIndication.FUNCTION) && choice.equals(LampIndication.functions().getFirst())) {
                panel.section(tr(KEY + "single_function"));
            }
            boolean available = LampIndication.available(choice);
            String note = !available ? tr(KEY + "soon") : buzzer ? "" : colorNote(choice);
            panel.choice(indicationLabel(choice), note, choice.equals(current), available,
                    () -> chooseIndication(choice));
        }
        if (buzzer) {
            panel.section(tr(KEY + "sound"));
            panel.stepper(tr(KEY + "volume"), volume + "%", () -> stepVolume(-10), () -> stepVolume(10));
            panel.buttons(List.of(tr(KEY + "test_sound")), List.of(() -> send("test_buzzer")));
        }
        return panel;
    }

    private String colorNote(String indication) {
        if (indication.startsWith(LampIndication.FUNCTION)) return tr(KEY + "color.by_severity");
        return tr(KEY + "color." + (LampIndication.color(indication, 0) == LampIndication.Color.RED ? "red" : "amber"));
    }

    private static int colorOf(String indication) {
        var palette = HudPanel.palette();
        return LampIndication.color(indication, 0) == LampIndication.Color.RED ? palette.error() : palette.warning();
    }

    static String indicationLabel(String indication) {
        if (indication.startsWith(LampIndication.FUNCTION)) {
            return tr("gui.aeronautics_fly_by_instruments.avionics_computer.function."
                    + indication.substring(LampIndication.FUNCTION.length()));
        }
        return tr(KEY + "indication." + indication);
    }
}
