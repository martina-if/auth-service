package com.codetest.auth.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import java.nio.charset.Charset;

import javax.crypto.spec.SecretKeySpec;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Paswords are stored as AES(SHA256( salt + " " + passwordtext))
 */
public class Passwords {

  private static final Logger LOG = getLogger(Passwords.class);
  private final SecretKeySpec serverKey;

  public Passwords(String serverKeyText) {
    this.serverKey = new SecretKeySpec(serverKeyText.getBytes(), "AES");
  }

  public boolean checkPassword(String passwordText, String salt, String encryptedPassword) {
    String givenPassword = encryptPassword(passwordText, salt);
    return CryptoUtil.secureEquals(givenPassword, encryptedPassword);
  }

  public String encryptPassword(String passwordText, String salt) {
    final byte[] loginHash = DigestUtils.sha256(salt + " " + passwordText);
    return new String(CryptoUtil.encrypt(loginHash, serverKey),
                      Charset.forName("UTF-8"));
  }

}
