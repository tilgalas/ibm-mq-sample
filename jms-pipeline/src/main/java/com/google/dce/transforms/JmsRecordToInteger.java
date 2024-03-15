package com.google.dce.transforms;

import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsRecordToInteger extends DoFn<JmsRecord, Integer> {
  private static final Logger LOG = LoggerFactory.getLogger(JmsRecordToInteger.class);

  @SuppressWarnings("unused")
  @ProcessElement
  public void processElement(@Element JmsRecord jmsRecord, OutputReceiver<Integer> outputReceiver) {
    try {
      int newMessageSize = Integer.parseInt(jmsRecord.getPayload());
      LOG.info("new message size received {}", newMessageSize);
      outputReceiver.output(newMessageSize);
    } catch (NumberFormatException e) {
      LOG.error("bad message size received", e);
    }
  }
}
