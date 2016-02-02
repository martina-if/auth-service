package com.codetest.auth.util;

import com.codetest.auth.EndpointException;
import com.spotify.apollo.Status;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import java.nio.charset.Charset;
import java.security.MessageDigest;

import javax.crypto.Cipher;
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
    return isPasswordHashEqual(givenPassword, encryptedPassword);
  }

  public String encryptPassword(String passwordText, String salt) {
    final byte[] loginHash = DigestUtils.sha256(salt + " " + passwordText);
    return new String(encrypt(loginHash), Charset.forName("UTF-8") );
  }

  private static boolean isPasswordHashEqual(final String passwordToCheck, final String hashedPassword) {
    // MessageDigest.isEqual does constant time comparison
    return MessageDigest.isEqual(passwordToCheck.getBytes(), hashedPassword.getBytes());
  }

  private byte[] encrypt(byte[] text) {
    try {
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, serverKey);
      return cipher.doFinal(text);
    } catch (Exception e){
      LOG.warn("Unable to encrypt text", e);
      throw new EndpointException(Status.INTERNAL_SERVER_ERROR, "server error");
    }
  }
}
