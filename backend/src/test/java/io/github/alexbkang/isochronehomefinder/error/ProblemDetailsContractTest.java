package io.github.alexbkang.isochronehomefinder.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.github.alexbkang.isochronehomefinder.geocoding.GeocodeRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * The real autoconfigured contract: with {@code spring.mvc.problemdetails.enabled=true}
 * Spring's ProblemDetailsExceptionHandler renders framework 400s, our ErrorResponse
 * exceptions, and upstream failures as ProblemDetail. The standalone controller tests
 * use a stand-in advice, so this is what actually proves the wiring.
 */
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("offline")
class ProblemDetailsContractTest {

  @LocalServerPort int port;

  @MockitoBean GeocodeRepository geocodeRepository;

  private HttpResponse<String> get(String path) throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void aFramework400IsProblemDetail() throws Exception {
    // Missing required `limit` on autocomplete: handled by Spring's handler, not ours.
    var response = get("/geocode/autocomplete?text=austin");
    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("\"status\":400"), response.body());
    assertTrue(response.body().contains("\"detail\""), response.body());
  }

  @Test
  void ourBadRequestIsProblemDetail() throws Exception {
    var response = get("/geocode?text=");
    assertEquals(400, response.statusCode());
    assertTrue(response.body().contains("\"status\":400"), response.body());
    assertTrue(response.body().contains("text must not be blank"), response.body());
  }

  @Test
  void ourNotFoundIsProblemDetail() throws Exception {
    when(geocodeRepository.search(anyString(), any(), any())).thenReturn(Optional.empty());

    var response = get("/geocode?text=austin");
    assertEquals(404, response.statusCode(), response.body());
    assertTrue(response.body().contains("No location matched \\\"austin\\\"."), response.body());
  }

  @Test
  void upstreamFailureIs502LogsProviderTextAndDoesNotLeakIt(CapturedOutput output)
      throws Exception {
    when(geocodeRepository.search(anyString(), any(), any()))
        .thenThrow(new UpstreamException("ORS geocode returned HTTP 500: provider secret"));

    var response = get("/geocode?text=austin");
    assertEquals(502, response.statusCode(), response.body());
    assertTrue(
        response
            .body()
            .contains("Temporarily unavailable."),
        response.body());
    assertFalse(response.body().contains("provider secret"), response.body());
    assertTrue(output.getOut().contains("provider secret"), output.getOut());
  }
}
