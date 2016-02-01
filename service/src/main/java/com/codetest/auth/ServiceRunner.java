package com.codetest.auth;

import com.codetest.auth.api.ActivityResource;
import com.codetest.auth.api.LoginResource;
import com.codetest.auth.api.RegisterResource;
import com.codetest.auth.storage.CassandraDataStore;
import com.codetest.auth.storage.InMemSessionStore;
import com.codetest.auth.storage.SessionStore;
import com.codetest.auth.storage.UserDataStore;
import com.codetest.auth.util.Passwords;
import com.spotify.apollo.Environment;
import com.spotify.apollo.httpservice.HttpService;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.route.Route;
import com.typesafe.config.Config;

public class ServiceRunner {

  public static void main(String... args) throws LoadingException {
    HttpService.boot(ServiceRunner::init, "auth", args);
  }

  static void init(Environment environment) {
    Config config = environment.config();
    String cassandraNode = config.getString("cassandra.node");
    Passwords passwords = new Passwords(config.getString("passwords.key"));

    UserDataStore userDataStore = new CassandraDataStore(cassandraNode, passwords);
    SessionStore sessionStore = new InMemSessionStore();

    LoginResource loginResource = new LoginResource(sessionStore, userDataStore, passwords);
    RegisterResource registerResource = new RegisterResource(userDataStore);
    ActivityResource activityResource = new ActivityResource(userDataStore, sessionStore);

    environment.routingEngine()
        .registerAutoRoute(Route.sync("GET", "/ping", requestContext -> "pong"))
        .registerAutoRoutes(loginResource)
        .registerAutoRoutes(registerResource)
        .registerAutoRoutes(activityResource);
  }
}
