package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.PanelActionPayload;
import com.radaeli.aeronauticsflybyinstruments.network.PanelScreenPayload;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PanelScreenTest {
    private PanelScreenPayload payload() {
        return new PanelScreenPayload(BlockPos.ZERO,0,0,"buzzer","gpws","gpws.any",2,80,
                "test",.5,"connected",false,1,0,"",UUID.randomUUID());
    }
    private void invoke(PanelScreen screen, String method) throws Exception {
        var m=PanelScreen.class.getDeclaredMethod(method); m.setAccessible(true); m.invoke(screen);
    }
    @Test void saveAndRemovalSendExactlyOneDraft() throws Exception {
        var sent=new ArrayList<PanelActionPayload>();
        var p=payload(); var screen=new PanelScreen(p,sent::add,()->{});
        invoke(screen,"save"); screen.removed(); screen.removed();
        assertEquals(1,sent.size()); assertEquals("save",sent.getFirst().action());
        assertEquals(p.interactionToken(),sent.getFirst().interactionToken());
    }
    @Test void cancelNeverAppliesDraft() throws Exception {
        var sent=new ArrayList<PanelActionPayload>();
        var screen=new PanelScreen(payload(),sent::add,()->{});
        invoke(screen,"closeWithoutSaving"); screen.removed();
        assertTrue(sent.isEmpty());
    }
    @Test void ordinaryScreenRemovalAppliesValidDraftOnce() {
        var sent=new ArrayList<PanelActionPayload>();
        var screen=new PanelScreen(payload(),sent::add,()->{});
        screen.removed(); screen.removed();
        assertEquals(1,sent.size()); assertEquals("save",sent.getFirst().action());
    }
    @Test void testSoundSendsOnlyPreviewAndCancelDiscardsDraft() throws Exception {
        var sent=new ArrayList<PanelActionPayload>();
        var screen=new PanelScreen(payload(),sent::add,()->{});
        var send=PanelScreen.class.getDeclaredMethod("send",String.class); send.setAccessible(true);
        send.invoke(screen,"test_buzzer"); invoke(screen,"closeWithoutSaving"); screen.removed();
        assertEquals(1,sent.size()); assertEquals("test_buzzer",sent.getFirst().action());
        assertEquals(80,sent.getFirst().volume());
    }
}
