package com.gtfs.bustracker.model;

/** Osnovni podatki o liniji iz routes.txt. */
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

    /** Vrne kratko ime linije, sicer dolgo ime ali ID. */
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
