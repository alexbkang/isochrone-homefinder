package io.github.alexbkang.isochronehomefinder.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class UpstreamException extends ErrorResponseException {

  private final String detailForLogs;

  public UpstreamException(String detailForLogs) {
    this(detailForLogs, null);
  }

  public UpstreamException(String detailForLogs, Throwable cause) {
    super(HttpStatus.BAD_GATEWAY, problem(), cause);
    this.detailForLogs = detailForLogs;
  }

  public String detailForLogs() {
    return detailForLogs;
  }

  private static ProblemDetail problem() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_GATEWAY, "Temporarily unavailable.");
  }
}
