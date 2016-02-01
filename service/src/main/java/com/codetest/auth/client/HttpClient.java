package com.codetest.auth.client;

import com.google.common.base.Joiner;

import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import okio.ByteString;

/**
 * From apollo-okhttp-client
 */
public class HttpClient {

    private static final MediaType DEFAULT_CONTENT_TYPE = MediaType.parse("application/octet-stream");

    private final OkHttpClient client;

    HttpClient() {
      this.client = new OkHttpClient();
    }

    public Response<ByteString> send(com.spotify.apollo.Request apolloRequest) throws IOException {

      final Optional<RequestBody> requestBody = apolloRequest.payload().map(payload -> {
        final MediaType contentType = apolloRequest.header("Content-Type")
            .map(MediaType::parse)
            .orElse(DEFAULT_CONTENT_TYPE);
        return RequestBody.create(contentType, payload);
      });

      final Request request = new Request.Builder()
          .method(apolloRequest.method(), requestBody.orElse(null))
          .url(apolloRequest.uri())
          .build();

      final CompletableFuture<Response<ByteString>> result =
          new CompletableFuture<>();

      //https://github.com/square/okhttp/wiki/Recipes#per-call-configuration
      OkHttpClient finalClient = client;
      if (apolloRequest.ttl().isPresent()
          && client.getReadTimeout() != apolloRequest.ttl().get().toMillis()) {
        finalClient = client.clone();
        finalClient.setReadTimeout(apolloRequest.ttl().get().toMillis(), TimeUnit.MILLISECONDS);
      }

      com.squareup.okhttp.Response response = finalClient.newCall(request).execute();

      return transformResponse(response);
    }

  static Response<ByteString> transformResponse(com.squareup.okhttp.Response response)
      throws IOException {

    final StatusType status = Status.createForCode(response.code());

    Response<ByteString> apolloResponse =
        Response.forStatus(status);

    for (Map.Entry<String, List<String>> entry : response.headers().toMultimap().entrySet()) {
      apolloResponse = apolloResponse.withHeader(
          entry.getKey(),
          Joiner.on(", ").join(entry.getValue()));
    }

    final byte[] bytes = response.body().bytes();
    if (bytes.length > 0) {
      apolloResponse = apolloResponse.withPayload(ByteString.of(bytes));
    }

    return apolloResponse;
  }
}
