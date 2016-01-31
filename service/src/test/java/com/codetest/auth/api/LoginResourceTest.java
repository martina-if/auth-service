package com.codetest.auth.api;

import com.codetest.auth.storage.SessionStore;
import com.codetest.auth.storage.UserData;
import com.codetest.auth.storage.UserDataBuilder;
import com.codetest.auth.storage.UserDataStore;
import com.codetest.auth.util.ObjectMappers;
import com.codetest.auth.util.Passwords;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.test.ServiceHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

import okio.ByteString;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class LoginResourceTest {

  private static final UserData USER = new UserDataBuilder()
      .fullname("Alice")
      .username("username")
      .password("password")
      .salt("salt")
      .build();
  private SessionStore sessionStore = mock(SessionStore.class);
  private UserDataStore userDataStore = mock(UserDataStore.class);
  private Passwords passwords = mock(Passwords.class);

  @Rule
  public ServiceHelper serviceHelper = ServiceHelper.create(this::init, "auth");

  private void init(Environment environment) {
    LoginResource loginResource = new LoginResource(sessionStore, userDataStore, passwords);
    environment.routingEngine().registerAutoRoutes(loginResource);
  }

  @Before
  public void setup() {
    when(sessionStore.createSessionToken(anyString()))
        .thenReturn("session-token");
    when(passwords.checkPassword(anyString(), anyString(), anyString()))
        .thenReturn(true);
    when(userDataStore.fetchUserData(anyString()))
        .thenReturn(Optional.of(USER));

  }
  @Test
  public void testPasswordCorrect() throws Exception {
    when(userDataStore.fetchUserData(anyString()))
        .thenReturn(Optional.of(USER));
    Response<ByteString> response = sendRequest("username", "password");
    assertEquals(200, response.status().code());
  }

  @Test
  public void testPasswordIncorrect() throws Exception {
    reset(passwords);
    when(passwords.checkPassword(anyString(), anyString(), anyString()))
        .thenReturn(false);
    Response<ByteString> response = sendRequest("username", "password");
    assertEquals(401, response.status().code());
  }

  @Test
  public void testNonExistingUser() throws Exception {
    reset(userDataStore);
    when(userDataStore.fetchUserData(anyString()))
        .thenReturn(Optional.<UserData>empty());
    Response < ByteString > response = sendRequest("username", "pass");
    assertEquals(401, response.status().code());
  }

  private Response<ByteString> sendRequest(String username, String password) throws Exception {
    LoginResource.LoginRequest payload = new LoginRequestBuilder()
        .username(username)
        .password(password)
        .build();
    Request request = Request.forUri("http://auth/v0/login", "POST")
        .withPayload(ByteString.of(ObjectMappers.JSON.writeValueAsBytes(payload)));
    return serviceHelper.serviceClient().send(request)
        .toCompletableFuture()
        .get();
  }

  @Test
  public void testNoPayload() throws Exception {
    Request request = Request.forUri("http://auth/v0/login", "POST");

    Response<ByteString> response = serviceHelper.serviceClient().send(request)
        .toCompletableFuture()
        .get();

    assertEquals(400, response.status().code());
  }

  @Test
  public void testInvalidPayload() throws Exception {
    Request request = Request.forUri("http://auth/v0/login", "POST")
        .withPayload(ByteString.encodeUtf8("{ invalid json"));

    Response<ByteString> response = serviceHelper.serviceClient().send(request)
        .toCompletableFuture()
        .get();

    assertEquals(400, response.status().code());
  }

}
