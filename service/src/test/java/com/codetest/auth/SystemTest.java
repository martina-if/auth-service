package com.codetest.auth;

import com.codetest.auth.api.LoginResource;
import com.codetest.auth.client.AuthClient;
import com.codetest.auth.util.ObjectMappers;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.test.ServiceHelper;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import okio.ByteString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This basic acceptance test expects a cassandra instance
 * running on localhost. This should start a cassandra container
 * for this test (can be done with helios-testing for example).
 */
public class SystemTest {

  private static final String USER = "user1";
  private static final String PASSWORD = "12345";

  @Rule
  public ServiceHelper serviceHelper = ServiceHelper.create(ServiceRunner::init, "auth");

  // This should take the service client from the service Helper
  private AuthClient authClient = new AuthClient(null, "localhost:8080");

  @Test
  public void basicAcceptanceTest() throws Exception {
    // Register a user
    Request registerRequest = authClient.createRegisterRequest(USER, PASSWORD, "John Doe");
    Response<ByteString> registerResponse = sendRequest(registerRequest);
    // After the first time this will return 400 if not run against a clean cassandra instance
//    assertEquals(200, registerResponse.status().code());

    // Try logging
    Request loginRequest = authClient.createLoginRequest(USER, PASSWORD);
    Response<ByteString> loginResponse = sendRequest(loginRequest);
    assertEquals(200, loginResponse.status().code());
    String sessionToken = parseSessionToken(loginResponse);


    // Get activity
    Request activityRequest = authClient.createActivityRequest(USER, USER, sessionToken);
    Response<ByteString> activityResponse = sendRequest(activityRequest);
    assertEquals(200, activityResponse.status().code());

  }

  private static String parseSessionToken(Response<ByteString> loginResponse) throws IOException {
    assertTrue(loginResponse.payload().isPresent());

    LoginResource.LoginResponse login = ObjectMappers.JSON.readValue(loginResponse.payload().get().toByteArray(),
                                                                              LoginResource.LoginResponse.class);
    return login.session();
  }

  private Response<ByteString> sendRequest(Request request) throws Exception {
    return serviceHelper.serviceClient().send(request)
        .toCompletableFuture()
        .get();
  }

}
