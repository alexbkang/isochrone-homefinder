package io.github.alexbkang.isochronehomefinder.geometry;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class GeoJson {

  private static final GeoJsonReader READER = new GeoJsonReader();
  private static final GeoJsonWriter WRITER = new GeoJsonWriter();
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private GeoJson() {}

  public static Geometry toGeometry(JsonNode geojson) {
    if (geojson == null || geojson.isNull()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "No GeoJSON geometry was supplied.");
    }
    try {
      return READER.read(geojson.toString());
    } catch (ParseException | RuntimeException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unusable GeoJSON geometry.", e);
    }
  }

  public static JsonNode toJson(Geometry geometry) {
    return JSON.readTree(WRITER.write(geometry));
  }
}
