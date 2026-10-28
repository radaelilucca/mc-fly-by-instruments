package com.radaeli.aeronauticsflybyinstruments.network;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PanelPayloadTest {
    @Test void panelActionRoundTripsItsInstallationToken() {
        var p=new PanelActionPayload(BlockPos.ZERO,0,1,"indication","gpws","caution",2,42,"test",.5,UUID.randomUUID());
        var buffer=Unpooled.buffer();
        try { PanelActionPayload.STREAM_CODEC.encode(buffer,p);
            assertEquals(p,PanelActionPayload.STREAM_CODEC.decode(buffer)); assertEquals(0,buffer.readableBytes());
        } finally {buffer.release();}
    }
    @Test void panelScreenRoundTripsItsTableToken() {
        var p=new PanelScreenPayload(BlockPos.ZERO,-1,-1,"","",100,"ok",false,2,1,
                "lamp,0,0,warning,red",UUID.randomUUID());
        var buffer=Unpooled.buffer();
        try { PanelScreenPayload.STREAM_CODEC.encode(buffer,p);
            assertEquals(p,PanelScreenPayload.STREAM_CODEC.decode(buffer)); assertEquals(0,buffer.readableBytes());
        } finally {buffer.release();}
    }
    @Test void wrenchScrollRoundTripsTheHit() {
        var p=new PanelScrollPayload(BlockPos.ZERO.above(),net.minecraft.core.Direction.UP,
                new net.minecraft.world.phys.Vec3(.25,1,.75),-1);
        var buffer=Unpooled.buffer();
        try { PanelScrollPayload.STREAM_CODEC.encode(buffer,p);
            assertEquals(p,PanelScrollPayload.STREAM_CODEC.decode(buffer)); assertEquals(0,buffer.readableBytes());
        } finally {buffer.release();}
    }
    @Test void avionicsComputerScreenRoundTripsSensorsAndFunctions() {
        var p=new AvionicsComputerScreenPayload(BlockPos.ZERO,"caution","terrain","normal","cruise","connected",
                java.util.List.of(new AvionicsComputerScreenPayload.SensorLine("radar_altimeter","primary","ok",14.25,"")),
                java.util.List.of(new AvionicsComputerScreenPayload.FunctionLine("terrain","active","caution","")));
        var buffer=Unpooled.buffer();
        try { AvionicsComputerScreenPayload.STREAM_CODEC.encode(buffer,p);
            assertEquals(p,AvionicsComputerScreenPayload.STREAM_CODEC.decode(buffer)); assertEquals(0,buffer.readableBytes());
        } finally {buffer.release();}
    }
}
