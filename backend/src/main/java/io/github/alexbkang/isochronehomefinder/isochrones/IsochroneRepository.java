package io.github.alexbkang.isochronehomefinder.isochrones;

import org.locationtech.jts.geom.Geometry;

public interface IsochroneRepository {

  Geometry fetchIsochrone(double lng, double lat, long rangeSeconds);
}
