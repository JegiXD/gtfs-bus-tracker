package com.gtfs.bustracker.gtfs;

import com.gtfs.bustracker.model.Route;
import com.gtfs.bustracker.model.ServiceCalendar;
import com.gtfs.bustracker.model.Stop;
import com.gtfs.bustracker.model.StopTimeEntry;
import com.gtfs.bustracker.model.TripInfo;
import com.gtfs.bustracker.util.GtfsTimeUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loads only the slice of a GTFS feed that is relevant to answering "next
 * arrivals" queries for ONE specific stop.
 *
 * <p>Memory strategy: the feed's largest files (stop_times.txt, trips.txt)
 * are streamed line-by-line and immediately filtered - a row is only kept
 * in memory if it is reachable from the requested stop_id. routes.txt and
 * calendar.txt are then filtered down to just the route/service ids that
 * survived that first pass. Nothing about stops other than the requested
 * one, or trips that never call at it, is ever retained.</p>
 */
public final class GtfsRepository {

    private final Stop stop;
    private final List<StopTimeEntry> stopTimesAtStop;
    private final Map<String, TripInfo> tripsById;
    private final Map<String, Route> routesById;
    private final Map<String, ServiceCalendar> calendarsByServiceId;

    private GtfsRepository(Stop stop,
                            List<StopTimeEntry> stopTimesAtStop,
                            Map<String, TripInfo> tripsById,
                            Map<String, Route> routesById,
                            Map<String, ServiceCalendar> calendarsByServiceId) {
        this.stop = stop;
        this.stopTimesAtStop = stopTimesAtStop;
        this.tripsById = tripsById;
        this.routesById = routesById;
        this.calendarsByServiceId = calendarsByServiceId;
    }

    /**
     * Loads and filters the GTFS feed located in {@code gtfsDir} for the
     * given stop id.
     *
     * @throws GtfsDataException if required files are missing/unreadable,
     *                            or the stop id does not exist in stops.txt
     */
    public static GtfsRepository loadForStop(Path gtfsDir, int stopId) {
        Stop stop = loadStop(gtfsDir, stopId);

        List<StopTimeEntry> stopTimes = new ArrayList<>();
        Set<String> neededTripIds = new HashSet<>();
        loadStopTimesForStop(gtfsDir, stopId, stopTimes, neededTripIds);

        Map<String, TripInfo> trips = new HashMap<>();
        Set<String> neededRouteIds = new HashSet<>();
        Set<String> neededServiceIds = new HashSet<>();
        loadTrips(gtfsDir, neededTripIds, trips, neededRouteIds, neededServiceIds);

        Map<String, Route> routes = loadRoutes(gtfsDir, neededRouteIds);
        Map<String, ServiceCalendar> calendars = loadCalendars(gtfsDir, neededServiceIds);

        return new GtfsRepository(stop, stopTimes, trips, routes, calendars);
    }

    private static Stop loadStop(Path gtfsDir, int stopId) {
        Path file = gtfsDir.resolve("stops.txt");
        requireFile(file, "stops.txt");
        String stopIdStr = Integer.toString(stopId);
        Stop[] found = new Stop[1];
        try (GtfsCsvTable table = GtfsCsvTable.open(file)) {
            table.forEachRow(row -> {
                if (found[0] != null) {
                    return;
                }
                if (stopIdStr.equals(row.get("stop_id"))) {
                    String name = row.get("stop_name");
                    found[0] = new Stop(stopId, name != null ? name : ("Stop " + stopId));
                }
            });
        } catch (IOException e) {
            throw new GtfsDataException("Failed to read stops.txt", e);
        }
        if (found[0] == null) {
            throw new GtfsDataException("No stop found with stop_id=" + stopId);
        }
        return found[0];
    }

    private static void loadStopTimesForStop(Path gtfsDir, int stopId,
                                              List<StopTimeEntry> out,
                                              Set<String> neededTripIdsOut) {
        Path file = gtfsDir.resolve("stop_times.txt");
        requireFile(file, "stop_times.txt");
        String stopIdStr = Integer.toString(stopId);
        try (GtfsCsvTable table = GtfsCsvTable.open(file)) {
            table.forEachRow(row -> {
                if (!stopIdStr.equals(row.get("stop_id"))) {
                    return;
                }
                String tripId = row.get("trip_id");
                String arrival = row.get("arrival_time");
                if (tripId == null || arrival == null) {
                    return; // malformed row - skip defensively
                }
                int arrivalSeconds;
                try {
                    arrivalSeconds = GtfsTimeUtil.parseToSecondsOfDay(arrival);
                } catch (IllegalArgumentException e) {
                    return; // skip rows with unparsable times rather than fail the whole load
                }
                String seqStr = row.get("stop_sequence");
                int sequence = seqStr != null ? safeParseInt(seqStr, 0) : 0;
                out.add(new StopTimeEntry(tripId, arrivalSeconds, sequence));
                neededTripIdsOut.add(tripId);
            });
        } catch (IOException e) {
            throw new GtfsDataException("Failed to read stop_times.txt", e);
        }
    }

