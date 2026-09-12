package io.github.alexbkang.isochronehomefinder.listings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.alexbkang.isochronehomefinder.error.ProblemDetailsTestAdvice;
import io.github.alexbkang.isochronehomefinder.error.UpstreamException;
import io.github.alexbkang.isochronehomefinder.error.UpstreamLoggingAdvice;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ListingsControllerTest {

  private static final String REGION_POLY =
      "{\"type\":\"Polygon\",\"coordinates\":[["
          + "[-118.4,34.0],[-118.2,34.0],[-118.2,34.2],[-118.4,34.2],[-118.4,34.0]]"
          + "]}";

  private MockMvc mvc(ListingRepository repository) {
    return MockMvcBuilders.standaloneSetup(new ListingsController(new ListingsService(repository)))
        .setControllerAdvice(new ProblemDetailsTestAdvice(), new UpstreamLoggingAdvice())
        .build();
  }

  private static Listing listing(double lon, double lat) {
    return new Listing(
        1, lat, lon, 0, null, null, null, null, null, null, null, null, null, null, null, null,
        List.of());
  }

  @Test
  void invalidGeometryIs400WithoutReachingClient() throws Exception {
    var repository = mock(ListingRepository.class);
    var mvc = mvc(repository);
    String[] bad = {
      "{}", "null", "{\"type\":\"LineString\",\"coordinates\":[]}",
    };
    for (var body : bad) {
      mvc.perform(post("/listings").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
    verifyNoInteractions(repository);
  }

  @Test
  void polygonIsSearchedOnce() throws Exception {
    var repository = mock(ListingRepository.class);
    when(repository.findWithin(any())).thenReturn(List.of(listing(-118.3, 34.1)));
    mvc(repository)
        .perform(post("/listings").contentType(MediaType.APPLICATION_JSON).content(REGION_POLY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    verify(repository, times(1)).findWithin(any());
  }

  @Test
  void sameSearchKeyIsServedFromCacheAcrossRequests() throws Exception {
    var repository = mock(ListingRepository.class);
    when(repository.findWithin(any())).thenReturn(List.of(listing(-118.3, 34.1)));
    var mvc = mvc(repository);
    // First call computes and caches; the repeat (same key, even a different geometry string
    // for the same search) must be served without touching realty again.
    mvc.perform(
            post("/listings?key=abc").contentType(MediaType.APPLICATION_JSON).content(REGION_POLY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    var movedGeometry =
        "{\"type\":\"Polygon\",\"coordinates\":[["
            + "[-118.4,34.0],[-118.3,34.0],[-118.3,34.1],[-118.2,34.1],[-118.2,34.2],[-118.4,34.2],[-118.4,34.0]]"
            + "]}";
    mvc.perform(
            post("/listings?key=abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(movedGeometry))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    verify(repository, times(1)).findWithin(any());
  }

  @Test
  void homesOutsideRegionAreFiltered() throws Exception {

    var repository = mock(ListingRepository.class);
    when(repository.findWithin(any()))
        .thenReturn(List.of(listing(-118.3, 34.1), listing(-118.1, 34.1)));
    mvc(repository)
        .perform(post("/listings").contentType(MediaType.APPLICATION_JSON).content(REGION_POLY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    verify(repository, times(1)).findWithin(any());
  }

  @Test
  void multipolygonHomesInsideEitherPolygonAreKept() throws Exception {

    var repository = mock(ListingRepository.class);
    when(repository.findWithin(any()))
        .thenReturn(List.of(listing(-118.3, 34.1), listing(-117.7, 34.1)));
    var body =
        "{\"type\":\"MultiPolygon\",\"coordinates\":[["
            + "[[-118.4,34.0],[-118.2,34.0],[-118.2,34.2],[-118.4,34.2],[-118.4,34.0]]"
            + "],["
            + "[[-117.8,34.0],[-117.6,34.0],[-117.6,34.2],[-117.8,34.2],[-117.8,34.0]]"
            + "]]}";
    mvc(repository)
        .perform(post("/listings").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
    verify(repository, times(1)).findWithin(any());
  }

  @Test
  void homesInsideAHoleAreFiltered() throws Exception {

    var repository = mock(ListingRepository.class);
    when(repository.findWithin(any()))
        .thenReturn(List.of(listing(-118.2, 34.2), listing(-118.35, 34.1)));
    var body =
        "{\"type\":\"Polygon\",\"coordinates\":["
            + "[[-118.4,34.0],[-118.0,34.0],[-118.0,34.4],[-118.4,34.4],[-118.4,34.0]],"
            + "[[-118.25,34.15],[-118.25,34.25],[-118.15,34.25],[-118.15,34.15],[-118.25,34.15]]"
            + "]}";
    mvc(repository)
        .perform(post("/listings").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    verify(repository, times(1)).findWithin(any());
  }

  @Test
  void upstreamFailureIs502AndSurfacesProviderText() throws Exception {
    var repository = mock(ListingRepository.class);
    when(repository.findWithin(any()))
        .thenThrow(new UpstreamException("realtyapi upstream exploded"));
    mvc(repository)
        .perform(post("/listings").contentType(MediaType.APPLICATION_JSON).content(REGION_POLY))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value("realtyapi upstream exploded"));
  }
}
