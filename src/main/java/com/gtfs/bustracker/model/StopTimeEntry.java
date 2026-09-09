package com.gtfs.bustracker.model;

/**
 * A single stop_times.txt row for ONE specific stop: which trip stops
 * there, and at what (raw, possibly >= 24:00:00) second-of-day.
 *
 * Only entries whose stop_id matches the stop being queried are ever
 * materialized - see {@link com.gtfs.bustracker.gtfs.GtfsRepository}.
 */
public final class StopTimeEntry {

    private final String tripId;
    private final int arrivalSeconds; // seconds since midnight of the trip's service day; may exceed 86400
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
