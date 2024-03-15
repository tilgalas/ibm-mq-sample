package com.google.dce.globals;

import com.google.auto.service.AutoService;
import com.google.dce.options.BasicOptions;
import com.google.dce.options.PipelineVariant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import org.apache.beam.sdk.harness.JvmInitializer;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.options.PipelineOptions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(JvmInitializer.class)
public class ConsumerInitializer implements JvmInitializer {

  public static final BlockingQueue<JmsRecord> consumerQueue = new SynchronousQueue<>(true);
  public static final Logger LOG = LoggerFactory.getLogger(ConsumerInitializer.class);

  @Override
  public void beforeProcessing(@NonNull PipelineOptions options) {
    BasicOptions basicOptions = options.as(BasicOptions.class);
    if (basicOptions.getPipelineVariant() == PipelineVariant.CONSUMER) {
      ScheduledExecutorService scheduledExecutorService =
          Executors.newSingleThreadScheduledExecutor();
      scheduledExecutorService.scheduleAtFixedRate(
          consumerQueue::poll,
          0L,
          Math.round(1e9 / basicOptions.getConsumeElementRate()),
          TimeUnit.NANOSECONDS);
    } else {
      LOG.info("nothing to do in {} pipeline", basicOptions.getPipelineVariant());
    }
  }
}
