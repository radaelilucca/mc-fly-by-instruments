package com.radaeli.aeronauticsflybyinstruments.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PanelPayloadTest {
    @Test void panelActionRoundTripsItsInstallationToken() {
        var p=new PanelActionPayload(BlockPos.ZERO,0,1,"save","gpws","gpws.any",2,42,"test",.5,UUID.randomUUID());
        var buffer=Unpooled.buffer();
        try { PanelActionPayload.STREAM_CODEC.encode(buffer,p);
            assertEquals(p,PanelActionPayload.STREAM_CODEC.decode(buffer)); assertEquals(0,buffer.readableBytes());
        } finally {buffer.release();}
    }
    @Test void panelScreenRoundTripsItsTableToken() {
        var p=new PanelScreenPayload(BlockPos.ZERO,-1,-1,"","gpws","gpws.any",2,100,"test",.5,
                "connected",false,2,1,"buzzer,0,0,true",UUID.randomUUID());
        var buffer=Unpooled.buffer();
        try { PanelScreenPayload.STREAM_CODEC.encode(buffer,p);
            assertEquals(p,PanelScreenPayload.STREAM_CODEC.decode(buffer)); assertEquals(0,buffer.readableBytes());
        } finally {buffer.release();}
    }
}
