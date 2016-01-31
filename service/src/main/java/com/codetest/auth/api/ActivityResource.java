package com.codetest.auth.api;

import com.codetest.auth.storage.UserData;
import com.codetest.auth.storage.UserDataStore;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.norberg.automatter.AutoMatter;

/**
 * Endpoints for fetching last 5 access timestamps for a user
 */
public class ActivityResource implements RouteProvider {

  private static final int NUM_LOGIN_ATTEMPTS = 5;
  private final UserDataStore userDataStore;

  public ActivityResource(final UserDataStore userDataStore) {
    this.userDataStore = userDataStore;
  }

  @Override
  public Stream<? extends Route<? extends AsyncHandler<?>>> routes() {
    return Stream.of(
        Route.sync("GET", "/v0/activity/logins/<username>", this::loginActivity)
    );
  }

  private Response<LoginActivityResponse> loginActivity(RequestContext context) {

    // Parse request
    String activityUsername = context.pathArgs().get("username");
    if (activityUsername == null) {
      return Response.forStatus(Status.BAD_REQUEST.withReasonPhrase("Missing username"));
    }

    // Check authorization
    // TODO Check authorization token from header

    // Recover user data for username
    Optional<UserData> userData = userDataStore.fetchUserData(activityUsername);
    if (!userData.isPresent()) {
      return Response.forStatus(Status.BAD_REQUEST); // Leaking usernames..
    }

    List<String> allLogins = userData.get().accessTimes();
    
    // Get last 5 elements of the list since each new attempt is appended to the end
    List<String> lastLogins = allLogins.subList(Math.max(0, allLogins.size() - NUM_LOGIN_ATTEMPTS), allLogins.size());

    return Response.forPayload(new LoginActivityResponseBuilder()
                                   .timestamps(lastLogins)
                                   .build());
  }

  @AutoMatter
  interface LoginActivityResponse {
    List<String> timestamps();
  }
}
