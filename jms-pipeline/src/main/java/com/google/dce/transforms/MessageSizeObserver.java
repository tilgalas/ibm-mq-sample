package com.google.dce.transforms;

import com.google.dce.coders.CountingCoder;
import com.google.dce.globals.GuiceInitializer;
import com.google.dce.globals.modules.AverageMetricModule;
import com.google.dce.metrics.AverageMetric;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageSizeObserver extends DoFn<JmsRecord, Void> {

  private static final Logger LOG = LoggerFactory.getLogger(MessageSizeObserver.class);
  private CountingCoder<JmsRecord> countingCoder;
  private AverageMetric<Long> averageMetric;

  @SuppressWarnings("unused")
  @Setup
  public void setup() {
    averageMetric = GuiceInitializer.getInstance(AverageMetricModule.Key.INSTANCE);
    countingCoder =
        new CountingCoder<>(SerializableCoder.of(JmsRecord.class), this::acceptMessageSize);
  }

  private void acceptMessageSize(long messageSize) {
    averageMetric.addDataPoint(messageSize);
  }

  @SuppressWarnings("unused")
  @ProcessElement
  public void processElement(@Element JmsRecord jmsRecord) {
    try {
      countingCoder.encode(jmsRecord, OutputStream.nullOutputStream());
    } catch (IOException e) {
      LOG.error("unable to observe message size", e);
    }
  }
}
