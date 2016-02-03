package com.codetest.auth.api;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.codetest.auth.storage.UserData;
import com.codetest.auth.storage.UserDataStore;
import com.codetest.auth.util.ObjectMappers;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.JsonSerializerMiddlewares;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;

import java.io.IOException;
import java.util.stream.Stream;

import io.norberg.automatter.AutoMatter;

/**
 * Endpoints for registering a new user. It accepts a POST request
 * with a json payload containing username, fullname and password.
 * See {@link RegisterRequest} for the expected json format.
 *
 * It will return OK if the registration was successful with a json
 * payload following this format: {@link RegisterResponse}.
 * It can answer with BAD_REQUEST for missing or malformed data in
 * the payload.
 */
public class RegisterResource implements RouteProvider {

  private final UserDataStore userDataStore;

  public RegisterResource(UserDataStore userDataStore) {
    this.userDataStore = userDataStore;
  }

  @Override
  public Stream<? extends Route<? extends AsyncHandler<?>>> routes() {
    return Stream.of(
        Route.future("POST", "/v0/register", this::registerUser)
            .withMiddleware(Middlewares.checkExceptions())
            .withMiddleware(JsonSerializerMiddlewares.
                jsonSerializeResponse(ObjectMappers.JSON.writer()))
    );
  }

  private ListenableFuture<Response<RegisterResponse>> registerUser(RequestContext context) {
    // Parse request
    if (!context.request().payload().isPresent()) {
      return Futures.immediateFuture(
          Response.forStatus(Status.BAD_REQUEST.withReasonPhrase("Missing payload")));
    }

    RegisterRequest request;
    try {
      request = ObjectMappers.JSON.readValue(context.request().payload().get().toByteArray(),
                                             RegisterRequest.class);
    } catch (IOException e) {
      return Futures.immediateFuture(
          Response.forStatus(Status.BAD_REQUEST.withReasonPhrase("Invalid payload")));
    }

    // TODO Validate input data

    // Store new user data
    ListenableFuture<UserData> userDataFuture = userDataStore.createUserData(request.username(),
                                                                             request.password(),
                                                                             request.fullname());

    return Futures.transform(userDataFuture, (UserData userData) ->
        Response.forPayload(new RegisterResponseBuilder()
                                .fullname(userData.fullname())
                                .username(userData.username())
                                .build()));

  }

  @AutoMatter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public interface RegisterRequest {
    String username();
    String password();
    String fullname();
  }

  @AutoMatter
  public interface RegisterResponse {
    String username();
    String fullname();
  }
}
