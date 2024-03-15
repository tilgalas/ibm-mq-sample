package com.google.dce.globals;

import com.google.auto.service.AutoService;
import com.google.dce.globals.modules.AverageMetricModule;
import com.google.dce.globals.modules.ClockModule;
import com.google.dce.globals.modules.PipelineOptionsModule;
import com.google.dce.globals.modules.QueueServiceModule;
import com.google.dce.options.BasicOptions;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.apache.arrow.util.Preconditions;
import org.apache.beam.sdk.harness.JvmInitializer;
import org.apache.beam.sdk.options.PipelineOptions;
import org.checkerframework.checker.nullness.qual.NonNull;

@AutoService(JvmInitializer.class)
public class GuiceInitializer implements JvmInitializer {

  public static Injector injector = null;

  public static <T> T getInstance(Key<T> key) {
    return Preconditions.checkNotNull(injector).getInstance(key);
  }

  public static <T> T getInstance(Class<T> type) {
    return Preconditions.checkNotNull(injector).getInstance(type);
  }

  @Override
  public void beforeProcessing(@NonNull PipelineOptions pipelineOptions) {
    BasicOptions basicOptions = pipelineOptions.as(BasicOptions.class);
    QueueServiceModule queueServiceModule = new QueueServiceModule();
    AverageMetricModule averageMetricModule = new AverageMetricModule();
    PipelineOptionsModule pipelineOptionsModule = new PipelineOptionsModule(basicOptions);
    ClockModule clockModule = new ClockModule();
    injector =
        Guice.createInjector(
            queueServiceModule, averageMetricModule, pipelineOptionsModule, clockModule);
  }
}
