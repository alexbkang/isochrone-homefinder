package io.github.alexbkang.isochronehomefinder.geocoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.alexbkang.isochronehomefinder.error.UpstreamException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class OrsGeocodeRepositoryTest {

  private static final String ENDPOINT = OrsGeocodeRepository.GEOCODE_ENDPOINT;
  private static final String AUTOCOMPLETE = OrsGeocodeRepository.AUTOCOMPLETE_ENDPOINT;

  private record ClientAndServer(OrsGeocodeRepository client, MockRestServiceServer server) {}

  private static ClientAndServer clientAndServer() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    return new ClientAndServer(new OrsGeocodeRepository(builder.build()), server);
  }

  private static final String FEATURES =
      """
      {
        "type": "FeatureCollection",
        "features": [
          {
            "type": "Feature",
            "geometry": { "type": "Point", "coordinates": [-97.74, 30.27] },
            "bbox": [-97.96, 30.13, -97.49, 30.51],
            "properties": { "name": "Austin", "label": "Austin, TX, USA",
                            "layer": "locality", "region": "Texas", "confidence": 0.98 }
          },
          {
            "type": "Feature",
            "geometry": { "type": "Point", "coordinates": [-98.49, 29.42] },
            "properties": { "name": "San Antonio", "label": "San Antonio, TX, USA",
                            "layer": "locality", "region": "Texas" }
          }
        ]
      }
      """;

  private static final String EMPTY = "{\"type\":\"FeatureCollection\",\"features\":[]}";

  private static final String SKIPS_BAD =
      """
      {
        "type": "FeatureCollection",
        "features": [
          { "type": "Feature", "geometry": null, "properties": {} },
          { "type": "Feature",
            "geometry": { "type": "Point", "coordinates": [-97.74, 30.27] },
            "properties": { "name": "Austin", "layer": "locality", "region": "Texas" } }
        ]
      }
      """;

  @Test
  void searchReturnsTheTopHit() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(URI.create(ENDPOINT + "?text=austin&size=1&boundary.country=US")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(FEATURES, MediaType.APPLICATION_JSON));

    var hit = cs.client.search("austin", null, null).orElseThrow();
    cs.server.verify();
    assertEquals("Austin", hit.name());
    assertEquals("Texas", hit.region());
    assertEquals(-97.74, hit.lon());
    assertEquals(30.27, hit.lat());
  }

  @Test
  void focusAddsRankingParams() {
    var cs = clientAndServer();
    cs.server
        .expect(
            requestTo(
                URI.create(
                    ENDPOINT
                        + "?text=austin&size=1&boundary.country=US"
                        + "&focus.point.lon=-97.74&focus.point.lat=30.27")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(FEATURES, MediaType.APPLICATION_JSON));

    cs.client.search("austin", -97.74, 30.27);
    cs.server.verify();
  }

  @Test
  void nullCoordinatesOmitFocus() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(URI.create(ENDPOINT + "?text=austin&size=1&boundary.country=US")))
        .andRespond(withSuccess(FEATURES, MediaType.APPLICATION_JSON));

    cs.client.search("austin", null, null);
    cs.server.verify();
  }

  @Test
  void emptyFeaturesReturnEmpty() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(URI.create(ENDPOINT + "?text=zzz&size=1&boundary.country=US")))
        .andRespond(withSuccess(EMPTY, MediaType.APPLICATION_JSON));

    assertTrue(cs.client.search("zzz", null, null).isEmpty());
  }

  @Test
  void malformedFeaturesAreSkipped() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(URI.create(ENDPOINT + "?text=x&size=1&boundary.country=US")))
        .andRespond(withSuccess(SKIPS_BAD, MediaType.APPLICATION_JSON));

    assertEquals("Austin", cs.client.search("x", null, null).orElseThrow().name());
  }

  @Test
  void upstreamErrorIsReadable() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(URI.create(ENDPOINT + "?text=x&size=1&boundary.country=US")))
        .andRespond(
            withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("bad upstream")
                .contentType(MediaType.APPLICATION_JSON));

    var e =
        assertThrows(RestClientResponseException.class, () -> cs.client.search("x", null, null));
    assertTrue(e.getMessage().contains("500"));
    assertTrue(e.getMessage().contains("bad upstream"));
  }

  @Test
  void emptySuccessBodyIsUpstreamFailure() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(URI.create(ENDPOINT + "?text=x&size=1&boundary.country=US")))
        .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

    var e = assertThrows(UpstreamException.class, () -> cs.client.search("x", null, null));
    assertTrue(e.getMessage().contains("empty response"));
  }

  @Test
  void autocompleteParsesAllHits() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(URI.create(AUTOCOMPLETE + "?text=austin&size=5&boundary.country=US")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(FEATURES, MediaType.APPLICATION_JSON));

    var hits = cs.client.autocomplete("austin", 5, null, null);
    cs.server.verify();
    assertEquals(2, hits.size());
    assertEquals("Austin", hits.get(0).name());
  }
}
