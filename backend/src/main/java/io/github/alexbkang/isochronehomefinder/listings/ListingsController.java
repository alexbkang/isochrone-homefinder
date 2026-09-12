package io.github.alexbkang.isochronehomefinder.listings;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/listings")
public class ListingsController {

  private final ListingsService service;

  public ListingsController(ListingsService service) {
    this.service = service;
  }

  @PostMapping
  public List<Listing> listings(
      @RequestBody(required = false) JsonNode geometry,
      @RequestParam(required = false) String key) {
    return service.listings(geometry, key);
  }
}
