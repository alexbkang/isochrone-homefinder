package io.github.alexbkang.isochronehomefinder.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class UpstreamException extends ErrorResponseException {

  private final String detail;

  public UpstreamException(String detail) {
    this(detail, null);
  }

  public UpstreamException(String detail, Throwable cause) {
    super(HttpStatus.BAD_GATEWAY, problem(), cause);
    this.detail = detail;
  }

  public String detail() {
    return detail;
  }

  private static ProblemDetail problem() {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, "Temporarily unavailable.");
  }
}
