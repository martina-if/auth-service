package com.codetest.auth.storage;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Optional;

/**
 * A class that can create, update and get users' data
 */
public interface UserDataStore {

  ListenableFuture<UserData> createUserData(String username, String password, String fullname);

  ListenableFuture<Optional<UserData>> fetchUserData(String username);

  ListenableFuture<Void> markUserAccess(String username);
}
