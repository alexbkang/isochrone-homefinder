package io.github.alexbkang.isochronehomefinder.isochrones;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record Anchor(
    @DecimalMin("-180") @DecimalMax("180") double lng,
    @DecimalMin("-90") @DecimalMax("90") double lat,
    @Min(1) @Max(60) int min) {}
