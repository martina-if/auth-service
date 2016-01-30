package com.codetest.auth.storage;

import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Todo create a token including username, expiration time and signature. Then no need for storage
 */
public class InMemSessionStore implements SessionStore {

  private final long TOKEN_TTL_SECONDS = 3600;
  private final Cache<String, String> userSessionTokens = CacheBuilder.newBuilder()
      .expireAfterWrite(TOKEN_TTL_SECONDS, TimeUnit.SECONDS)
      .build();

  @Override
  public String createSessionToken(final String username) {
    String newSessionToken = UUID.randomUUID().toString();
    userSessionTokens.put(username, newSessionToken);
    return newSessionToken;
  }

  @Override
  public boolean isValidToken(final String username, final String sessionToken) {
    if (Strings.isNullOrEmpty(username)) {
      return false;
    }
    return sessionToken.equals(userSessionTokens.getIfPresent(username));
  }

}
