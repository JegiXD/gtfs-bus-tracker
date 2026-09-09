package com.gtfs.bustracker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration test: invokes the same {@code Main.run} entry
 * point the packaged jar uses, against the real bundled GTFS feed on disk,
 * and checks the exact console output for a known stop/time.
 *
 * The reference date (2 March 2020) is chosen deliberately: it falls
 * inside the sample feed's calendar.txt validity window
 * (2020-02-15 .. 2020-05-15), so results are stable and reproducible.
 */
class BusTripsCliIT {

    private Path gtfsDir;
    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void setUp() throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("gtfs-fixture/stops.txt");
        assertTrue(resource != null, "gtfs-fixture test resources not found on classpath");
        gtfsDir = Paths.get(resource.toURI()).getParent();

        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void printsNextArrivalsInRelativeFormat() {
        Main.run(new String[]{
                "2", "2", "relative",
                "--gtfs", gtfsDir.toString(),
                "--at", "2020-03-02T06:00:00"
        });

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        String[] lines = output.strip().split("\\R");

        assertEquals("Upcoming buses at AL Masjid Al-nabawi (Clock Roundabout) (stop 2)", lines[0]);
        assertEquals("Query time: 2020-03-02 06:00 | next 2h", lines[1]);
        assertEquals("101: 9min, 20min", lines[2]);
        assertEquals("107: 11min, 20min", lines[3]);
        assertEquals("106: 20min, 21min", lines[4]);
        assertEquals(5, lines.length);
    }

    @Test
    void printsNextArrivalsInAbsoluteFormat() {
        Main.run(new String[]{
                "2", "2", "absolute",
                "--gtfs", gtfsDir.toString(),
                "--at", "2020-03-02T06:00:00"
        });

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        String[] lines = output.strip().split("\\R");

        assertEquals("101: 06:09, 06:20", lines[2]);
        assertEquals("107: 06:11, 06:20", lines[3]);
        assertEquals("106: 06:20, 06:21", lines[4]);
    }

    @Test
    void respectsNumBusesPerLineLimit() {
        Main.run(new String[]{
                "2", "1", "absolute",
                "--gtfs", gtfsDir.toString(),
                "--at", "2020-03-02T06:00:00"
        });

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        String[] lines = output.strip().split("\\R");

        assertEquals("101: 06:09", lines[2]);
        assertEquals("107: 06:11", lines[3]);
        assertEquals("106: 06:20", lines[4]);
    }

    @Test
    void reportsNoBusesOutsideServiceWindow() {
        // Well past midnight with nothing scheduled in the next 2h for this feed.
        Main.run(new String[]{
                "2", "3", "absolute",
                "--gtfs", gtfsDir.toString(),
                "--at", "2020-03-02T02:00:00"
        });

        String output = capturedOut.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("No upcoming buses in the next 2 hours."));
    }
}
