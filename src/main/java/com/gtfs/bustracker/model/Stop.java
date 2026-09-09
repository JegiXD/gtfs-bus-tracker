package com.gtfs.bustracker.model;

/** Osnovni podatki o postajališču iz stops.txt. */
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
