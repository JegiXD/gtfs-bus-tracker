package com.gtfs.bustracker;

import com.gtfs.bustracker.gtfs.GtfsDataException;
import com.gtfs.bustracker.gtfs.GtfsRepository;
import com.gtfs.bustracker.model.Stop;
import com.gtfs.bustracker.service.ArrivalFormatter;
import com.gtfs.bustracker.service.NextArrivalsService;
import com.gtfs.bustracker.service.RouteArrivals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


public final class Main {

    private static final String DEFAULT_STATION_ID = "2";
    private static final String DEFAULT_BUSES_PER_LINE = "2";
    private static final String DEFAULT_FORMAT = "relative";

    private Main() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (UsageException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            System.err.println(usage());
            System.exit(2);
        } catch (GtfsDataException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    static void run(String[] args) {
        if (args.length == 0) {
            args = new String[]{
                    DEFAULT_STATION_ID,
                    DEFAULT_BUSES_PER_LINE,
                    DEFAULT_FORMAT
            };
        }
        if (args.length < 3) {
            throw new UsageException("Expected at least 3 arguments, got " + args.length);
        }

        int stationId = parseStationId(args[0]);
        int numBusesPerLine = parseNumBuses(args[1]);
        ArrivalFormatter.TimeFormat format = parseFormat(args[2]);

        Path gtfsDir = defaultGtfsDir();
        LocalDateTime now = LocalDateTime.now();

        int i = 3;
        while (i < args.length) {
            String flag = args[i];
            if (i + 1 >= args.length) {
                throw new UsageException("Missing value for argument: " + flag);
            }
            switch (flag) {
                case "--gtfs" -> gtfsDir = Paths.get(args[i + 1]);
                case "--at" -> now = LocalDateTime.parse(args[i + 1]);
                default -> throw new UsageException("Unknown argument: " + flag);
            }
            i += 2;
        }

        GtfsRepository repository = GtfsRepository.loadForStop(gtfsDir, stationId);
        NextArrivalsService service = new NextArrivalsService();
        List<RouteArrivals> arrivals = service.getNextArrivals(repository, now, numBusesPerLine);

        printResults(repository.getStop(), now, arrivals, format);
    }

    private static void printResults(Stop stop, LocalDateTime now, List<RouteArrivals> arrivals,
                                      ArrivalFormatter.TimeFormat format) {
        System.out.println("Upcoming buses at " + stop.getName() + " (stop " + stop.getStopId() + ")");
        System.out.println("Query time: " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + " | next " + NextArrivalsService.LOOKAHEAD_HOURS + "h");
        if (arrivals.isEmpty()) {
            System.out.println("No upcoming buses in the next " + NextArrivalsService.LOOKAHEAD_HOURS + " hours.");
            return;
        }
        ArrivalFormatter formatter = new ArrivalFormatter();
        for (String line : formatter.formatAll(arrivals, now, format)) {
            System.out.println(line);
        }
    }

    private static int parseStationId(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new UsageException("station_id must be an integer, got '" + raw + "'");
        }
    }

    private static int parseNumBuses(String raw) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) {
                throw new UsageException("num_buses_per_line must be a positive integer, got " + value);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new UsageException("num_buses_per_line must be an integer, got '" + raw + "'");
        }
    }

    private static ArrivalFormatter.TimeFormat parseFormat(String raw) {
        try {
            return ArrivalFormatter.TimeFormat.parse(raw);
        } catch (IllegalArgumentException e) {
            throw new UsageException(e.getMessage());
        }
    }

    private static Path defaultGtfsDir() {
        String env = System.getenv("GTFS_DATA_DIR");
        return Paths.get(env != null ? env : "gtfs-data");
    }

    private static String usage() {
        return "Usage: busTrips <station_id> <num_buses_per_line> <relative|absolute> "
                + "[--gtfs <dir>] [--at <yyyy-MM-ddTHH:mm>]";
    }

    /** Napaka pri argumentih programa. */
    private static final class UsageException extends RuntimeException {
        UsageException(String message) {
            super(message);
        }
    }
}
