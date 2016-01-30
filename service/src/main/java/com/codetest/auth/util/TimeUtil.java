package com.codetest.auth.util;

import java.time.Clock;
import java.time.LocalDateTime;

public class TimeUtil {

  private TimeUtil() { }

  public static LocalDateTime now(Clock clock) {
    return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
  }

  public static String timestamp(Clock clock) {
    return LocalDateTime.ofInstant(clock.instant(), clock.getZone()).toString();
  }
}
