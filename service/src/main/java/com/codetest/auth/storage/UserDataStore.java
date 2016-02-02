package com.codetest.auth.storage;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Optional;

public interface UserDataStore {

  ListenableFuture<UserData> createUserData(String username, String password, String fullname);

  ListenableFuture<Optional<UserData>> fetchUserData(String username);

  ListenableFuture<Void> markUserAccess(String username);
}
