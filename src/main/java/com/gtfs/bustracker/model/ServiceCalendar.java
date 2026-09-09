package com.gtfs.bustracker.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Dnevi v tednu in obdobje veljavnosti voznega reda iz calendar.txt. */
public final class ServiceCalendar {

    private final String serviceId;
    private final boolean[] daysActive; // Indeksi: 0 = ponedeljek, 6 = nedelja.
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

    /** Preveri, ali datum in dan v tednu ustrezata voznemu redu. */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(startDate) || date.isAfter(endDate)) {
            return false;
        }
        DayOfWeek dow = date.getDayOfWeek();
        return daysActive[dow.getValue() - 1];
    }
}
