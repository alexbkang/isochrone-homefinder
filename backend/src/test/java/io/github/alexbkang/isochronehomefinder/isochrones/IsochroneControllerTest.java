package io.github.alexbkang.isochronehomefinder.isochrones;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.alexbkang.isochronehomefinder.geometry.GeoJson;
import io.github.alexbkang.isochronehomefinder.error.ProblemDetailsTestAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

class IsochroneControllerTest {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static final String POLY =
      "{\"type\":\"Polygon\",\"coordinates\":[[[-118.4,34.0],[-118.3,34.0],[-118.3,34.1],[-118.4,34.1],[-118.4,34.0]]]}";

  private static MockMvc mvc(IsochroneRepository ors) {
    return MockMvcBuilders.standaloneSetup(new IsochroneController(new IsochroneService(ors)))
        .setControllerAdvice(new ProblemDetailsTestAdvice())
        .build();
  }

  private static String place(int min, double lat, double lng) {
    return "{\"min\":" + min + ",\"lat\":" + lat + ",\"lng\":" + lng + "}";
  }

  @Test
  void successReturnsRegionAndZones() throws Exception {
    var ors = mock(IsochroneRepository.class);
    when(ors.fetchIsochrone(
            org.mockito.ArgumentMatchers.anyDouble(),
            org.mockito.ArgumentMatchers.anyDouble(),
            org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(GeoJson.toGeometry(JSON.readTree(POLY)));

    var body = "[[" + place(20, 34.05, -118.35) + "],[" + place(15, 34.05, -118.35) + "]]";
    mvc(ors)
        .perform(post("/region").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.region.type").value("Polygon"))
        .andExpect(jsonPath("$.zones.length()").value(2));
  }

  @Test
  void singlePlaceIsValid() throws Exception {
    var ors = mock(IsochroneRepository.class);
    when(ors.fetchIsochrone(
            org.mockito.ArgumentMatchers.anyDouble(),
            org.mockito.ArgumentMatchers.anyDouble(),
            org.mockito.ArgumentMatchers.anyLong()))
        .thenReturn(GeoJson.toGeometry(JSON.readTree(POLY)));
    mvc(ors)
        .perform(
            post("/region")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[[" + place(20, 34.05, -118.35) + "]]"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.region.type").value("Polygon"))
        .andExpect(jsonPath("$.zones.length()").value(1));
  }

  @Test
  void invalidRequestsAre400AndNeverReachTheClient() throws Exception {
    var ors = mock(IsochroneRepository.class);
    var mvc = mvc(ors);

    var single = place(20, 34, -118);
    var sevenGroups =
        "[" + String.join(",", java.util.Collections.nCopies(7, "[" + single + "]")) + "]";

    var sixInGroup = "[[" + String.join(",", java.util.Collections.nCopies(6, single)) + "]]";
    String[] bad = {
      "[]", "[[]]", sevenGroups, sixInGroup, "[[{\"min\":61,\"lat\":34,\"lng\":-118}]]", "[[null]]",
    };
    for (var body : bad) {
      mvc.perform(post("/region").contentType(MediaType.APPLICATION_JSON).content(body))
          .andExpect(status().isBadRequest());
    }
    verifyNoInteractions(ors);
  }
}
