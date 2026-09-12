package io.github.alexbkang.isochronehomefinder.listings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.alexbkang.isochronehomefinder.error.UpstreamException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ListingCacheTest {

  private static final class MutableClock extends Clock {
    private Instant now = Instant.EPOCH;

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }

    void advance(Duration d) {
      now = now.plus(d);
    }
  }

  private record Harness(ListingCache cache, MutableClock clock, AtomicInteger loads) {}

  private static Harness harness(Duration ttl, long cap) {
    var clock = new MutableClock();
    return new Harness(new ListingCache(ttl, cap, clock), clock, new AtomicInteger());
  }

  private static List<Listing> canned() {
    return List.of();
  }

  @Test
  void repeatWithinTtlServedFromCache() {
    var h = harness(Duration.ofHours(1), 512);
    ListingCache.Loader loader =
        () -> {
          h.loads.incrementAndGet();
          return canned();
        };
    h.cache.get("k", loader);
    h.cache.get("k", loader);
    assertEquals(1, h.loads.get(), "identical key within TTL must not re-load");
  }

  @Test
  void differentKeyMisses() {
    var h = harness(Duration.ofHours(1), 512);
    h.cache.get(
        "a",
        () -> {
          h.loads.incrementAndGet();
          return canned();
        });
    h.cache.get(
        "b",
        () -> {
          h.loads.incrementAndGet();
          return canned();
        });
    assertEquals(2, h.loads.get());
  }

  @Test
  void expiryTriggersRefetch() {
    var h = harness(Duration.ofHours(1), 512);
    ListingCache.Loader loader =
        () -> {
          h.loads.incrementAndGet();
          return canned();
        };
    h.cache.get("k", loader);
    h.clock.advance(Duration.ofMinutes(30));
    h.cache.get("k", loader);
    h.clock.advance(Duration.ofMinutes(31));
    h.cache.get("k", loader);
    assertEquals(2, h.loads.get());
  }

  @Test
  void errorIsNotCached() {
    var h = harness(Duration.ofHours(1), 512);
    var calls = new AtomicInteger();
    ListingCache.Loader loader =
        () -> {
          if (calls.incrementAndGet() == 1) {
            throw new UpstreamException("monthly quota");
          }
          h.loads.incrementAndGet();
          return canned();
        };
    assertThrows(UpstreamException.class, () -> h.cache.get("k", loader));
    h.cache.get("k", loader);
    assertEquals(1, h.loads.get());
  }

  @Test
  void zeroTtlDisablesCaching() {
    var h = harness(Duration.ZERO, 512);
    ListingCache.Loader loader =
        () -> {
          h.loads.incrementAndGet();
          return canned();
        };
    h.cache.get("k", loader);
    h.cache.get("k", loader);
    assertEquals(2, h.loads.get());
  }
}
