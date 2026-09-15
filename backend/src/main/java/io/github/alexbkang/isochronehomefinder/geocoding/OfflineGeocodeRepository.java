package io.github.alexbkang.isochronehomefinder.geocoding;

import java.util.List;
import java.util.Optional;

public final class OfflineGeocodeRepository implements GeocodeRepository {

  private static final List<Hit> HITS =
      List.of(
          new Hit("Los Angeles", "California", -118.2437, 34.0522),
          new Hit("Downtown Los Angeles", "California", -118.2468, 34.0407),
          new Hit("Santa Monica", "California", -118.4912, 34.0195));

  @Override
  public Optional<Hit> search(String text, Focus focus) {
    return HITS.stream().findFirst();
  }

  @Override
  public List<Hit> autocomplete(String text, int limit, Focus focus) {
    return HITS;
  }
}
