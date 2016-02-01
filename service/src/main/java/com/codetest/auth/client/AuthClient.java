package com.codetest.auth.client;

import com.codetest.auth.api.LoginRequestBuilder;
import com.codetest.auth.api.LoginResource;
import com.codetest.auth.api.RegisterRequestBuilder;
import com.codetest.auth.api.RegisterResource;
import com.codetest.auth.util.ObjectMappers;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import okio.ByteString;

public class AuthClient {

  private static final String BASE_URI = "http://%s/";
  private static final String REGISTER_ENDPOINT = "v0/register";
  private static final String LOGIN_ENDPOINT = "v0/login";
  private static final String ACTIVITY_ENDPOINT = "v0/activity/";

  private final HttpClient client;
  private final String baseUri;

  public AuthClient(HttpClient client) {
    this(client, "localhost:8080");
  }

  public AuthClient(HttpClient client, String endpoint) {
    this.client = client;
    this.baseUri = String.format(BASE_URI, endpoint);
  }

  public Response<ByteString> sendLoginRequest(String username, String password)
      throws ExecutionException, InterruptedException, IOException {

    LoginResource.LoginRequest payload = new LoginRequestBuilder()
        .username(username)
        .password(password)
        .build();
    Request request = Request.forUri(baseUri + LOGIN_ENDPOINT, "POST")
        .withPayload(ByteString.of(ObjectMappers.JSON.writeValueAsBytes(payload)));

    return client.send(request);
  }

  public Response<ByteString> sendRegisterRequest(String username, String password, String fullname)
      throws ExecutionException, InterruptedException, IOException {

    RegisterResource.RegisterRequest payload = new RegisterRequestBuilder()
        .username(username)
        .password(password)
        .fullname(fullname)
        .build();
    Request request = Request.forUri(baseUri + REGISTER_ENDPOINT, "POST")
        .withPayload(ByteString.of(ObjectMappers.JSON.writeValueAsBytes(payload)));

    return client.send(request);
  }


  public Response<ByteString> sendActivityRequest(String username, String authUsername, String sessionToken)
      throws ExecutionException, InterruptedException, IOException {

    Request request = Request.forUri(baseUri + ACTIVITY_ENDPOINT + username, "GET")
        .withHeader("Session-Token", sessionToken)
        .withHeader("Username", authUsername);

    return client.send(request);
  }
}