    private static void loadTrips(Path gtfsDir, Set<String> neededTripIds,
                                   Map<String, TripInfo> tripsOut,
                                   Set<String> neededRouteIdsOut,
                                   Set<String> neededServiceIdsOut) {
        Path file = gtfsDir.resolve("trips.txt");
        requireFile(file, "trips.txt");
        try (GtfsCsvTable table = GtfsCsvTable.open(file)) {
            table.forEachRow(row -> {
                String tripId = row.get("trip_id");
                if (tripId == null || !neededTripIds.contains(tripId)) {
                    return;
                }
                String routeId = row.get("route_id");
                String serviceId = row.get("service_id");
                String headsign = row.get("trip_headsign");
                tripsOut.put(tripId, new TripInfo(tripId, routeId, serviceId, headsign));
                if (routeId != null) {
                    neededRouteIdsOut.add(routeId);
                }
                if (serviceId != null) {
                    neededServiceIdsOut.add(serviceId);
                }
            });
        } catch (IOException e) {
            throw new GtfsDataException("Failed to read trips.txt", e);
        }
    }

    private static Map<String, Route> loadRoutes(Path gtfsDir, Set<String> neededRouteIds) {
        Path file = gtfsDir.resolve("routes.txt");
        requireFile(file, "routes.txt");
        Map<String, Route> routes = new HashMap<>();
        try (GtfsCsvTable table = GtfsCsvTable.open(file)) {
            table.forEachRow(row -> {
                String routeId = row.get("route_id");
                if (routeId == null || !neededRouteIds.contains(routeId)) {
                    return;
                }
                routes.put(routeId, new Route(routeId, row.get("route_short_name"), row.get("route_long_name")));
            });
        } catch (IOException e) {
            throw new GtfsDataException("Failed to read routes.txt", e);
        }
        return routes;
    }

    /**
     * calendar.txt is optional per the assignment; if it is absent, every
     * service id referenced by a relevant trip is treated as active every
     * day (calendar_dates.txt exceptions are out of scope here).
     */
    private static Map<String, ServiceCalendar> loadCalendars(Path gtfsDir, Set<String> neededServiceIds) {
        Map<String, ServiceCalendar> calendars = new HashMap<>();
        Path file = gtfsDir.resolve("calendar.txt");
        if (!Files.exists(file)) {
            return calendars;
        }
        try (GtfsCsvTable table = GtfsCsvTable.open(file)) {
            table.forEachRow(row -> {
                String serviceId = row.get("service_id");
                if (serviceId == null || !neededServiceIds.contains(serviceId)) {
                    return;
                }
                boolean[] days = new boolean[]{
                        "1".equals(row.get("monday")),
                        "1".equals(row.get("tuesday")),
                        "1".equals(row.get("wednesday")),
                        "1".equals(row.get("thursday")),
                        "1".equals(row.get("friday")),
                        "1".equals(row.get("saturday")),
                        "1".equals(row.get("sunday")),
                };
                LocalDate start = parseDate(row.get("start_date"));
                LocalDate end = parseDate(row.get("end_date"));
                if (start == null || end == null) {
                    return;
                }
                calendars.put(serviceId, new ServiceCalendar(serviceId, days, start, end));
            });
        } catch (IOException e) {
            throw new GtfsDataException("Failed to read calendar.txt", e);
        }
        return calendars;
    }

    private static LocalDate parseDate(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) {
            return null;
        }
        try {
            int year = Integer.parseInt(yyyymmdd.substring(0, 4));
            int month = Integer.parseInt(yyyymmdd.substring(4, 6));
            int day = Integer.parseInt(yyyymmdd.substring(6, 8));
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            return null;
        }
    }

    private static int safeParseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void requireFile(Path file, String name) {
        if (!Files.exists(file)) {
            throw new GtfsDataException("Required GTFS file not found: " + name + " (looked at " + file + ")");
        }
    }

    /**
     * Builds a repository directly from in-memory data, bypassing file I/O.
     * Intended for unit tests that want to exercise {@code NextArrivalsService}
     * against hand-crafted fixtures without touching the filesystem.
     */
    public static GtfsRepository forData(Stop stop,
                                          List<StopTimeEntry> stopTimesAtStop,
                                          Map<String, TripInfo> tripsById,
                                          Map<String, Route> routesById,
                                          Map<String, ServiceCalendar> calendarsByServiceId) {
        return new GtfsRepository(stop, stopTimesAtStop, tripsById, routesById, calendarsByServiceId);
    }

    public Stop getStop() {
        return stop;
    }

    public List<StopTimeEntry> getStopTimesAtStop() {
        return stopTimesAtStop;
    }

    public Map<String, TripInfo> getTripsById() {
        return tripsById;
    }

    public Map<String, Route> getRoutesById() {
        return routesById;
    }

    public Map<String, ServiceCalendar> getCalendarsByServiceId() {
        return calendarsByServiceId;
    }

    /**
     * Whether the given service id is active on the given date. If no
     * calendar entry exists for the service (either calendar.txt was
     * absent, or the service id was unlisted), it is conservatively treated
     * as active - this matches the "calendar.txt is optional" contract.
     */
    public boolean isServiceActiveOn(String serviceId, LocalDate date) {
        ServiceCalendar cal = calendarsByServiceId.get(serviceId);
        if (cal == null) {
            return true;
        }
        return cal.isActiveOn(date);
    }
}
