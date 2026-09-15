package io.github.alexbkang.isochronehomefinder.config;

import io.github.alexbkang.isochronehomefinder.geocoding.GeocodeRepository;
import io.github.alexbkang.isochronehomefinder.geocoding.OfflineGeocodeRepository;
import io.github.alexbkang.isochronehomefinder.isochrones.IsochroneRepository;
import io.github.alexbkang.isochronehomefinder.isochrones.OfflineIsochroneRepository;
import io.github.alexbkang.isochronehomefinder.listings.Listing;
import io.github.alexbkang.isochronehomefinder.listings.ListingRepository;
import io.github.alexbkang.isochronehomefinder.listings.OfflineListingRepository;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("offline")
class OfflineConfiguration {

  @Bean
  OfflineData offlineData() {
    return OfflineData.synthetic();
  }

  @Bean
  GeocodeRepository geocodeRepository() {
    return new OfflineGeocodeRepository();
  }

  @Bean
  IsochroneRepository isochroneRepository(OfflineData data) {
    return new OfflineIsochroneRepository(data.region());
  }

  @Bean
  ListingRepository listingRepository(OfflineData data) {
    return new OfflineListingRepository(data.homes());
  }

  record OfflineData(List<Listing> homes, Geometry region) {

    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    private static final Geometry REGION =
        GEOMETRY.createPolygon(
            new Coordinate[] {
              new Coordinate(-118.2637, 34.0292),
              new Coordinate(-118.2237, 34.0292),
              new Coordinate(-118.2237, 34.0692),
              new Coordinate(-118.2637, 34.0692),
              new Coordinate(-118.2637, 34.0292)
            });

    private static final List<Listing> HOMES =
        List.of(
            new Listing(
                9000,
                34.0462,
                -118.2557,
                300000L,
                1,
                1,
                700.0,
                "singleFamily",
                1950,
                1,
                4000.0,
                "squareFeet",
                "1st St",
                "Demo",
                "CA",
                "90000",
                List.of()),
            new Listing(
                9001,
                34.0462,
                -118.2497,
                347000L,
                2,
                2,
                830.0,
                "condo",
                1957,
                2,
                4300.0,
                "squareFeet",
                "2nd St",
                "Demo",
                "CA",
                "90000",
                List.of()),
            new Listing(
                9002,
                34.0462,
                -118.2437,
                394000L,
                3,
                3,
                960.0,
                "townhouse",
                1964,
                3,
                4600.0,
                "squareFeet",
                "3rd St",
                "Demo",
                "CA",
                "90000",
                List.of()),
            new Listing(
                9003,
                34.0462,
                -118.2377,
                441000L,
                4,
                1,
                1090.0,
                "singleFamily",
                1971,
                4,
                4900.0,
                "squareFeet",
                "4th St",
                "Demo",
                "CA",
                "90000",
                List.of()),
            new Listing(
                9004,
                34.0462,
                -118.2317,
                488000L,
                1,
                2,
                1220.0,
                "condo",
                1978,
                5,
                5200.0,
                "squareFeet",
                "5th St",
                "Demo",
                "CA",
                "90000",
                List.of()),
            new Listing(
                9005,
                34.0522,
                -118.2557,
                535000L,
                2,
                3,
                1350.0,
                "singleFamily",
                1985,
                6,
                5500.0,
                "squareFeet",
                "6th St",
                "Demo",
                "CA",
                "90000",
                List.of()),
            new Listing(
                9006,
                34.0522,
                -118.2497,
                582000L,
                3,
                1,
                1480.0,
                "condo",
                1992,
                7,
                5800.0,
                "squareFeet",
                "7th St",
                "Demo",
                "CA",
                "90000",
                List.of()),
            new Listing(
                9007,
                34.0522,
                -118.2437,
                629000L,
                4,
                2,
                1610.0,
                "townhouse",
                1999,
                8,
                6100.0,
                "squareFeet",
                "8th St",
                "Demo",
                "CA",
                "90000",
                List.of()),
            new Listing(
                9008,
                34.0522,
                -118.2377,
                676000L,
                1,
                3,
                1740.0,
                "singleFamily",
                2006,
                9,
                6400.0,
                "squareFeet",
                "9th St",
                "Demo",
                "CA",
                "90000",
                List.of()),
            new Listing(
                9009,
                34.0522,
                -118.2317,
                723000L,
                2,
                1,
                1870.0,
                "condo",
                2013,
                10,
                6700.0,
                "squareFeet",
                "10th St",
                "Demo",
                "CA",
                "90000",
                List.of()));

    static OfflineData synthetic() {
      return new OfflineData(HOMES, REGION);
    }
  }
}
