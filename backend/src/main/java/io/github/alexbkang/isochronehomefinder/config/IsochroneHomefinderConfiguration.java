package io.github.alexbkang.isochronehomefinder.config;

import io.github.alexbkang.isochronehomefinder.geocoding.GeocodeRepository;
import io.github.alexbkang.isochronehomefinder.geocoding.OrsGeocodeRepository;
import io.github.alexbkang.isochronehomefinder.isochrones.IsochroneRepository;
import io.github.alexbkang.isochronehomefinder.isochrones.OrsIsochroneRepository;
import io.github.alexbkang.isochronehomefinder.listings.ListingRepository;
import io.github.alexbkang.isochronehomefinder.listings.RealtyListingRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

@Validated
@ConfigurationProperties("ors")
record OrsProperties(@NotBlank String key, @NotNull Duration timeout) {}

@Validated
@ConfigurationProperties("realtyapi")
record RealtyProperties(@NotBlank String key, @NotNull Duration timeout) {}

@Configuration
@Profile("!offline")
@EnableConfigurationProperties({OrsProperties.class, RealtyProperties.class})
class IsochroneHomefinderConfiguration {

  @Bean
  GeocodeRepository geocodeRepository(OrsProperties props, Builder builder) {
    return new OrsGeocodeRepository(ors(props, builder));
  }

  @Bean
  IsochroneRepository isochroneRepository(OrsProperties props, Builder builder) {
    return new OrsIsochroneRepository(ors(props, builder));
  }

  @Bean
  ListingRepository listingRepository(RealtyProperties props, Builder builder) {
    return new RealtyListingRepository(realty(props, builder));
  }

  private static RestClient ors(OrsProperties props, Builder builder) {
    return rest(builder, "Authorization", props.key(), props.timeout());
  }

  private static RestClient realty(RealtyProperties props, Builder builder) {
    return rest(builder, "x-realtyapi-key", props.key(), props.timeout());
  }

  private static RestClient rest(Builder builder, String authHeader, String key, Duration timeout) {
    var httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    var factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(timeout);
    return builder.clone().requestFactory(factory).defaultHeader(authHeader, key).build();
  }
}
