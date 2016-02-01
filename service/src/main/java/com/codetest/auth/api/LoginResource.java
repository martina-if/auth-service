package com.codetest.auth.api;

import com.codetest.auth.storage.SessionStore;
import com.codetest.auth.storage.UserData;
import com.codetest.auth.storage.UserDataStore;
import com.codetest.auth.util.ObjectMappers;
import com.codetest.auth.util.Passwords;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.JsonSerializerMiddlewares;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import io.norberg.automatter.AutoMatter;

public class LoginResource implements RouteProvider {

  private final SessionStore sessionStore;
  private final UserDataStore userDataStore;
  private final Passwords passwords;

  public LoginResource(SessionStore sessionStore, UserDataStore userDataStore, Passwords passwords) {
    this.sessionStore = sessionStore;
    this.userDataStore = userDataStore;
    this.passwords = passwords;
  }

  @Override
  public Stream<? extends Route<? extends AsyncHandler<?>>> routes() {
    return Stream.of(
        Route.sync("POST", "/v0/login", this::login)
            .withMiddleware(JsonSerializerMiddlewares.
                jsonSerializeResponse(ObjectMappers.JSON.writer()))
    );
  }

  private Response<LoginResponse> login(RequestContext context) {
    // Parse request TODO: Validate data
    if (!context.request().payload().isPresent()) {
      return Response.forStatus(Status.BAD_REQUEST.withReasonPhrase("Missing payload"));
    }

    LoginRequest loginRequest;
    try {
      loginRequest = ObjectMappers.JSON.readValue(context.request().payload().get().toByteArray(),
                                                  LoginRequest.class);
    } catch (IOException e) {
      return Response.forStatus(Status.BAD_REQUEST.withReasonPhrase("Invalid payload"));
    }

    // Recover User data from store
    String username = loginRequest.username();
    Optional<UserData> userData = userDataStore.fetchUserData(username);
    if (!userData.isPresent()) {
      return Response.forStatus(Status.UNAUTHORIZED.withReasonPhrase("Could not authenticate"));
    }

    boolean validPassword = passwords.checkPassword(loginRequest.password(),
                                                    userData.get().salt(),
                                                    userData.get().password());
    if (!validPassword) {
      return Response.forStatus(Status.UNAUTHORIZED.withReasonPhrase("Could not authenticate"));
    }

    // Write access time
    userDataStore.markUserAccess(username);

    // Generate token
    String sessionToken = sessionStore.createSessionToken(username);
    LoginResponse loginResponse = new LoginResponseBuilder()
        .status("200")
        .username(username)
        .session(sessionToken)
        .build();

    return Response.of(Status.OK, loginResponse);
  }


  @AutoMatter
  @JsonIgnoreProperties(ignoreUnknown = true)
  interface LoginRequest {
    String username();
    String password();
  }

  @AutoMatter
  @JsonInclude(JsonInclude.Include.NON_NULL)
  interface LoginResponse {
    @Nullable
    String status();

    @Nullable
    String message();

    @Nullable
    String session();

    @Nullable
    String username();
  }

}
