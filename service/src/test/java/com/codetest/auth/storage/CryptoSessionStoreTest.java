package com.codetest.auth.storage;

import org.junit.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import javax.crypto.spec.SecretKeySpec;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class CryptoSessionStoreTest {

  @Test
  public void basicTest() {
    Clock clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
    CryptoSessionStore cryptoSessionStore = new CryptoSessionStore(
        clock,
        2,
        new SecretKeySpec("0123456789012345".getBytes(), "AES"));

    String token = cryptoSessionStore.createSessionToken("someuser");
    assertTrue(cryptoSessionStore.isValidToken("someuser", token));
  }

  @Test
  public void basicTestFailed() {
    Clock clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
    CryptoSessionStore cryptoSessionStore = new CryptoSessionStore(
        clock,
        2,
        new SecretKeySpec("0123456789012345".getBytes(), "AES"));

    String token = cryptoSessionStore.createSessionToken("someuser");
    assertTrue(cryptoSessionStore.isValidToken("someuser2", token));
  }

  @Test
  public void testExpiration() {
    Clock clock = Mockito.mock(Clock.class);
    Instant now = Instant.now();
    Instant futureInstant = now.plus(3, ChronoUnit.DAYS);
    when(clock.instant()).thenReturn(now, futureInstant);
    when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
    CryptoSessionStore cryptoSessionStore = new CryptoSessionStore(
        clock,
        2,
        new SecretKeySpec("0123456789012345".getBytes(), "AES"));

    String token = cryptoSessionStore.createSessionToken("someuser");

    // Next call to clocl.instant will return a date 3 days later
    assertFalse(cryptoSessionStore.isValidToken("someuser", token));
  }
}
