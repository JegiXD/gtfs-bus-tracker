package com.gtfs.bustracker.util;

/** Obdeluje čase GTFS, tudi ure nad 23 za vožnje po polnoči. */
public final class GtfsTimeUtil {

    private GtfsTimeUtil() {
    }

    /**
     * Pretvori čas GTFS v sekunde od polnoči. Ure nad 23 ohrani.
     * @throws IllegalArgumentException če čas ni veljaven
     */
    public static int parseToSecondsOfDay(String gtfsTime) {
        if (gtfsTime == null) {
            throw new IllegalArgumentException("GTFS time must not be null");
        }
        String trimmed = gtfsTime.trim();
        String[] parts = trimmed.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid GTFS time format: '" + gtfsTime + "'");
        }
        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            int seconds = Integer.parseInt(parts[2]);
            if (hours < 0 || minutes < 0 || minutes > 59 || seconds < 0 || seconds > 59) {
                throw new IllegalArgumentException("Invalid GTFS time format: '" + gtfsTime + "'");
            }
            return hours * 3600 + minutes * 60 + seconds;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid GTFS time format: '" + gtfsTime + "'", e);
        }
    }

    /** Oblikuje minute, npr. 7min. Negativne vrednosti prikaže kot 0min. */
    public static String formatRelativeMinutes(long minutes) {
        long clamped = Math.max(0, minutes);
        return clamped + "min";
    }
}
