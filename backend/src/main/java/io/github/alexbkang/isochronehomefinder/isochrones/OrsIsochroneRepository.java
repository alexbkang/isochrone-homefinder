package io.github.alexbkang.isochronehomefinder.isochrones;

import io.github.alexbkang.isochronehomefinder.error.UpstreamException;
import io.github.alexbkang.isochronehomefinder.geometry.GeoJson;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Geometry;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

public class OrsIsochroneRepository implements IsochroneRepository {

  public static final String ORS_ENDPOINT =
      "https://api.openrouteservice.org/v2/isochrones/driving-car";

  private record IsochroneRequest(List<double[]> locations, long[] range) {}

  private final RestClient rest;

  public OrsIsochroneRepository(RestClient rest) {
    this.rest = rest;
  }

  @Override
  public Geometry fetchIsochrone(double lng, double lat, long rangeSeconds) {
    var root =
        Optional.ofNullable(
                rest.post()
                    .uri(ORS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        new IsochroneRequest(
                            List.of(new double[] {lng, lat}), new long[] {rangeSeconds}))
                    .retrieve()
                    .body(JsonNode.class))
            .orElseThrow(() -> new UpstreamException("ORS returned an empty response"));
    // "features" can be empty and "geometry" can be null.
    var geometry = root.path("features").path(0).path("geometry");
    if (!geometry.isObject()) {
      throw new UpstreamException("ORS returned no isochrone polygon");
    }
    return GeoJson.toGeometry(geometry);
  }
}
