package com.codetest.auth.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;

import com.codetest.auth.util.CryptoUtil;
import com.codetest.auth.util.TimeUtil;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import java.time.Clock;
import java.time.LocalDateTime;

import javax.crypto.spec.SecretKeySpec;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Create tokens signed by the server. This removes the need for storage.
 *
 * SIGNATURE = BASE64(AES(SHA-256(username | expiry), ServerKey))
 * SESSION_TOKEN = BASE64(username) | BASE64(expiry) | SIGNATURE
 *
 * TODO: add user id to signature?
 *
 * This is a simplified version, for a production service use this
 * implementation instead:
 *
 *   A Secure Cookie Protocol. Alex X. Liu1, Jason M. Kovacs:
 *   https://www.cse.msu.edu/~alexliu/publications/Cookie/cookie.pdf
 */
public class CryptoSessionStore implements SessionStore {

  private static final Logger LOG = getLogger(CryptoSessionStore.class);
  private static final int SESSION_PARTS = 3;
  private final int expirationDays;
  private final Clock clock;
  private final SecretKeySpec serverKey;

  public CryptoSessionStore(int expirationDays, final String serverKeyText) {
    this(Clock.systemUTC(), expirationDays, serverKeyText);
  }

  @VisibleForTesting
  CryptoSessionStore(final Clock clock, int expirationDays, final String serverKeyText) {
    this.clock = clock;
    this.expirationDays = expirationDays;
    this.serverKey = new SecretKeySpec(serverKeyText.getBytes(), "AES");;
  }

  @Override
  public String createSessionToken(final String username) {
    LocalDateTime expirationTime = TimeUtil.now(clock).plusDays(expirationDays);
    String expirationTimestamp = TimeUtil.timestamp(expirationTime);

    String signature = createSignature(username, expirationTimestamp);
    String sessionIdBase = Joiner.on('|')
        .join(base64(username),
              base64(expirationTimestamp),
              signature);

    return sessionIdBase;
  }

  @Override
  public boolean isValidToken(final String username, final String sessionToken) {
    try {
      String[] parts = sessionToken.split("\\|");
      if (parts.length != SESSION_PARTS) {
        LOG.warn("Failed to decode signature: {}", sessionToken);
        return false;
      }

      String tokenUsername = fromBase64(parts[0]);
      String tokenExpirationTimestamp = fromBase64(parts[1]);
      LocalDateTime tokenExpirationTime = TimeUtil.parseDatetime(tokenExpirationTimestamp);
      String signatureBase64 = parts[2];

      LocalDateTime currentTime = TimeUtil.now(clock);
      if (tokenExpirationTime.isBefore(currentTime)) {
        LOG.info("Expired session id: {}. Expiration time: {}", sessionToken, tokenExpirationTime);
        return false;
      }

      if (!verifySignature(tokenUsername, tokenExpirationTimestamp, signatureBase64)) {
        LOG.warn("Failed to verify signature: {}", sessionToken);
        return false;
      }

      return true;

    } catch (Exception e) {
      LOG.warn("Failed to decode signature: {}", sessionToken, e);
      return false;
    }
  }

  /**
   * Create a signature of the session token by encrypting a hash
   * of the username and expiration time and encoding the result
   * as base 64
   */
  private String createSignature(String username, String expirationTimestamp) {
    String signatureBase = Joiner.on('|').join(username, expirationTimestamp);
    byte[] signatureHash = DigestUtils.sha256(signatureBase);
    byte[] signatureEncrypted = CryptoUtil.encrypt(signatureHash, serverKey);
    return base64(signatureEncrypted);
  }

  /**
   * Verify the signature by recomputing it for a given username and expiration
   * time and compare it to the given one.
   */
  private boolean verifySignature(String username, String expirationTime, String signature) {
    String computedSignature = createSignature(username, expirationTime);
    return CryptoUtil.secureEquals(signature, computedSignature);
  }

  private static String base64(String text) {
    return  Base64.encodeBase64String(text.getBytes(Charsets.UTF_8));
  }

  private static String base64(byte[] bytes) {
    return Base64.encodeBase64String(bytes);
  }

  private static String fromBase64(String text) {
    return new String(Base64.decodeBase64(text.getBytes(Charsets.UTF_8)),
                      Charsets.UTF_8);
  }
}
