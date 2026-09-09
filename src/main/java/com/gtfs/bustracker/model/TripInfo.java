package com.gtfs.bustracker.model;

/** Povezuje vožnjo z linijo in voznim koledarjem iz trips.txt. */
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
