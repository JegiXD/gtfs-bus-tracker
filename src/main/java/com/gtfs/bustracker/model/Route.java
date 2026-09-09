package com.gtfs.bustracker.model;

/**
 * Minimal representation of a GTFS route (routes.txt).
 */
public final class Route {

    private final String routeId;
    private final String shortName;
    private final String longName;

    public Route(String routeId, String shortName, String longName) {
        this.routeId = routeId;
        this.shortName = shortName;
        this.longName = longName;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getShortName() {
        return shortName;
    }

    public String getLongName() {
        return longName;
    }

    /**
     * The name that should be shown to the user: prefer the short name
     * (e.g. a line number like "101"), fall back to the long name, and
     * finally to the raw route id if neither is present.
     */
    public String displayName() {
        if (shortName != null && !shortName.isBlank()) {
            return shortName;
        }
        if (longName != null && !longName.isBlank()) {
            return longName;
        }
        return routeId;
    }

    @Override
    public String toString() {
        return "Route{id='" + routeId + "', display='" + displayName() + "'}";
    }
}
