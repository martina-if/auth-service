package com.codetest.auth.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

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
  public UserData createUserData(final String username, final String password, final String fullname) {
    UserData userData = new UserDataBuilder()
        .username(username)
        .password(password) // FIXME
        .salt("salt") // FIXME
        .fullname(fullname)
        .accessTimes(Collections.singletonList(TimeUtil.timestamp(clock)))
        .build();
    users.put(username, userData);
    return userData;
  }

  @Override
  public Optional<UserData> fetchUserData(final String username) {
    return Optional.ofNullable(users.get(username));
  }

  @Override
  public void markUserAccess(final String username) {
    UserData userData = users.get(username);
    if (userData == null) {
      throw new EndpointException(Status.BAD_REQUEST, "Invalid request");
    }
    List<String> timestamps = new ArrayList<>(userData.accessTimes());
    timestamps.add(TimeUtil.timestamp(clock));
    UserData newUserData = UserDataBuilder.from(userData).accessTimes(timestamps).build();
    users.put(username, newUserData);
  }
}
