package io.github.alexbkang.isochronehomefinder.isochrones;

import io.github.alexbkang.isochronehomefinder.geometry.GeoJson;
import org.locationtech.jts.geom.Geometry;
import tools.jackson.databind.JsonNode;

public final class OfflineIsochroneRepository implements IsochroneRepository {

  private final Geometry region;

  public OfflineIsochroneRepository(JsonNode regionJson) {
    this.region = GeoJson.toGeometry(regionJson);
  }

  @Override
  public Geometry fetchIsochrone(double lng, double lat, long rangeSeconds) {
    return region;
  }
}
