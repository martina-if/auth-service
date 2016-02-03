package com.codetest.auth;

import com.spotify.apollo.StatusType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An exception that contains the status code that should be
 * sent for the current request
 */
public class EndpointException extends RuntimeException {

  private final StatusType statusType;

  public EndpointException(final StatusType statusCode, final String message, final Throwable cause) {
    super(message, cause);
    this.statusType = checkNotNull(statusCode);
  }

  public EndpointException(final StatusType statusCode, final String message) {
    super(message);
    this.statusType = checkNotNull(statusCode);
  }

  public StatusType getStatusCode() {
    return statusType;
  }
}
