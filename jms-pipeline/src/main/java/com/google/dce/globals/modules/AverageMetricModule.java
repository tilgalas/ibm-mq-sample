package com.google.dce.globals.modules;

import com.google.dce.metrics.AverageMetric;
import com.google.dce.options.BasicOptions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import java.time.Clock;
import java.time.Duration;

@SuppressWarnings("unused")
public class AverageMetricModule extends AbstractModule {

  public static final class Key {
    private Key() {}

    public static final com.google.inject.Key<AverageMetric<Long>> INSTANCE =
        com.google.inject.Key.get(new TypeLiteral<>() {});
  }

  @Provides
  @Singleton
  public AverageMetric<Long> averageMetric(
      @Named("averageMetricWindowSize") Duration windowSize, Clock clock) {
    return new AverageMetric<>(windowSize, clock);
  }

  @Provides
  @Named("averageMetricWindowSize")
  public Duration windowSize(BasicOptions pipelineOptions) {
    return Duration.ofSeconds(pipelineOptions.getAutoScalerMetricWindowDurationSecs());
  }
}
