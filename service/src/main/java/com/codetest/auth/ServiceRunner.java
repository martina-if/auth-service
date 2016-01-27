package com.codetest.auth;

import com.spotify.apollo.Environment;
import com.spotify.apollo.httpservice.HttpService;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.route.Route;

public class ServiceRunner {

  public static void main(String... args) throws LoadingException {
    HttpService.boot(ServiceRunner::init, "auth", args);
  }

  static void init(Environment environment) {
    environment.routingEngine()
        .registerAutoRoute(Route.sync("GET", "/ping", requestContext -> "pong"));
  }
}
