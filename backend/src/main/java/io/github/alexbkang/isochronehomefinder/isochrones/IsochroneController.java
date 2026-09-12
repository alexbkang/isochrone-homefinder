package io.github.alexbkang.isochronehomefinder.isochrones;

import io.github.alexbkang.isochronehomefinder.geometry.GeoJson;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/region")
public class IsochroneController {

  private final IsochroneService service;

  public IsochroneController(IsochroneService service) {
    this.service = service;
  }

  public record RegionResponse(JsonNode region, List<JsonNode> zones) {}

  @PostMapping
  public RegionResponse region(@RequestBody @NonNull List<List<Anchor>> groups) {
    var result = service.composeReachableRegion(groups);
    return new RegionResponse(
        result.region() == null ? null : GeoJson.toJson(result.region()),
        result.zones().stream().map(GeoJson::toJson).toList());
  }
}
