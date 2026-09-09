package com.gtfs.bustracker.gtfs;

import com.gtfs.bustracker.model.Route;
import com.gtfs.bustracker.model.Stop;
import com.gtfs.bustracker.model.StopTimeEntry;
import com.gtfs.bustracker.model.TripInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Preveri nalaganje testnih datotek GTFS, tudi posebnosti zapisa CSV. */
class GtfsRepositoryLoadIT {

    private Path gtfsDir;

    @BeforeEach
    void locateFixtureDir() throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource("gtfs-fixture/stops.txt");
        assertTrue(resource != null, "gtfs-fixture test resources not found on classpath");
        gtfsDir = Paths.get(resource.toURI()).getParent();
    }

    @Test
    void loadsStopMetadataForKnownStop() {
        GtfsRepository repo = GtfsRepository.loadForStop(gtfsDir, 2);

        Stop stop = repo.getStop();
        assertEquals(2, stop.getStopId());
        assertEquals("AL Masjid Al-nabawi (Clock Roundabout)", stop.getName());
    }

    @Test
    void unknownStopIdFailsFast() {
        assertThrows(GtfsDataException.class, () -> GtfsRepository.loadForStop(gtfsDir, 999_999));
    }

    @Test
    void onlyKeepsStopTimesForTheRequestedStop() {
        GtfsRepository repo = GtfsRepository.loadForStop(gtfsDir, 2);

        assertFalse(repo.getStopTimesAtStop().isEmpty());
        // Vsak prihod mora imeti pripadajočo vožnjo.
        for (StopTimeEntry entry : repo.getStopTimesAtStop()) {
            assertTrue(repo.getTripsById().containsKey(entry.getTripId()),
                    "trip " + entry.getTripId() + " should have been loaded from trips.txt");
        }
    }

    @Test
    void onlyKeepsRoutesReachableFromTheRequestedStop() {
        // Na postajališču 2 ustavljajo samo linije 101, 106 in 107.
        GtfsRepository repo = GtfsRepository.loadForStop(gtfsDir, 2);

        Set<String> routeIds = repo.getRoutesById().values().stream()
                .map(Route::getRouteId)
                .collect(Collectors.toSet());

        assertEquals(Set.of("101", "106", "107"), routeIds);

        // Shranjene vožnje morajo pripadati tem linijam.
        // Nepovezanih voženj in linij ne shranjujemo.
        for (TripInfo trip : repo.getTripsById().values()) {
            assertTrue(routeIds.contains(trip.getRouteId()));
        }
    }

    @Test
    void loadsCalendarForReferencedServices() {
        GtfsRepository repo = GtfsRepository.loadForStop(gtfsDir, 2);

        assertFalse(repo.getCalendarsByServiceId().isEmpty());
        assertTrue(repo.isServiceActiveOn("1", java.time.LocalDate.of(2020, 3, 2)));
        assertFalse(repo.isServiceActiveOn("1", java.time.LocalDate.of(2019, 1, 1)));
    }
}
