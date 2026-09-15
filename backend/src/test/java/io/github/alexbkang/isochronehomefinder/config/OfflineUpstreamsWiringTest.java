package io.github.alexbkang.isochronehomefinder.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.alexbkang.isochronehomefinder.geocoding.GeocodeRepository;
import io.github.alexbkang.isochronehomefinder.geocoding.OfflineGeocodeRepository;
import io.github.alexbkang.isochronehomefinder.isochrones.IsochroneRepository;
import io.github.alexbkang.isochronehomefinder.isochrones.OfflineIsochroneRepository;
import io.github.alexbkang.isochronehomefinder.listings.ListingRepository;
import io.github.alexbkang.isochronehomefinder.listings.OfflineListingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * The {@code offline} profile wires the canned adapters and needs no API keys, so the live
 * {@code @ConfigurationProperties} must not be registered.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("offline")
class OfflineUpstreamsWiringTest {

  @Autowired ApplicationContext context;

  @Test
  void wiresOfflineAdaptersWithoutKeys() {
    assertInstanceOf(OfflineGeocodeRepository.class, context.getBean(GeocodeRepository.class));
    assertInstanceOf(OfflineIsochroneRepository.class, context.getBean(IsochroneRepository.class));
    assertInstanceOf(OfflineListingRepository.class, context.getBean(ListingRepository.class));
    assertEquals(0, context.getBeanNamesForType(OrsProperties.class).length);
    assertEquals(0, context.getBeanNamesForType(RealtyProperties.class).length);
  }
}
