package io.github.alexbkang.isochronehomefinder.isochrones;

import org.locationtech.jts.geom.Geometry;

public final class OfflineIsochroneRepository implements IsochroneRepository {

  private final Geometry region;

  public OfflineIsochroneRepository(Geometry region) {
    this.region = region;
  }

  @Override
  public Geometry fetchIsochrone(double lng, double lat, long rangeSeconds) {
    return region;
  }
}
