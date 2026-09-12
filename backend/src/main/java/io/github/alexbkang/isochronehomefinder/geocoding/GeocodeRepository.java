package io.github.alexbkang.isochronehomefinder.geocoding;

import java.util.List;
import java.util.Optional;

public interface GeocodeRepository {

  record Hit(String name, String region, double lon, double lat) {}

  Optional<Hit> search(String text, Double lon, Double lat);

  List<Hit> autocomplete(String text, int limit, Double lon, Double lat);
}
