package io.github.alexbkang.isochronehomefinder.geocoding;

/** An optional ranking focus point for a geocode query. */
public sealed interface Focus {

  record At(double lon, double lat) implements Focus {}

  record None() implements Focus {}
}
