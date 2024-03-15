package com.google.dce.clock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;

public class MutableClock extends Clock {
  private ZoneId zoneId;
  private Instant instant;

  public MutableClock(Instant instant, ZoneId zoneId) {
    this.instant = instant;
    this.zoneId = zoneId;
  }

  @Override
  public ZoneId getZone() {
    return zoneId;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return new MutableClock(instant, zone);
  }

  @Override
  public synchronized Instant instant() {
    return instant;
  }

  public synchronized void setInstant(Instant instant) {
    this.instant = instant;
  }

  public synchronized void advance(TemporalAmount duration) {
    setInstant(this.instant.plus(duration));
  }
}
