package com.codetest.auth.client;

import com.codetest.auth.api.LoginRequestBuilder;
import com.codetest.auth.api.LoginResource;
import com.codetest.auth.api.RegisterRequestBuilder;
import com.codetest.auth.api.RegisterResource;
import com.codetest.auth.util.ObjectMappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import java.io.IOException;

import okio.ByteString;

/**
 * This class handles the http connection to the auth service.
 * Provides an API for the 3 endpoints: register, login and activity.
 * It should be adapted to take a {@link com.spotify.apollo.Client} so it
 * can be used for the system tests.
 */
public class AuthClient {

  private static final String BASE_URI = "http://%s/";
  private static final String REGISTER_ENDPOINT = "v0/register";
  private static final String LOGIN_ENDPOINT = "v0/login";
  private static final String ACTIVITY_ENDPOINT = "v0/activity/";

  private final HttpClient client;
  private String baseUri;

  public AuthClient(HttpClient client) {
    this(client, "localhost:8080");
  }

  public AuthClient(HttpClient client, String endpoint) {
    this.client = client;
    this.baseUri = String.format(BASE_URI, endpoint);
  }

  public Response<ByteString> sendLoginRequest(String username, String password)
      throws IOException {
    return client.send(createLoginRequest(username, password));
  }

  public Request createLoginRequest(String username, String password) throws JsonProcessingException {
    LoginResource.LoginRequest payload = new LoginRequestBuilder()
        .username(username)
        .password(password)
        .build();
    return Request.forUri(baseUri + LOGIN_ENDPOINT, "POST")
        .withPayload(ByteString.of(ObjectMappers.JSON.writeValueAsBytes(payload)));
  }

  public Response<ByteString> sendRegisterRequest(String username, String password, String fullname)
      throws IOException {

    return client.send(createRegisterRequest(username, password, fullname));
  }

  public Request createRegisterRequest(String username, String password, String fullname) throws JsonProcessingException {

    RegisterResource.RegisterRequest payload = new RegisterRequestBuilder()
        .username(username)
        .password(password)
        .fullname(fullname)
        .build();
    return Request.forUri(baseUri + REGISTER_ENDPOINT, "POST")
        .withPayload(ByteString.of(ObjectMappers.JSON.writeValueAsBytes(payload)));
  }

  public Response<ByteString> sendActivityRequest(String username, String authUsername, String sessionToken)
      throws IOException {

    return client.send(createActivityRequest(username, authUsername, sessionToken));
  }

  public Request createActivityRequest(String username, String authUsername, String sessionToken) {
    return Request.forUri(baseUri + ACTIVITY_ENDPOINT + username, "GET")
        .withHeader("Session-Token", sessionToken)
        .withHeader("Username", authUsername);
  }
}
