package io.github.alexbkang.isochronehomefinder.listings;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

final class ListingCache {

  @FunctionalInterface
  interface Loader {
    List<Listing> load();
  }

  private final Duration ttl;
  private final Cache<String, List<Listing>> cache;

  ListingCache(Duration ttl, long cap, Clock clock) {
    this.ttl = ttl;
    this.cache =
        Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(cap)
            .ticker(() -> clock.instant().toEpochMilli() * 1_000_000L)
            .build();
  }

  List<Listing> get(String key, Loader loader) {
    if (ttl.isZero()) {
      return loader.load();
    }
    return cache.get(key, k -> loader.load());
  }
}
