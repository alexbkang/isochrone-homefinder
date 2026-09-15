package io.github.alexbkang.isochronehomefinder.error;

public class UpstreamException extends RuntimeException {

  public UpstreamException(String message) {
    super(message);
  }

  public UpstreamException(String message, Throwable cause) {
    super(message, cause);
  }
}
