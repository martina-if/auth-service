package com.codetest.auth.storage;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InMemSessionStoreTest {

  private InMemSessionStore inMemSessionStore = new InMemSessionStore();
  private String token;

  @Before
  public void setup() {
    token = inMemSessionStore.createSessionToken("username");
  }

  @Test
  public void testValidSession() {
    assertTrue(inMemSessionStore.isValidToken("username", token));
  }

  @Test
  public void testInvalidUsername() {
    assertFalse(inMemSessionStore.isValidToken("username1", token));
  }

  @Test
  public void testIncorrectToken() {
    assertFalse(inMemSessionStore.isValidToken("username", "incorrectToken"));
  }

}
