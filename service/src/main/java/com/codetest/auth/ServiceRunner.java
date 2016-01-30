package com.codetest.auth;

import com.codetest.auth.api.LoginResource;
import com.codetest.auth.storage.InMemSessionStore;
import com.codetest.auth.storage.InMemUserDataStore;
import com.codetest.auth.storage.SessionStore;
import com.codetest.auth.storage.UserDataStore;
import com.spotify.apollo.Environment;
import com.spotify.apollo.httpservice.HttpService;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.route.Route;

public class ServiceRunner {

  public static void main(String... args) throws LoadingException {
    HttpService.boot(ServiceRunner::init, "auth", args);
  }

  static void init(Environment environment) {

    SessionStore sessionStore = new InMemSessionStore();
    UserDataStore userDataStore = new InMemUserDataStore();
    LoginResource loginResource = new LoginResource(sessionStore, userDataStore);
    environment.routingEngine()
        .registerAutoRoute(Route.sync("GET", "/ping", requestContext -> "pong"))
        .registerAutoRoutes(loginResource);
  }
}
