package io.github.alexbkang.isochronehomefinder.error;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Stands in for the {@code ProblemDetailsExceptionHandler} Boot auto-configures when {@code
 * spring.mvc.problemdetails.enabled=true}. The standalone MockMvc tests do not load Boot
 * auto-configuration, so they register this instead.
 */
@RestControllerAdvice
public class ProblemDetailsTestAdvice extends ResponseEntityExceptionHandler {}
