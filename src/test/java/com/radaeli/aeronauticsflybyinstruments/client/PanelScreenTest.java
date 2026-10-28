package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.PanelActionPayload;
import com.radaeli.aeronauticsflybyinstruments.network.PanelScreenPayload;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PanelScreenTest {
    private PanelScreenPayload buzzer() {
        return new PanelScreenPayload(BlockPos.ZERO,0,0,"buzzer","caution",80,"ok",false,1,0,"",UUID.randomUUID());
    }
    @Test void choosingAnIndicationAppliesImmediatelyWithTheModuleToken() {
        var sent=new ArrayList<PanelActionPayload>();
        var p=buzzer(); var screen=new PanelScreen(p,sent::add,()->{});
        screen.chooseIndication("warning");
        assertEquals(1,sent.size()); assertEquals("indication",sent.getFirst().action());
        assertEquals("warning",sent.getFirst().channel());
        assertEquals(p.interactionToken(),sent.getFirst().interactionToken());
    }
    @Test void volumeIsSentOnceWhenTheScreenCloses() {
        var sent=new ArrayList<PanelActionPayload>();
        var screen=new PanelScreen(buzzer(),sent::add,()->{});
        screen.stepVolume(10); screen.stepVolume(10); screen.stepVolume(10);
        assertTrue(sent.isEmpty());
        screen.removed(); screen.removed();
        assertEquals(1,sent.size()); assertEquals("volume",sent.getFirst().action());
        assertEquals(100,sent.getFirst().volume());
    }
    @Test void unchangedVolumeSendsNothing() {
        var sent=new ArrayList<PanelActionPayload>();
        var screen=new PanelScreen(buzzer(),sent::add,()->{});
        screen.removed();
        assertTrue(sent.isEmpty());
    }
    @Test void testSoundPreviewsTheCurrentVolume() {
        var sent=new ArrayList<PanelActionPayload>();
        var screen=new PanelScreen(buzzer(),sent::add,()->{});
        screen.stepVolume(-30); screen.send("test_buzzer");
        assertEquals("test_buzzer",sent.getFirst().action()); assertEquals(50,sent.getFirst().volume());
    }
}
