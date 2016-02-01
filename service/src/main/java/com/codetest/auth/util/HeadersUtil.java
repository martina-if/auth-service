package com.codetest.auth.util;

import com.spotify.apollo.Request;

import java.util.Optional;

public class HeadersUtil {

  private HeadersUtil() { }

  /**
   * Get the session token from the Session-token header in a request
   * TODO: Use Authorizaton header with format: credentials = auth-scheme #auth-param
   */
  public static Optional<String> getSessionToken(Request request) {
    return request.header("Session-Token");
  }

  /**
   * Get the authenticated user from the correct header in a request
   * TODO: Use Authorizaton header with format: credentials = auth-scheme #auth-param
   */
  public static Optional<String> getUsername(Request request) {
    return request.header("Username");
  }
}
