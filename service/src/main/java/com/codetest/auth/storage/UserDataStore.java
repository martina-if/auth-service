package com.codetest.auth.storage;

import java.util.Optional;

public interface UserDataStore {

  UserData createUserData(String username, String password, String fullname);

  Optional<UserData> fetchUserData(String username);

  void markUserAccess(String username);
}
