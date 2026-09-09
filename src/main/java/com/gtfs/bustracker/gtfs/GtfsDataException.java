package com.gtfs.bustracker.gtfs;

/** Napaka pri branju ali vsebini podatkov GTFS. */
public class GtfsDataException extends RuntimeException {

    public GtfsDataException(String message) {
        super(message);
    }

    public GtfsDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
