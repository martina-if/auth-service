package com.codetest.auth.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import com.codetest.auth.EndpointException;
import com.codetest.auth.util.TimeUtil;
import com.spotify.apollo.Status;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemUserDataStore implements UserDataStore {

  private final Map<String, UserData> users;
  private final Clock clock;

  public InMemUserDataStore() {
    users = Maps.newConcurrentMap();
    clock = Clock.systemUTC();
  }

  @VisibleForTesting
  InMemUserDataStore(Map<String, UserData> users, Clock clock) {
    this.users = users;
    this.clock = clock;
  }

  @Override
  public ListenableFuture<UserData> createUserData(final String username, final String password, final String fullname) {
    UserData userData = new UserDataBuilder()
        .username(username)
        .password(password) // FIXME
        .salt("salt") // FIXME
        .fullname(fullname)
        .loginTimestamps(Collections.singletonList(TimeUtil.timestamp(clock)))
        .build();
    users.put(username, userData);
    return Futures.immediateFuture(userData);
  }

  @Override
  public ListenableFuture<Optional<UserData>> fetchUserData(final String username) {
    return Futures.immediateFuture(Optional.ofNullable(users.get(username)));
  }

  @Override
  public ListenableFuture<Void> markUserAccess(final String username) {
    UserData userData = users.get(username);
    if (userData == null) {
      throw new EndpointException(Status.BAD_REQUEST, "Invalid request");
    }
    List<String> timestamps = new ArrayList<>(userData.loginTimestamps());
    timestamps.add(TimeUtil.timestamp(clock));
    UserData newUserData = UserDataBuilder.from(userData).loginTimestamps(timestamps).build();
    users.put(username, newUserData);
    return Futures.immediateFuture(null);
  }
}
