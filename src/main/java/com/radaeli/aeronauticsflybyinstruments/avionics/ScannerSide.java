package com.radaeli.aeronauticsflybyinstruments.avionics;

import net.minecraft.core.Direction;

import java.util.Locale;
import java.util.Optional;

/**
 * The side of the aircraft a terrain scanner looks at, set by the player in the scanner's own
 * screen. Nothing is inferred from block facings: until the player picks a side the scanner has no
 * role, and the scanners with a side are what tell the Avionics Computer where the nose is.
 */
public enum ScannerSide {
    UNSET, FRONT, REAR, LEFT, RIGHT;

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public boolean set() { return this != UNSET; }

    /**
     * Nose of the aircraft in the sublevel's block frame, from the direction a scanner looks
     * ({@code scannerFacing}) and the side the player said that is. Empty while unset.
     */
    public Optional<Direction> nose(Direction scannerFacing) {
        return switch (this) {
            case FRONT -> Optional.of(scannerFacing);
            case REAR -> Optional.of(scannerFacing.getOpposite());
            case LEFT -> Optional.of(scannerFacing.getClockWise());
            case RIGHT -> Optional.of(scannerFacing.getCounterClockWise());
            case UNSET -> Optional.empty();
        };
    }

    public static Optional<ScannerSide> parse(String id) {
        for (ScannerSide side : values()) {
            if (side.id().equals(id)) return Optional.of(side);
        }
        return Optional.empty();
    }
}
