package com.gtfs.bustracker.service;

import com.gtfs.bustracker.gtfs.GtfsRepository;
import com.gtfs.bustracker.model.Route;
import com.gtfs.bustracker.model.StopTimeEntry;
import com.gtfs.bustracker.model.TripInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Poišče prihode v naslednjih dveh urah. Upošteva tudi včerajšnje vožnje s časi nad 24:00:00. */
public final class NextArrivalsService {

    /** Koliko ur vnaprej iščemo prihode. */
    public static final long LOOKAHEAD_HOURS = 2;

    /**
     * Vrne do maxPerRoute prihodov na linijo v naslednjih dveh urah.
     * Linije uredi po najbližjem prihodu; prazne izpusti.
     * @param repository podatki izbranega postajališča
     * @param now čas poizvedbe
     * @param maxPerRoute največ prihodov na linijo; mora biti več kot 0
     * @return prihodi, združeni po linijah
     */
    public List<RouteArrivals> getNextArrivals(GtfsRepository repository, LocalDateTime now, int maxPerRoute) {
        if (maxPerRoute <= 0) {
            throw new IllegalArgumentException("maxPerRoute must be positive, got " + maxPerRoute);
        }

        long windowStartSec = now.toLocalTime().toSecondOfDay();
        long windowEndSec = windowStartSec + LOOKAHEAD_HOURS * 3600;

        LocalDate today = now.toLocalDate();
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime todayMidnight = today.atStartOfDay();
        LocalDateTime yesterdayMidnight = yesterday.atStartOfDay();

        // Zbrani prihodi po ID-ju linije, še brez razvrščanja in omejitve.
        Map<String, List<LocalDateTime>> arrivalsByRoute = new LinkedHashMap<>();

        for (StopTimeEntry entry : repository.getStopTimesAtStop()) {
            TripInfo trip = repository.getTripsById().get(entry.getTripId());
            if (trip == null || trip.getRouteId() == null) {
                continue; // Preskoči vožnjo brez podatkov o liniji.
            }

            LocalDateTime arrivalInstant = null;

            // Današnja vožnja.
            if (repository.isServiceActiveOn(trip.getServiceId(), today)
                    && entry.getArrivalSeconds() >= windowStartSec
                    && entry.getArrivalSeconds() <= windowEndSec) {
                arrivalInstant = todayMidnight.plusSeconds(entry.getArrivalSeconds());
            }
            // Včerajšnja vožnja s prihodom po polnoči.
            // Čas v GTFS je vsaj 24:00:00.
            else if (repository.isServiceActiveOn(trip.getServiceId(), yesterday)
                    && entry.getArrivalSeconds() >= windowStartSec + 86400
                    && entry.getArrivalSeconds() <= windowEndSec + 86400) {
                arrivalInstant = yesterdayMidnight.plusSeconds(entry.getArrivalSeconds());
            }

            if (arrivalInstant != null) {
                arrivalsByRoute.computeIfAbsent(trip.getRouteId(), k -> new ArrayList<>()).add(arrivalInstant);
            }
        }

        List<RouteArrivals> result = new ArrayList<>();
        for (Map.Entry<String, List<LocalDateTime>> e : arrivalsByRoute.entrySet()) {
            List<LocalDateTime> times = e.getValue();
            times.sort(Comparator.naturalOrder());
            List<LocalDateTime> limited = times.size() > maxPerRoute
                    ? new ArrayList<>(times.subList(0, maxPerRoute))
                    : times;
            Route route = repository.getRoutesById().get(e.getKey());
            if (route == null) {
                route = new Route(e.getKey(), null, null);
            }
            result.add(new RouteArrivals(route, limited));
        }

        // Linije uredi po najbližjem prihodu.
        result.sort(Comparator.comparing(ra -> ra.getArrivalTimes().get(0)));
        return result;
    }
}
