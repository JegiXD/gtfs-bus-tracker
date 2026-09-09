package com.gtfs.bustracker.model;

/**
 * Minimal representation of a GTFS stop (stops.txt).
 * Only the fields the application actually needs are kept, on purpose,
 * to avoid retaining the full stops.txt row in memory.
 */
public final class Stop {

    private final int stopId;
    private final String name;

    public Stop(int stopId, String name) {
        this.stopId = stopId;
        this.name = name;
    }

    public int getStopId() {
        return stopId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Stop{id=" + stopId + ", name='" + name + "'}";
    }
}
