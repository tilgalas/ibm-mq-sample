package com.google.dce.globals.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.time.Clock;

public class ClockModule extends AbstractModule {
  @Provides
  @Singleton
  public Clock systemClock() {
    return Clock.systemDefaultZone();
  }
}
