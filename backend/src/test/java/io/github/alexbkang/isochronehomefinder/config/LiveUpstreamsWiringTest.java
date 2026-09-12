package io.github.alexbkang.isochronehomefinder.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.alexbkang.isochronehomefinder.geocoding.GeocodeRepository;
import io.github.alexbkang.isochronehomefinder.geocoding.OrsGeocodeRepository;
import io.github.alexbkang.isochronehomefinder.isochrones.IsochroneRepository;
import io.github.alexbkang.isochronehomefinder.isochrones.OrsIsochroneRepository;
import io.github.alexbkang.isochronehomefinder.listings.ListingRepository;
import io.github.alexbkang.isochronehomefinder.listings.RealtyListingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

/** With no {@code offline} profile, the live vendor adapters are wired. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {"ors.key=test", "realtyapi.key=test"})
class LiveUpstreamsWiringTest {

  @Autowired ApplicationContext context;

  @Test
  void wiresLiveAdapters() {
    assertInstanceOf(OrsGeocodeRepository.class, context.getBean(GeocodeRepository.class));
    assertInstanceOf(OrsIsochroneRepository.class, context.getBean(IsochroneRepository.class));
    assertInstanceOf(RealtyListingRepository.class, context.getBean(ListingRepository.class));
  }
}
