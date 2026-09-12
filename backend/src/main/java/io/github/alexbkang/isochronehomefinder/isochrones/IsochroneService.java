package io.github.alexbkang.isochronehomefinder.isochrones;

import io.github.alexbkang.isochronehomefinder.error.UpstreamException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.locationtech.jts.geom.Geometry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IsochroneService {

  public static final int MAX_GROUPS = 6;
  public static final int MAX_OR = 5;

  public record RegionResult(Geometry region, List<Geometry> zones) {}

  private final IsochroneRepository ors;
  private final ExecutorService pool =
      Executors.newFixedThreadPool(
          MAX_OR,
          runnable -> {
            var thread = new Thread(runnable, "isochrone-fetch");
            thread.setDaemon(true);
            return thread;
          });

  public IsochroneService(IsochroneRepository ors) {
    this.ors = ors;
  }

  public RegionResult composeReachableRegion(List<List<Anchor>> request) {
    var zones = new ArrayList<Geometry>();
    var it = normalize(request).iterator();
    var region = fetchParallelGroup(it.next(), zones);
    while (!region.isEmpty() && it.hasNext()) {
      region = region.intersection(fetchParallelGroup(it.next(), zones));
    }
    return region.isEmpty() ? new RegionResult(null, zones) : new RegionResult(region, zones);
  }

  private Geometry fetchParallelGroup(List<Anchor> group, List<Geometry> zones) {
    var futures =
        group.stream()
            .map(
                anchor ->
                    pool.submit(
                        () -> ors.fetchIsochrone(anchor.lng(), anchor.lat(), anchor.min() * 60L)))
            .toList();
    var geoms = new ArrayList<Geometry>(futures.size());
    try {
      for (var future : futures) geoms.add(future.get());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while fetching isochrones.", ex);
    } catch (ExecutionException ex) {
      var cause = ex.getCause();
      if (cause instanceof UpstreamException ue) throw ue;
      throw new UpstreamException("Failed to fetch isochrone.", cause);
    }
    zones.addAll(geoms);
    return geoms.stream().reduce(Geometry::union).orElseThrow();
  }

  private static List<List<Anchor>> normalize(List<List<Anchor>> groups) {
    if (groups == null || groups.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Supply at least one group of places.");
    }
    if (groups.size() > MAX_GROUPS) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "No more than " + MAX_GROUPS + " groups per search.");
    }
    return groups.stream().map(IsochroneService::validGroup).toList();
  }

  private static List<Anchor> validGroup(List<Anchor> group) {
    if (group == null || group.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Each group needs at least one place.");
    }
    if (group.size() > MAX_OR) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A group may hold at most " + MAX_OR + " places.");
    }
    var seen = new HashSet<String>();
    return group.stream()
        .map(
            place -> {
              if (place == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Every place needs a latitude, a longitude, and a drive limit.");
              }
              if (place.min() < 1 || place.min() > 60) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Each drive limit must be 1-60 minutes.");
              }
              return place;
            })
        .filter(anchor -> seen.add(anchor.lat() + "," + anchor.lng()))
        .toList();
  }
}
