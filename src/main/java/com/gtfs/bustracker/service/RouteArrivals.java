package com.gtfs.bustracker.service;

import com.gtfs.bustracker.model.Route;

import java.time.LocalDateTime;
import java.util.List;

/** Prihodi ene linije, urejeni po času in omejeni na zahtevano število. */
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
