package com.codetest.auth.storage;

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
  private final int EXPIRATION_DAYS;
  private final Clock clock;
  private final SecretKeySpec serverKey;

  public CryptoSessionStore(final Clock clock, int expirationDays, final SecretKeySpec serverKey) {
    this.clock = clock;
    EXPIRATION_DAYS = expirationDays;
    this.serverKey = serverKey;
  }

  @Override
  public String createSessionToken(final String username) {
      LocalDateTime expirationTime = TimeUtil.now(clock).plusDays(EXPIRATION_DAYS);
      String expirationTimestamp = TimeUtil.timestamp(expirationTime);

    String signatureBase = Joiner.on('|').join(username, expirationTimestamp);
    byte[] signatureHash = DigestUtils.sha256(signatureBase);
    byte[] signatureEncrypted = CryptoUtil.encrypt(signatureHash, serverKey);
    String signature = base64(signatureEncrypted);
    String sessionIdBase = Joiner.on('|')
        .join(base64(username),
              base64(expirationTimestamp),
              signature);

    return sessionIdBase;
  }

  @Override
  public boolean isValidToken(final String username, final String sessionToken) {
    return false;
  }

  private static String base64(String text) {
    return  Base64.encodeBase64String(text.getBytes(Charsets.UTF_8));
  }

  private static String base64(byte[] bytes) {
    return Base64.encodeBase64String(bytes);
  }
}
