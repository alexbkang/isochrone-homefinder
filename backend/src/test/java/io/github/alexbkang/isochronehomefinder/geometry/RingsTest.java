package io.github.alexbkang.isochronehomefinder.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class RingsTest {

  private static final GeometryFactory GEOMETRY = new GeometryFactory();

  private static Polygon square(double minLon, double maxLon) {
    return GEOMETRY.createPolygon(
        new Coordinate[] {
          new Coordinate(minLon, 34.0),
          new Coordinate(maxLon, 34.0),
          new Coordinate(maxLon, 34.2),
          new Coordinate(minLon, 34.2),
          new Coordinate(minLon, 34.0)
        });
  }

  @Test
  void polygonYieldsOneRing() {
    var rings = Rings.outerRings(square(-118.4, -118.3)).toList();
    assertEquals(1, rings.size());
    assertEquals(5, rings.get(0).getNumPoints());
  }

  @Test
  void multiPolygonYieldsEachRing() {
    var region =
        GEOMETRY.createMultiPolygon(new Polygon[] {square(-118.4, -118.3), square(-117.8, -117.6)});
    var rings = Rings.outerRings(region).toList();
    assertEquals(2, rings.size());
    assertEquals(-118.4, rings.get(0).getCoordinateN(0).x);
    assertEquals(-117.8, rings.get(1).getCoordinateN(0).x);
  }

  @Test
  void nonPolygonalGeometryIsRejected() {
    var line =
        GEOMETRY.createLineString(new Coordinate[] {new Coordinate(0, 0), new Coordinate(1, 1)});
    var e = assertThrows(ResponseStatusException.class, () -> Rings.outerRings(line));
    assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    assertTrue(e.getReason().contains("LineString"));
  }
}
