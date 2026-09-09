package com.gtfs.bustracker.model;

/** Vožnja in čas prihoda na izbrano postajališče iz stop_times.txt. */
public final class StopTimeEntry {

    private final String tripId;
    private final int arrivalSeconds; // Sekunde od polnoči dneva vožnje; lahko presegajo 86400.
    private final int stopSequence;

    public StopTimeEntry(String tripId, int arrivalSeconds, int stopSequence) {
        this.tripId = tripId;
        this.arrivalSeconds = arrivalSeconds;
        this.stopSequence = stopSequence;
    }

    public String getTripId() {
        return tripId;
    }

    public int getArrivalSeconds() {
        return arrivalSeconds;
    }

    public int getStopSequence() {
        return stopSequence;
    }
}
