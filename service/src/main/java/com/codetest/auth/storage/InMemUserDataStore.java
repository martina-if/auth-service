package com.codetest.auth.storage;

import com.google.common.collect.Maps;

import com.codetest.auth.EndpointException;
import com.codetest.auth.util.TimeUtil;
import com.spotify.apollo.Status;

import java.time.Clock;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class InMemUserDataStore implements UserDataStore {

  private final Map<String, UserData> users = Maps.newHashMap();
  private final Clock clock = Clock.systemUTC();

  @Override
  public void createUserData(final String username, final String password, final String fullname) {
    UserData userData = new UserDataBuilder()
        .username(username)
        .password(password) // FIXME
        .fullname(fullname)
        .accessTimes(Collections.singletonList(TimeUtil.timestamp(clock)))
        .build();
    users.put(username, userData);
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
  }
}
