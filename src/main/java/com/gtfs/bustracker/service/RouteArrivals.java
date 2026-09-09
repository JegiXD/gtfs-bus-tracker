package com.gtfs.bustracker.service;

import com.gtfs.bustracker.model.Route;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The upcoming arrivals for a single route at the queried stop, already
 * sorted earliest-first and limited to the requested count.
 */
public final class RouteArrivals {

    private final Route route;
    private final List<LocalDateTime> arrivalTimes;

    public RouteArrivals(Route route, List<LocalDateTime> arrivalTimes) {
        this.route = route;
        this.arrivalTimes = arrivalTimes;
    }

    public Route getRoute() {
        return route;
    }

    public List<LocalDateTime> getArrivalTimes() {
        return arrivalTimes;
    }
}
