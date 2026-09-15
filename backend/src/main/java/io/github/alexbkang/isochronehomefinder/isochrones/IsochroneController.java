package io.github.alexbkang.isochronehomefinder.isochrones;

import io.github.alexbkang.isochronehomefinder.geometry.GeoJson;
import jakarta.validation.Valid;
import java.util.List;
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
  public RegionResponse region(@RequestBody @Valid List<List<@Valid Anchor>> groups) {
    var result = service.composeReachableRegion(groups);
    return switch (result) {
      case ReachableRegion.Found found ->
          new RegionResponse(
              GeoJson.toJson(found.region()), found.zones().stream().map(GeoJson::toJson).toList());
      case ReachableRegion.Empty empty ->
          new RegionResponse(null, empty.zones().stream().map(GeoJson::toJson).toList());
    };
  }
}
