package io.github.alexbkang.isochronehomefinder.listings;

import java.util.List;
import org.locationtech.jts.geom.Geometry;

public interface ListingRepository {

  List<Listing> findWithin(Geometry region);
}
