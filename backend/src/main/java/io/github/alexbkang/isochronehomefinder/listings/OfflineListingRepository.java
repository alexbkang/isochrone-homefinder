package io.github.alexbkang.isochronehomefinder.listings;

import java.util.List;
import org.locationtech.jts.geom.Geometry;

public final class OfflineListingRepository implements ListingRepository {

  private final List<Listing> homes;

  public OfflineListingRepository(List<Listing> homes) {
    this.homes = homes;
  }

  @Override
  public List<Listing> findWithin(Geometry region) {
    return homes;
  }
}
