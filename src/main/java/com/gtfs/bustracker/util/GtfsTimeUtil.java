package com.gtfs.bustracker.util;

/**
 * Helpers for GTFS's slightly unusual time format: "HH:MM:SS" where HH may
 * legally go beyond 23 (e.g. "25:30:00") to represent a time after midnight
 * that still belongs to the previous day's service/trip block.
 */
public final class GtfsTimeUtil {

    private GtfsTimeUtil() {
    }

    /**
     * Parses a GTFS time string such as "08:15:00" or "25:30:00" into the
     * number of seconds since midnight of the service day. Values of 24:00:00
     * and beyond are preserved as-is (not wrapped), which is exactly what
     * GTFS intends.
     *
     * @throws IllegalArgumentException if the string is not a valid GTFS time
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

    /**
     * Formats a number of minutes as a short relative duration, e.g. "0min",
     * "7min". Negative values are clamped to 0.
     */
    public static String formatRelativeMinutes(long minutes) {
        long clamped = Math.max(0, minutes);
        return clamped + "min";
    }
}
