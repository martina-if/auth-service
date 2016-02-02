package com.codetest.auth.storage;

import com.google.common.collect.Maps;

import com.codetest.auth.EndpointException;

import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InMemUserDataStoreTest {

  private Map<String, UserData> users = Maps.newConcurrentMap();
  private static final String CURRENT_TIME = "2016-01-30T16:18:00Z";
  private final Instant first = Instant.parse(CURRENT_TIME);
  private final Instant second = first.plusSeconds(60);

  private Clock clock = mock(Clock.class);
  private InMemUserDataStore inMemUserDataStore = new InMemUserDataStore(users, clock);

  @Before
  public void setup() {
    when(clock.instant()).thenReturn(first, second);
  }

  @Test
  public void testCreate() throws ExecutionException, InterruptedException {
    UserData userData = inMemUserDataStore.createUserData("username", "passwd", "fullname").get();
    assertEquals("username", userData.username());
    assertEquals("passwd", userData.password()); // FIXME
    assertEquals("salt", userData.salt()); // FIXME
    assertEquals("fullname", userData.fullname());
    assertEquals(1, userData.loginTimestamps().size());
    assertEquals(CURRENT_TIME, userData.loginTimestamps().get(0));
  }

  @Test
  public void testMarkAccessUserDoesntExist() throws ExecutionException, InterruptedException {
    try {
      inMemUserDataStore.markUserAccess("nonexistingusername").get();
      fail("Call to markUserAccess for a non existing user should throw an exception");
    } catch (EndpointException e) {
      // expected
    }
  }

  @Test
  public void testMarkUserAccess() throws ExecutionException, InterruptedException {
    inMemUserDataStore.createUserData("username", "passwd", "fullname").get();
    inMemUserDataStore.markUserAccess("username").get();
    Optional<UserData> userdata = inMemUserDataStore.fetchUserData("username").get();
    assertTrue(userdata.isPresent());
    assertEquals(2, userdata.get().loginTimestamps().size());
    assertEquals(CURRENT_TIME, userdata.get().loginTimestamps().get(0));
    assertEquals(second.toString(), userdata.get().loginTimestamps().get(1));
  }
}
