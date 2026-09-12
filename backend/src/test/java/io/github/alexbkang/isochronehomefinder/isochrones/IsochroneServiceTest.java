package io.github.alexbkang.isochronehomefinder.isochrones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.web.server.ResponseStatusException;

class IsochroneServiceTest {

  private static final GeometryFactory GEOMETRY = new GeometryFactory();

  private static Geometry square(double centerX, double centerY, double radius) {
    return GEOMETRY.createPolygon(
        new Coordinate[] {
          new Coordinate(centerX - radius, centerY - radius),
          new Coordinate(centerX + radius, centerY - radius),
          new Coordinate(centerX + radius, centerY + radius),
          new Coordinate(centerX - radius, centerY + radius),
          new Coordinate(centerX - radius, centerY - radius)
        });
  }

  private static IsochroneRepository centeredSquares(double radius) throws Exception {
    var ors = mock(IsochroneRepository.class);
    when(ors.fetchIsochrone(anyDouble(), anyDouble(), anyLong()))
        .thenAnswer(
            (InvocationOnMock invocation) ->
                square(invocation.getArgument(0), invocation.getArgument(1), radius));
    return ors;
  }

  private static Anchor place(double lng, double lat, int min) {
    return new Anchor(lng, lat, min);
  }

  @Test
  void emptyGroupIsRejectedBeforeFetching() throws Exception {

    var ors = centeredSquares(0.5);
    var service = new IsochroneService(ors);
    org.junit.jupiter.api.Assertions.assertThrows(
        ResponseStatusException.class, () -> service.composeReachableRegion(List.of(List.of())));
    verifyNoInteractions(ors);
  }

  @Test
  void duplicateAlternativeInGroupIsIgnoredNotFetched() throws Exception {

    var ors = centeredSquares(0.5);
    var service = new IsochroneService(ors);
    var result =
        service.composeReachableRegion(
            List.of(List.of(place(0, 0, 20), place(0, 0, 20), place(0.6, 0, 20))));
    assertNotNull(result.region());
    assertEquals(2, result.zones().size());
    verify(ors, times(2)).fetchIsochrone(anyDouble(), anyDouble(), anyLong());
  }

  @Test
  void orGroupUnionsAlternatives() throws Exception {

    var ors = centeredSquares(0.5);
    var service = new IsochroneService(ors);
    var result =
        service.composeReachableRegion(List.of(List.of(place(0, 0, 20), place(0.6, 0, 20))));
    assertNotNull(result.region());
    assertEquals(2, result.zones().size());
  }

  @Test
  void disjointOrGroupIsAMultiPolygon() throws Exception {

    var ors = centeredSquares(0.5);
    var service = new IsochroneService(ors);
    var result =
        service.composeReachableRegion(List.of(List.of(place(0, 0, 20), place(50, 50, 20))));
    assertNotNull(result.region());
    assertEquals("MultiPolygon", result.region().getGeometryType());
    assertEquals(2, result.zones().size());
  }

  @Test
  void andGroupsIntersect() throws Exception {

    var ors = centeredSquares(1.0);
    var service = new IsochroneService(ors);
    var result =
        service.composeReachableRegion(
            List.of(List.of(place(0, 0, 20)), List.of(place(0.5, 0, 20))));
    assertNotNull(result.region());
    assertEquals(2, result.zones().size());
  }

  @Test
  void disjointAndGroupsCollapseAndShortCircuit() throws Exception {
    var ors = centeredSquares(0.5);
    var service = new IsochroneService(ors);

    var result =
        service.composeReachableRegion(
            List.of(
                List.of(place(0, 0, 20)), List.of(place(50, 50, 20)), List.of(place(1, 1, 20))));
    assertNull(result.region());
    assertEquals(2, result.zones().size());

    verify(ors, times(2)).fetchIsochrone(anyDouble(), anyDouble(), anyLong());
  }
}
