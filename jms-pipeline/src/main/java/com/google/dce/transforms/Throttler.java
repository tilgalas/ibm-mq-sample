package com.google.dce.transforms;

import com.google.dce.globals.ConsumerInitializer;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.metrics.Distribution;
import org.apache.beam.sdk.metrics.Metrics;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Throttler extends DoFn<JmsRecord, Void> {
  private static final Logger LOG = LoggerFactory.getLogger(Throttler.class);

  public Throttler() {}

  @ProcessElement
  public void processElement(@Element JmsRecord jmsRecord) throws InterruptedException {
    LOG.info("read JmsRecord, payload: {}", jmsRecord.getPayload());
    long start = System.nanoTime();
    ConsumerInitializer.consumerQueue.put(jmsRecord);
    long t = System.nanoTime() - start;
    LOG.info(String.format("processing complete after %.2f ms", t / 1e6));
    Distribution processingTimes = Metrics.distribution(Throttler.class, "ProcessingTimes");
    processingTimes.update(Math.round(t / 1e6));
  }
}
