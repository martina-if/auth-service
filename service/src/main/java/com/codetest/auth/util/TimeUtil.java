package com.codetest.auth.util;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

  private static final ZoneId zoneId = ZoneId.of("UTC");

  private TimeUtil() { }

  public static LocalDateTime now(Clock clock) {
    return LocalDateTime.ofInstant(clock.instant(), clock.getZone());
  }

  public static String timestamp(Clock clock) {
    return DateTimeFormatter.ISO_INSTANT.format(clock.instant().atZone(zoneId));
  }
}
