package io.github.alexbkang.isochronehomefinder.geocoding;

import io.github.alexbkang.isochronehomefinder.geocoding.GeocodeRepository.Hit;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/geocode")
public class GeocodeController {

  public static final int MAX_LIMIT = 10;

  private final GeocodeRepository repository;

  public GeocodeController(GeocodeRepository repository) {
    this.repository = repository;
  }

  @GetMapping
  public Hit geocode(
      @RequestParam @NotBlank String text,
      @RequestParam(required = false) Double lon,
      @RequestParam(required = false) Double lat) {
    var focus = focus(lon, lat);
    return repository
        .search(text, focus)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No location matched \"" + text + "\"."));
  }

  @GetMapping("/autocomplete")
  public List<Hit> autocomplete(
      @RequestParam @NotBlank String text,
      @RequestParam @Min(1) @Max(MAX_LIMIT) int limit,
      @RequestParam(required = false) Double lon,
      @RequestParam(required = false) Double lat) {
    var focus = focus(lon, lat);
    return repository.autocomplete(text, limit, focus);
  }

  private static Focus focus(Double lon, Double lat) {
    if ((lon == null) != (lat == null)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "lon and lat must be supplied together");
    }
    if (lon == null) {
      return new Focus.None();
    }
    if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "focus lon/lat out of range");
    }
    return new Focus.At(lon, lat);
  }
}
