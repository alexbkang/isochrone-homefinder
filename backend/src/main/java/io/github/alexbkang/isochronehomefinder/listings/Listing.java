package io.github.alexbkang.isochronehomefinder.listings;

import java.util.List;

public record Listing(
    long zpid,
    double lat,
    double lng,
    long priceValue,
    Integer beds,
    Integer baths,
    Double sqft,
    String type,
    Integer year,
    Integer daysOnMarket,
    Double lotSize,
    String lotSizeUnit,
    String street,
    String city,
    String state,
    String zip,
    List<String> photos) {}
