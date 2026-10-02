package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.radaeli.aeronauticsflybyinstruments.avionics.ReadingStatus;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RadarAltimeterScanTest {
    @Test
    void definesNineSymmetricCastsAroundTheLocalLowerAxis() {
        var directions = RadarAltimeterScan.directions();
        assertEquals(9, directions.size());
        assertEquals(new RadarAltimeterScan.Direction(0.0, -1.0, 0.0), directions.getFirst());
        double expectedY = -Math.cos(Math.toRadians(20.0));
        for (int index = 1; index < directions.size(); index++) {
            var direction = directions.get(index);
            assertEquals(expectedY, direction.y(), 1.0E-12);
            assertEquals(1.0, Math.sqrt(direction.x() * direction.x() + direction.y() * direction.y()
                    + direction.z() * direction.z()), 1.0E-12);
        }
        assertEquals(0.0, directions.stream().mapToDouble(RadarAltimeterScan.Direction::x).sum(), 1.0E-12);
        assertEquals(0.0, directions.stream().mapToDouble(RadarAltimeterScan.Direction::z).sum(), 1.0E-12);
    }

    @Test
    void requiresThreeValidReturnsAndUsesMedianOfThem() {
        var insufficient = RadarAltimeterScan.aggregate(List.of(10.0, 2.0), 25);
        assertEquals(ReadingStatus.UNAVAILABLE, insufficient.status());
        assertTrue(insufficient.value().isEmpty());

        var median = RadarAltimeterScan.aggregate(List.of(10.0, 2.0, 6.0, Double.NaN, -1.0), 30);
        assertEquals(ReadingStatus.VALID, median.status());
        assertEquals(6.0, median.value().orElseThrow());
        assertEquals("blocks", median.unit());
        assertEquals(30, median.gameTime());
    }

    @Test
    void evenNumberOfValidReturnsUsesMiddlePairAverage() {
        var median = RadarAltimeterScan.aggregate(List.of(8.0, 2.0, 6.0, 4.0), 31);
        assertEquals(5.0, median.value().orElseThrow());
    }
}
