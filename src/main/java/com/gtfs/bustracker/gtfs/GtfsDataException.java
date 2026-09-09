package com.gtfs.bustracker.gtfs;

/** Thrown when GTFS data cannot be loaded or is inconsistent/unusable. */
public class GtfsDataException extends RuntimeException {

    public GtfsDataException(String message) {
        super(message);
    }

    public GtfsDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
