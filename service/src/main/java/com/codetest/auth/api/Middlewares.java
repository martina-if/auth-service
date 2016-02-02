package com.codetest.auth.api;

import com.codetest.auth.EndpointException;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.route.AsyncHandler;

import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

import static org.slf4j.LoggerFactory.getLogger;

public class Middlewares {

  private static final Logger LOG = getLogger(Middlewares.class);

  /**
   * Handle errors in requests and log response status code
   */
  public static <T> com.spotify.apollo.route.Middleware<AsyncHandler<Response<T>>, AsyncHandler<Response<T>>>
  checkExceptions() {
    return handler ->
        requestContext -> {
          try {
            return handler.invoke(requestContext)
                .thenApply(response -> {
                  LOG.info("Returned status {} to request {}",
                           response.status(),
                           requestContext.request().uri());
                  return response;
                })
                .exceptionally(t -> {
                  if (t.getCause() instanceof EndpointException) {
                    EndpointException e = (EndpointException) t.getCause();
                    LOG.info("Returned status {} to request {}", e.getStatusCode(), requestContext.request().uri());
                    return Response.<T>forStatus(e.getStatusCode());
                  } else {
                    LOG.info("Returned status {} to request {}",
                             Status.INTERNAL_SERVER_ERROR, requestContext.request().uri(),
                             t);
                    return Response.<T>forStatus(Status.INTERNAL_SERVER_ERROR);
                  }
                });
          } catch (EndpointException e) {
            LOG.info("Returned status {} to request {}", e.getStatusCode(), requestContext.request().uri(), e);
            return CompletableFuture.completedFuture(Response.<T>forStatus(e.getStatusCode()))
                .toCompletableFuture();
          }
        };
  }

}
