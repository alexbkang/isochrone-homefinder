package io.github.alexbkang.isochronehomefinder.config;

import io.github.alexbkang.isochronehomefinder.geocoding.GeocodeRepository;
import io.github.alexbkang.isochronehomefinder.geocoding.OfflineGeocodeRepository;
import io.github.alexbkang.isochronehomefinder.isochrones.IsochroneRepository;
import io.github.alexbkang.isochronehomefinder.isochrones.OfflineIsochroneRepository;
import io.github.alexbkang.isochronehomefinder.listings.ListingRepository;
import io.github.alexbkang.isochronehomefinder.listings.OfflineListingRepository;
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
}
