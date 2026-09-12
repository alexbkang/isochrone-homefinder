package io.github.alexbkang.isochronehomefinder.geometry;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Rings {

  public static Stream<LineString> outerRings(Geometry region) {
    if (region instanceof Polygon polygon) {
      return Stream.of(polygon.getExteriorRing());
    }
    if (region instanceof MultiPolygon multi) {
      return IntStream.range(0, multi.getNumGeometries())
          .mapToObj(multi::getGeometryN)
          .flatMap(
              geometry ->
                  geometry instanceof Polygon polygon
                      ? Stream.of(polygon.getExteriorRing())
                      : Stream.empty());
    }
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Unsupported region geometry type \"" + region.getGeometryType() + "\".");
  }
}
