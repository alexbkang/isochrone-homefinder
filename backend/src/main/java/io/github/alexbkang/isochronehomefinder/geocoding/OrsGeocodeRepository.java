package io.github.alexbkang.isochronehomefinder.geocoding;

import io.github.alexbkang.isochronehomefinder.error.UpstreamException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

public class OrsGeocodeRepository implements GeocodeRepository {

  public static final String GEOCODE_ENDPOINT = "https://api.openrouteservice.org/geocode/search";
  public static final String AUTOCOMPLETE_ENDPOINT =
      "https://api.openrouteservice.org/geocode/autocomplete";
  public static final String COUNTRY = "US";

  private final RestClient rest;

  public OrsGeocodeRepository(RestClient rest) {
    this.rest = rest;
  }

  @Override
  public Optional<Hit> search(String text, Double lon, Double lat) {
    return fetchHits(GEOCODE_ENDPOINT, text, 1, lon, lat).stream().findFirst();
  }

  @Override
  public List<Hit> autocomplete(String text, int limit, Double lon, Double lat) {
    return fetchHits(AUTOCOMPLETE_ENDPOINT, text, limit, lon, lat);
  }

  private List<Hit> fetchHits(String endpoint, String text, int limit, Double lon, Double lat) {
    var focused = lon != null && lat != null;
    var root =
        Optional.ofNullable(
                rest.get()
                    .uri(
                        endpoint,
                        builder -> {
                          builder
                              .queryParam("text", text)
                              .queryParam("size", limit)
                              .queryParam("boundary.country", COUNTRY);
                          if (focused) {
                            builder
                                .queryParam("focus.point.lon", lon)
                                .queryParam("focus.point.lat", lat);
                          }
                          return builder.build();
                        })
                    .retrieve()
                    .body(JsonNode.class))
            .orElseThrow(() -> new UpstreamException("ORS geocode returned an empty response"));
    return root.path("features")
        .valueStream()
        .map(OrsGeocodeRepository::toHit)
        .filter(Objects::nonNull)
        .toList();
  }

  private static Hit toHit(JsonNode feature) {
    var coords = feature.path("geometry").path("coordinates");
    if (!coords.isArray() || coords.size() < 2) return null;
    var props = feature.path("properties");
    return new Hit(
        props.path("name").asString(""),
        props.path("region").asString(""),
        coords.get(0).asDouble(),
        coords.get(1).asDouble());
  }
}
