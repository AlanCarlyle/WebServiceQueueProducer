package org.ajcarlyle.utilities;

import org.slf4j.helpers.MessageFormatter;

public final class Strings {
// The declared package "org.ajcarlyle.utilities" does not match the expected package "org.ajcarlyle.utilites"
 public final static String format(final String pattern, Object... args) {
    return MessageFormatter.arrayFormat(pattern, args).getMessage();
  }

  public final static String format(final String pattern, Object arg) {
    return MessageFormatter.format(pattern, arg).getMessage();
  }

}