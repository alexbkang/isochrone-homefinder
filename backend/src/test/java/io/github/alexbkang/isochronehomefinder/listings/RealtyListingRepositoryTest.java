package io.github.alexbkang.isochronehomefinder.listings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import io.github.alexbkang.isochronehomefinder.error.UpstreamException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RealtyListingRepositoryTest {

  private static final String ENDPOINT = RealtyListingRepository.REALTYAPI_ENDPOINT;
  private static final GeometryFactory GEOMETRY = new GeometryFactory();

  private record ClientAndServer(
      RealtyListingRepository client, MockRestServiceServer server) {}

  private static ClientAndServer clientAndServer() {
    var builder = RestClient.builder();
    var server = MockRestServiceServer.bindTo(builder).build();
    return new ClientAndServer(new RealtyListingRepository(builder.build()), server);
  }

  private static final String BODY =
      "{\"searchResults\":[{\"property\":{"
          + "\"zpid\":1,"
          + "\"location\":{\"latitude\":34.05,\"longitude\":-118.35},"
          + "\"price\":{\"value\":1250000},"
          + "\"address\":{\"streetAddress\":\"1 Main St\"}}}]}";

  // Answers the next request with `body` and returns the decoded listings.
  private static List<Listing> decode(String body) {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(Matchers.containsString(ENDPOINT + "?polygon=")))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    var got = cs.client.findWithin(square(-118.4, -118.3));
    cs.server.verify();
    return got;
  }

  private static String resource(String path) throws Exception {
    try (var in = RealtyListingRepositoryTest.class.getResourceAsStream(path)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static Polygon square(double minLon, double maxLon) {
    var minLat = 34.0;
    var maxLat = 34.2;
    return GEOMETRY.createPolygon(
        new Coordinate[] {
          new Coordinate(minLon, minLat),
          new Coordinate(maxLon, minLat),
          new Coordinate(maxLon, maxLat),
          new Coordinate(minLon, maxLat),
          new Coordinate(minLon, minLat)
        });
  }

  @Test
  void zillowPolygonReversesToLatLon() {
    var ring =
        GEOMETRY.createLineString(
            new Coordinate[] {
              new Coordinate(-118.4, 34.0),
              new Coordinate(-118.3, 34.0),
              new Coordinate(-118.3, 34.1),
              new Coordinate(-118.4, 34.0)
            });
    assertEquals(
        "34.0 -118.4,34.0 -118.3,34.1 -118.3,34.0 -118.4",
        RealtyListingRepository.zillowPolygon(ring));
  }

  @Test
  void successfulResponseDecodesToListings() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(Matchers.containsString(ENDPOINT + "?polygon=")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

    var got = cs.client.findWithin(square(-118.4, -118.3));
    cs.server.verify();
    assertEquals(1, got.size());
    assertEquals(1, got.get(0).zpid());
    assertEquals(1250000, got.get(0).priceValue());
    assertEquals("1 Main St", got.get(0).street());
  }

  @Test
  void mapsRealZillowPayload() throws Exception {
    var got = decode(resource("/zillow.json"));
    assertEquals(3, got.size(), "all three listings have usable coordinates");

    var l = got.get(0);

    assertEquals(20516513, l.zpid());
    assertTrue(l.lat() != 0 && l.lng() != 0);
    assertEquals("9000 Dorrington Ave", l.street());
    assertEquals("West Hollywood", l.city());
    assertEquals("CA", l.state());
    assertEquals("90048", l.zip());
    assertEquals(3195000, l.priceValue());
    assertEquals(3, l.beds());
    assertEquals(3, l.baths());
    assertEquals(1848.0, l.sqft());
    assertEquals("singleFamily", l.type());
    assertEquals(1927, l.year());
    assertEquals(1, l.daysOnMarket());
    assertTrue(l.lotSize() > 0);
    assertEquals("squareFeet", l.lotSizeUnit());
    assertFalse(l.photos().isEmpty());
    assertEquals(
        "https://photos.zillowstatic.com/fp/f2c9ad1f0645254c313ebcb2e303cec2-p_c.jpg",
        l.photos().get(0));
  }

  @Test
  void mapsEveryWrappedListing() {
    var got =
        decode(
            "{\"searchResults\":[{\"property\":{\"zpid\":1,"
                + "\"location\":{\"latitude\":34.05,\"longitude\":-118.35}}},"
                + "{\"property\":{\"zpid\":2}}]}");
    assertEquals(2, got.size());
    assertEquals(1, got.get(0).zpid());
    assertEquals(34.05, got.get(0).lat());
    assertEquals(-118.35, got.get(0).lng());
    assertEquals(2, got.get(1).zpid());
  }

  @Test
  void photosComeFromTheGalleryAsIs() {
    var got =
        decode(
            "{\"searchResults\":[{\"property\":{\"zpid\":1,\"media\":{"
                + "\"allPropertyPhotos\":{\"medium\":[\"m\",\"x\"],\"highResolution\":[\"y\"]}}}}]}");
    assertEquals(List.of("m", "x"), got.get(0).photos());
  }

  @Test
  void photosFallBackToHighResolutionWhenMediumIsMissing() {
    var got =
        decode(
            "{\"searchResults\":[{\"property\":{\"zpid\":1,\"media\":{"
                + "\"allPropertyPhotos\":{\"highResolution\":[\"y\",\"z\"]}}}}]}");
    assertEquals(List.of("y", "z"), got.get(0).photos());
  }

  @Test
  void emptySearchResultsIsFine() {
    assertTrue(decode("{\"searchResults\":[]}").isEmpty());
  }

  @Test
  void payloadWithoutEnvelopeIsEmpty() {
    assertTrue(decode("{\"data\":[]}").isEmpty());
    assertTrue(decode("[]").isEmpty());
    assertTrue(decode("null").isEmpty());
  }

  @Test
  void multiPolygonQueriesEachRingAndConcatenates() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(Matchers.containsString(ENDPOINT + "?polygon=")))
        .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));
    cs.server
        .expect(requestTo(Matchers.containsString(ENDPOINT + "?polygon=")))
        .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

    var region = GEOMETRY.createMultiPolygon(new Polygon[] {square(-118.4, -118.3), square(-117.8, -117.6)});
    var got = cs.client.findWithin(region);
    cs.server.verify();
    assertEquals(2, got.size(), "one query per ring, both results concatenated");
  }

  @Test
  void upstreamErrorBodyIsPassedThrough() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(Matchers.containsString(ENDPOINT)))
        .andRespond(
            withStatus(HttpStatus.PAYMENT_REQUIRED)
                .body("{\"error\":\"monthly quota\"}")
                .contentType(MediaType.APPLICATION_JSON));

    var e = assertThrows(UpstreamException.class, () -> cs.client.findWithin(square(-118.4, -118.3)));
    assertTrue(e.detailForLogs().contains("{\"error\":\"monthly quota\"}"));
  }

  @Test
  void emptySuccessBodyIsUpstreamFailure() {
    var cs = clientAndServer();
    cs.server
        .expect(requestTo(Matchers.containsString(ENDPOINT)))
        .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

    var e = assertThrows(UpstreamException.class, () -> cs.client.findWithin(square(-118.4, -118.3)));
    assertTrue(e.detailForLogs().contains("empty response"));
  }
}
