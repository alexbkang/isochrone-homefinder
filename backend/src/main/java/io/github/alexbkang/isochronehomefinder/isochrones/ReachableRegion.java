package io.github.alexbkang.isochronehomefinder.isochrones;

import java.util.List;
import org.locationtech.jts.geom.Geometry;

/** The reachable region of a search, which may be empty when the groups do not intersect. */
public sealed interface ReachableRegion {

  List<Geometry> zones();

  record Found(Geometry region, List<Geometry> zones) implements ReachableRegion {}

  record Empty(List<Geometry> zones) implements ReachableRegion {}
}
