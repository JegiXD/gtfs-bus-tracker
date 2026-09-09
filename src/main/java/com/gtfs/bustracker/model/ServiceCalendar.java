package com.gtfs.bustracker.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Minimal representation of a GTFS calendar.txt row: on which days of the
 * week, and within which date range, a service_id operates.
 */
public final class ServiceCalendar {

    private final String serviceId;
    private final boolean[] daysActive; // index 0 = Monday ... 6 = Sunday
    private final LocalDate startDate;
    private final LocalDate endDate;

    public ServiceCalendar(String serviceId, boolean[] daysActive, LocalDate startDate, LocalDate endDate) {
        if (daysActive.length != 7) {
            throw new IllegalArgumentException("daysActive must have exactly 7 entries (Mon..Sun)");
        }
        this.serviceId = serviceId;
        this.daysActive = daysActive;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getServiceId() {
        return serviceId;
    }

    /**
     * Whether this service operates on the given calendar date, i.e. the
     * date falls within [startDate, endDate] and the matching weekday flag
     * is set.
     */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(startDate) || date.isAfter(endDate)) {
            return false;
        }
        DayOfWeek dow = date.getDayOfWeek();
        return daysActive[dow.getValue() - 1];
    }
}
