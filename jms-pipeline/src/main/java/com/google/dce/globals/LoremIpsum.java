package com.google.dce.globals;

import java.io.BufferedInputStream;
import java.io.IOException;

public final class LoremIpsum {
  private LoremIpsum() {}

  public static final String loremIpsum = loadLoremIpsum();

  private static String loadLoremIpsum() {
    BufferedInputStream bufferedInputStream =
        new BufferedInputStream(
            LoremIpsum.class.getClassLoader().getResourceAsStream("loremipsum.txt"));

    try {
      return new String(bufferedInputStream.readAllBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
