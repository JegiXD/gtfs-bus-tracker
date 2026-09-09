package com.gtfs.bustracker.model;

/**
 * Minimal representation of a GTFS trip (trips.txt) - only the columns
 * needed to resolve "which route" and "which service calendar" a trip
 * belongs to.
 */
public final class TripInfo {

    private final String tripId;
    private final String routeId;
    private final String serviceId;
    private final String headsign;

    public TripInfo(String tripId, String routeId, String serviceId, String headsign) {
        this.tripId = tripId;
        this.routeId = routeId;
        this.serviceId = serviceId;
        this.headsign = headsign;
    }

    public String getTripId() {
        return tripId;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getHeadsign() {
        return headsign;
    }
}
