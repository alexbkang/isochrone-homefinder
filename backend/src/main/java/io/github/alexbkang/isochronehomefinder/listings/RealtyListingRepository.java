package io.github.alexbkang.isochronehomefinder.listings;

import io.github.alexbkang.isochronehomefinder.error.UpstreamException;
import io.github.alexbkang.isochronehomefinder.geometry.Rings;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

public class RealtyListingRepository implements ListingRepository {

  public static final String REALTYAPI_ENDPOINT = "https://zillow.realtyapi.io/search/bypolygon";

  private static final String LISTING_STATUS_FOR_SALE = "For_Sale";
  private static final Duration REALTYAPI_TTL = Duration.ofDays(1);
  private static final int CACHE_CAP = 512;

  private final RestClient rest;
  private final ListingCache cache;

  public RealtyListingRepository(RestClient rest) {
    this.rest = rest;
    this.cache = new ListingCache(REALTYAPI_TTL, CACHE_CAP, Clock.systemUTC());
  }

  @Override
  public List<Listing> findWithin(Geometry region) {
    return Rings.outerRings(region)
        .flatMap(
            ring -> {
              var polygon = zillowPolygon(ring);
              return cache.get(polygon, () -> fetch(polygon)).stream();
            })
        .toList();
  }

  private List<Listing> fetch(String polygon) {
    var root =
        Optional.ofNullable(
                rest.get()
                    .uri(
                        REALTYAPI_ENDPOINT,
                        builder ->
                            builder
                                .queryParam("polygon", polygon)
                                .queryParam("listingStatus", LISTING_STATUS_FOR_SALE)
                                .build())
                    .retrieve()
                    .body(JsonNode.class))
            .orElseThrow(() -> new UpstreamException("realtyapi returned an empty response"));
    return extract(root);
  }

  private static List<Listing> extract(JsonNode root) {
    var out = new ArrayList<Listing>();
    for (var entry : root.path("searchResults")) {
      var property = entry.get("property");
      if (property != null && property.isObject()) {
        out.add(toListing(property));
      }
    }
    return out;
  }

  private static Listing toListing(JsonNode property) {
    var loc = property.path("location");
    var address = property.path("address");
    var lot = property.path("lotSizeWithUnit");
    return new Listing(
        property.path("zpid").asLong(),
        loc.path("latitude").asDouble(),
        loc.path("longitude").asDouble(),
        property.path("price").path("value").asLong(),
        property.path("bedrooms").asInt(),
        property.path("bathrooms").asInt(),
        property.path("livingArea").asDouble(),
        property.path("propertyType").asString(),
        property.path("yearBuilt").asInt(),
        property.path("daysOnZillow").asInt(),
        lot.path("lotSize").asDouble(),
        lot.path("lotSizeUnit").asString(),
        address.path("streetAddress").asString(),
        address.path("city").asString(),
        address.path("state").asString(),
        address.path("zipcode").asString(),
        photos(property.path("media")));
  }

  private static List<String> photos(JsonNode media) {
    var gallery = media.path("allPropertyPhotos");
    var chosen = gallery.path("medium");
    if (!chosen.isArray() || chosen.isEmpty()) {
      chosen = gallery.path("highResolution");
    }
    return chosen.valueStream().map(JsonNode::asString).filter(url -> !url.isBlank()).toList();
  }

  // Zillow expects `lat lon`
  static String zillowPolygon(LineString ring) {
    var out = new StringBuilder();
    for (var c : ring.getCoordinates()) {
      if (out.length() > 0) out.append(',');
      out.append(c.y).append(' ').append(c.x);
    }
    return out.toString();
  }
}
