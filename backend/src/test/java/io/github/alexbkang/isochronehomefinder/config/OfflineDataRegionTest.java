package io.github.alexbkang.isochronehomefinder.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * The offline region is a hardcoded box. {@code ListingsService} keeps a listing only if the region
 * contains it, so a home outside the box would silently vanish from offline results.
 */
class OfflineDataRegionTest {

  private static final GeometryFactory GEOMETRY = new GeometryFactory();

  @Test
  void regionContainsEveryHome() {
    var data = OfflineConfiguration.OfflineData.synthetic();
    for (var home : data.homes()) {
      var point = GEOMETRY.createPoint(new Coordinate(home.lng(), home.lat()));
      assertTrue(
          data.region().contains(point), home.street() + " falls outside the offline region");
    }
  }
}
