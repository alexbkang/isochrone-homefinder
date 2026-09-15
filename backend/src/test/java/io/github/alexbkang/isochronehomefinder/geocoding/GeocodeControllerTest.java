package io.github.alexbkang.isochronehomefinder.geocoding;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.alexbkang.isochronehomefinder.error.ProblemDetailsTestAdvice;
import io.github.alexbkang.isochronehomefinder.geocoding.GeocodeRepository.Hit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GeocodeControllerTest {

  private static MockMvc mvc(GeocodeRepository repository) {
    return MockMvcBuilders.standaloneSetup(new GeocodeController(repository))
        .setControllerAdvice(new ProblemDetailsTestAdvice())
        .build();
  }

  @Test
  void geocodeReturnsTheSingleHit() throws Exception {
    var repository = mock(GeocodeRepository.class);
    when(repository.search("austin", new Focus.None()))
        .thenReturn(Optional.of(new Hit("Austin", "Texas", -97.74, 30.27)));
    mvc(repository)
        .perform(get("/geocode").param("text", "austin"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Austin"))
        .andExpect(jsonPath("$.region").value("Texas"))
        .andExpect(jsonPath("$.lon").value(-97.74));
  }

  @Test
  void geocodeWithNoMatchIs404() throws Exception {
    var repository = mock(GeocodeRepository.class);
    when(repository.search("nowhere", new Focus.None())).thenReturn(Optional.empty());
    mvc(repository)
        .perform(get("/geocode").param("text", "nowhere"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.detail").value("No location matched \"nowhere\"."));
  }

  @Test
  void autocompleteReturnsHits() throws Exception {
    var repository = mock(GeocodeRepository.class);
    when(repository.autocomplete("austin", 5, new Focus.None()))
        .thenReturn(List.of(new Hit("Austin", "Texas", -97.74, 30.27)));
    mvc(repository)
        .perform(get("/geocode/autocomplete").param("text", "austin").param("limit", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Austin"));
  }

  @Test
  void blankTextIs400AndNeverReachesTheClient() throws Exception {
    var repository = mock(GeocodeRepository.class);
    var mvc = mvc(repository);
    mvc.perform(get("/geocode").param("text", "   ")).andExpect(status().isBadRequest());
    verifyNoInteractions(repository);
  }

  @Test
  void focusIsPassedThrough() throws Exception {
    var repository = mock(GeocodeRepository.class);
    when(repository.search("austin", new Focus.At(-97.74, 30.27)))
        .thenReturn(Optional.of(new Hit("Austin", "Texas", -97.74, 30.27)));
    mvc(repository)
        .perform(
            get("/geocode").param("text", "austin").param("lon", "-97.74").param("lat", "30.27"))
        .andExpect(status().isOk());
  }

  @Test
  void partialFocusIs400() throws Exception {
    var repository = mock(GeocodeRepository.class);
    var mvc = mvc(repository);
    mvc.perform(get("/geocode").param("text", "austin").param("lon", "-97.74"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(repository);
  }

  @Test
  void outOfRangeLimitIs400() throws Exception {
    var repository = mock(GeocodeRepository.class);
    var mvc = mvc(repository);
    mvc.perform(get("/geocode/autocomplete").param("text", "austin").param("limit", "50"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(repository);
  }
}
