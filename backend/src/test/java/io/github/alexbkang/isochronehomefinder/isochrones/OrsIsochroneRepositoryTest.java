package io.github.alexbkang.isochronehomefinder.isochrones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.alexbkang.isochronehomefinder.geometry.GeoJson;
import io.github.alexbkang.isochronehomefinder.error.UpstreamException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class OrsIsochroneRepositoryTest {

  private static final String ORS_ENDPOINT = OrsIsochroneRepository.ORS_ENDPOINT;
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private record ClientAndServer(OrsIsochroneRepository client, MockRestServiceServer server) {}

  private static ClientAndServer clientAndServer() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    return new ClientAndServer(new OrsIsochroneRepository(builder.build()), server);
  }

  private static final String ONE_CONTOUR =
      """
      {
        "type": "FeatureCollection",
        "features": [
          {
            "type": "Feature",
            "geometry": {
              "type": "Polygon",
              "coordinates": [[[0,0],[1,0],[1,1],[0,0]]]
            }
          }
        ]
      }
      """;

  private static final String EMPTY_FEATURE_COLLECTION =
      """
      {
        "type": "FeatureCollection",
        "features": []
      }
      """;

  private static final String NULL_GEOMETRY =
      """
      {
        "type": "FeatureCollection",
        "features": [
          {
            "type": "Feature",
            "geometry": null
          }
        ]
      }
      """;

  @Test
  void isochroneReturnsGeometry() throws Exception {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(ORS_ENDPOINT))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(ONE_CONTOUR, MediaType.APPLICATION_JSON));

    var geometry = cs.client.fetchIsochrone(1.0, 1.0, 600);
    cs.server.verify();
    var expected = "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[1,0],[1,1],[0,0]]]}";
    assertEquals(GeoJson.toGeometry(JSON.readTree(expected)), geometry);
    assertEquals("Polygon", geometry.getGeometryType());
  }

  @Test
  void emptyFeatureCollectionThrows() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(ORS_ENDPOINT))
        .andRespond(withSuccess(EMPTY_FEATURE_COLLECTION, MediaType.APPLICATION_JSON));

    var e = assertThrows(UpstreamException.class, () -> cs.client.fetchIsochrone(1.0, 1.0, 600));
    assertTrue(e.detailForLogs().contains("no isochrone polygon"));
  }

  @Test
  void nullGeometryThrows() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(ORS_ENDPOINT))
        .andRespond(withSuccess(NULL_GEOMETRY, MediaType.APPLICATION_JSON));

    var e = assertThrows(UpstreamException.class, () -> cs.client.fetchIsochrone(1.0, 1.0, 600));
    assertTrue(e.detailForLogs().contains("no isochrone polygon"));
  }

  @Test
  void upstreamErrorIsReadable() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(ORS_ENDPOINT))
        .andRespond(
            withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("bad upstream")
                .contentType(MediaType.APPLICATION_JSON));

    var e = assertThrows(UpstreamException.class, () -> cs.client.fetchIsochrone(1.0, 1.0, 600));
    assertTrue(e.detailForLogs().contains("500"));
    assertTrue(e.detailForLogs().contains("bad upstream"));
  }

  @Test
  void emptySuccessBodyIsUpstreamFailure() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(ORS_ENDPOINT))
        .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

    var e = assertThrows(UpstreamException.class, () -> cs.client.fetchIsochrone(1.0, 1.0, 600));
    assertTrue(e.detailForLogs().contains("empty response"));
  }
}
