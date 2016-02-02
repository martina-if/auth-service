package com.codetest.auth.util;

import com.codetest.auth.EndpointException;
import com.spotify.apollo.Status;

import org.slf4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static org.slf4j.LoggerFactory.getLogger;

public class CryptoUtil {

  private static final Logger LOG = getLogger(CryptoUtil.class);

  public static byte[] encrypt(byte[] text, SecretKeySpec secretKeySpec) {
    try {
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
      return cipher.doFinal(text);
    } catch (Exception e){
      LOG.warn("Unable to encrypt text", e);
      throw new EndpointException(Status.INTERNAL_SERVER_ERROR, "server error");
    }
  }
}
