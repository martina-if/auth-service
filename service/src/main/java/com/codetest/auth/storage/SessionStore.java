package com.codetest.auth.storage;

/**
 * A class that can create and validate session tokens
 */
public interface SessionStore {

  String createSessionToken(String username);

  boolean isValidToken(String username, String sessionToken);

}
