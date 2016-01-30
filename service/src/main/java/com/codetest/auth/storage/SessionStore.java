package com.codetest.auth.storage;

public interface SessionStore {

  String createSessionToken(String username);

  boolean isValidToken(String username, String sessionToken);

}
