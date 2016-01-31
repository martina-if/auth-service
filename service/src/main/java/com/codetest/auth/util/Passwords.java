package com.codetest.auth.util;

public class Passwords {

  public boolean checkPassword(String passwordText, String salt, String encryptedPassword) {
    // TODO encrypt password hash
    return passwordText.equals(encryptedPassword);
  }

}
