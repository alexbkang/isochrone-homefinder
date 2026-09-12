package io.github.alexbkang.isochronehomefinder.config;

import io.github.alexbkang.isochronehomefinder.listings.Listing;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The offline dataset: homes for the listing adapter and an enclosing region for the isochrone
 * adapter. Fixed, so offline mode needs no upstream data.
 */
final class OfflineData {

  private static final double[] DEFAULT_CENTER = new double[] {-118.2437, 34.0522};

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final List<Listing> HOMES =
      List.of(
          new Listing(
              9000, 34.0462, -118.2557, 300000L, 1, 1, 700.0, "singleFamily", 1950, 1, 4000.0,
              "squareFeet", "1st St", "Demo", "CA", "90000", List.of()),
          new Listing(
              9001, 34.0462, -118.2497, 347000L, 2, 2, 830.0, "condo", 1957, 2, 4300.0,
              "squareFeet", "2nd St", "Demo", "CA", "90000", List.of()),
          new Listing(
              9002, 34.0462, -118.2437, 394000L, 3, 3, 960.0, "townhouse", 1964, 3, 4600.0,
              "squareFeet", "3rd St", "Demo", "CA", "90000", List.of()),
          new Listing(
              9003, 34.0462, -118.2377, 441000L, 4, 1, 1090.0, "singleFamily", 1971, 4, 4900.0,
              "squareFeet", "4th St", "Demo", "CA", "90000", List.of()),
          new Listing(
              9004, 34.0462, -118.2317, 488000L, 1, 2, 1220.0, "condo", 1978, 5, 5200.0,
              "squareFeet", "5th St", "Demo", "CA", "90000", List.of()),
          new Listing(
              9005, 34.0522, -118.2557, 535000L, 2, 3, 1350.0, "singleFamily", 1985, 6, 5500.0,
              "squareFeet", "6th St", "Demo", "CA", "90000", List.of()),
          new Listing(
              9006, 34.0522, -118.2497, 582000L, 3, 1, 1480.0, "condo", 1992, 7, 5800.0,
              "squareFeet", "7th St", "Demo", "CA", "90000", List.of()),
          new Listing(
              9007, 34.0522, -118.2437, 629000L, 4, 2, 1610.0, "townhouse", 1999, 8, 6100.0,
              "squareFeet", "8th St", "Demo", "CA", "90000", List.of()),
          new Listing(
              9008, 34.0522, -118.2377, 676000L, 1, 3, 1740.0, "singleFamily", 2006, 9, 6400.0,
              "squareFeet", "9th St", "Demo", "CA", "90000", List.of()),
          new Listing(
              9009, 34.0522, -118.2317, 723000L, 2, 1, 1870.0, "condo", 2013, 10, 6700.0,
              "squareFeet", "10th St", "Demo", "CA", "90000", List.of()));

  private final List<Listing> homes;
  private final JsonNode region;

  private OfflineData(List<Listing> homes, JsonNode region) {
    this.homes = homes;
    this.region = region;
  }

  List<Listing> homes() {
    return homes;
  }

  JsonNode region() {
    return region;
  }

  static OfflineData synthetic() {
    return new OfflineData(HOMES, envelope(HOMES));
  }

  /** A GeoJSON Polygon enclosing every home, with a margin so none sit on the ring boundary. */
  private static JsonNode envelope(List<Listing> homes) {
    var minLon = Double.POSITIVE_INFINITY;
    var minLat = Double.POSITIVE_INFINITY;
    var maxLon = Double.NEGATIVE_INFINITY;
    var maxLat = Double.NEGATIVE_INFINITY;
    var n = 0;
    for (var home : homes) {
      var lon = home.lng();
      var lat = home.lat();
      if (!Double.isFinite(lon) || !Double.isFinite(lat)) {
        continue;
      }
      minLon = Math.min(minLon, lon);
      maxLon = Math.max(maxLon, lon);
      minLat = Math.min(minLat, lat);
      maxLat = Math.max(maxLat, lat);
      n++;
    }
    var lon = DEFAULT_CENTER[0];
    var lat = DEFAULT_CENTER[1];
    if (n > 0 && minLon == maxLon && minLat == maxLat) {
      lon = minLon;
      lat = minLat;
    } else if (n > 0) {
      lon = (minLon + maxLon) / 2.0;
      lat = (minLat + maxLat) / 2.0;
    }
    var m = 0.02;
    var poly = JSON.createObjectNode();
    poly.put("type", "Polygon");
    var coords = poly.putArray("coordinates");
    var ring = coords.addArray();
    ring.addArray().add(lon - m).add(lat - m);
    ring.addArray().add(lon + m).add(lat - m);
    ring.addArray().add(lon + m).add(lat + m);
    ring.addArray().add(lon - m).add(lat + m);
    ring.addArray().add(lon - m).add(lat - m);
    return poly;
  }
}
