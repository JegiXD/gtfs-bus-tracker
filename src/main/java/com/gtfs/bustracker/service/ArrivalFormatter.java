package com.gtfs.bustracker.service;

import com.gtfs.bustracker.util.GtfsTimeUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/** Oblikuje izpis prihodov kot uro (12:10) ali čas do prihoda (10min). */
public final class ArrivalFormatter {

    public enum TimeFormat {
        ABSOLUTE, RELATIVE;

        public static TimeFormat parse(String raw) {
            if (raw == null) {
                throw new IllegalArgumentException("Time format must not be null");
            }
            String normalized = raw.trim().toLowerCase();
            return switch (normalized) {
                case "absolute" -> ABSOLUTE;
                case "relative" -> RELATIVE;
                default -> throw new IllegalArgumentException(
                        "Invalid time format '" + raw + "': expected 'relative' or 'absolute'");
            };
        }
    }

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    public String format(RouteArrivals routeArrivals, LocalDateTime now, TimeFormat format) {
        String times = routeArrivals.getArrivalTimes().stream()
                .map(t -> formatOne(t, now, format))
                .collect(Collectors.joining(", "));
        return routeArrivals.getRoute().displayName() + ": " + times;
    }

    public List<String> formatAll(List<RouteArrivals> routeArrivals, LocalDateTime now, TimeFormat format) {
        return routeArrivals.stream().map(ra -> format(ra, now, format)).collect(Collectors.toList());
    }

    private String formatOne(LocalDateTime arrival, LocalDateTime now, TimeFormat format) {
        if (format == TimeFormat.ABSOLUTE) {
            return arrival.toLocalTime().format(CLOCK);
        }
        long minutes = Duration.between(now, arrival).toMinutes();
        return GtfsTimeUtil.formatRelativeMinutes(minutes);
    }
}
