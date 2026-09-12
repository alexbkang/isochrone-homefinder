package io.github.alexbkang.isochronehomefinder.listings;

import io.github.alexbkang.isochronehomefinder.geometry.GeoJson;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
public class ListingsService {

  private static final GeometryFactory FACTORY = new GeometryFactory();
  private static final Duration SEARCH_TTL = Duration.ofDays(1);
  private static final int CACHE_CAP = 512;

  private final ListingRepository repository;
  private final ListingCache searchCache;

  public ListingsService(ListingRepository repository) {
    this.repository = repository;
    this.searchCache = new ListingCache(SEARCH_TTL, CACHE_CAP, Clock.systemUTC());
  }

  public List<Listing> listings(JsonNode geometry, String searchKey) {
    if (searchKey == null || searchKey.isBlank()) {
      return compute(geometry);
    }
    return searchCache.get(searchKey, () -> compute(geometry));
  }

  private List<Listing> compute(JsonNode geometry) {
    var region = GeoJson.toGeometry(geometry);
    if (!(region instanceof Polygonal)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Unsupported region geometry type \"" + region.getGeometryType() + "\".");
    }
    var prepared = PreparedGeometryFactory.prepare(region);
    return repository.findWithin(region).stream()
        .filter(
            listing ->
                prepared.contains(
                    FACTORY.createPoint(new Coordinate(listing.lng(), listing.lat()))))
        .toList();
  }
}
