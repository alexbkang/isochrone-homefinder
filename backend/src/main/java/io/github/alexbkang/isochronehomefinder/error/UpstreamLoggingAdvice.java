package io.github.alexbkang.isochronehomefinder.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UpstreamLoggingAdvice {

  private static final Logger log = LoggerFactory.getLogger(UpstreamLoggingAdvice.class);

  @ExceptionHandler({RestClientException.class, UpstreamException.class})
  ProblemDetail log(Exception e) {
    log.error("Upstream request failed", e);
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, e.getMessage());
  }
}
