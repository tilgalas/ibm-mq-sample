package com.google.dce.pipelines;

import com.google.dce.autoscaler.RestAutoScaler;
import com.google.dce.globals.LoremIpsum;
import com.google.dce.options.BasicOptions;
import com.google.dce.transforms.MessageSizeObserver;
import com.google.dce.transforms.ReadLatestMessageSize;
import com.google.dce.transforms.Throttler;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.GenerateSequence;
import org.apache.beam.sdk.io.jms.JmsIO;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PipelineBuilders {

  private PipelineBuilders() {}

  public static void consumerPipeline(
      Pipeline pipeline,
      BasicOptions pipelineOptions,
      SerializableFunction<Void, ? extends ConnectionFactory> mqConnectionFactory) {
    PCollection<JmsRecord> jmsRecords =
        pipeline.apply(
            "Read from IBM MQ",
            JmsIO.read()
                .withConnectionFactoryProviderFn(mqConnectionFactory)
                .withUsername(pipelineOptions.getMqUsername())
                .withPassword(pipelineOptions.getMqPassword())
                .withQueue(pipelineOptions.getQueueName())
                .withAutoScaler(
                    new RestAutoScaler(
                        pipelineOptions.getQueueManager(), pipelineOptions.getQueueName())));
    jmsRecords.apply("Observe message size", ParDo.of(new MessageSizeObserver()));
    jmsRecords.apply("Log payload", ParDo.of(new Throttler()));
  }

  public static void producerPipeline(
      Pipeline pipeline,
      BasicOptions pipelineOptions,
      SerializableFunction<Void, ? extends ConnectionFactory> mqConnectionFactory) {

    PCollection<Long> longSequence =
        pipeline.apply(
            "Artificial source",
            GenerateSequence.from(0L)
                .withRate(pipelineOptions.getProduceElementRate(), Duration.standardSeconds(1)));

    PCollectionView<Integer> latestMessageSize =
        pipeline.apply(
            "Read latest requested message size from topic",
            new ReadLatestMessageSize(pipelineOptions, mqConnectionFactory));

    PCollection<String> textSequence =
        longSequence.apply(
            "Map to text",
            ParDo.of(new GenerateTextMessage(latestMessageSize)).withSideInputs(latestMessageSize));

    textSequence.apply(
        "Write to IBM MQ",
        JmsIO.<String>write()
            .withConnectionFactoryProviderFn(mqConnectionFactory)
            .withUsername(pipelineOptions.getMqUsername())
            .withPassword(pipelineOptions.getMqPassword())
            .withQueue(pipelineOptions.getQueueName())
            .withValueMapper(
                (text, session) -> {
                  try {
                    return session.createTextMessage(text);
                  } catch (JMSException e) {
                    throw new RuntimeException(e);
                  }
                }));
  }

  private static class GenerateTextMessage extends DoFn<Long, String> {
    private final PCollectionView<Integer> latestMessageSize;
    private static final Logger LOG = LoggerFactory.getLogger(GenerateTextMessage.class);

    public GenerateTextMessage(PCollectionView<Integer> latestMessageSize) {
      this.latestMessageSize = latestMessageSize;
    }

    @SuppressWarnings("unused")
    @ProcessElement
    public void processElement(
        OutputReceiver<String> outputReceiver, ProcessContext processContext) {
      Random rng = ThreadLocalRandom.current();
      Integer messageSize = processContext.sideInput(latestMessageSize);
      if (messageSize != null && messageSize > 0) {
        int endIndex = (int) Math.round(rng.nextGaussian() * 100 + messageSize);
        endIndex = Math.min(LoremIpsum.loremIpsum.length(), endIndex);
        endIndex = Math.max(0, endIndex);

        outputReceiver.output(LoremIpsum.loremIpsum.substring(0, endIndex));
      } else {
        LOG.info("not emitting message, since messageSize is {}", messageSize);
      }
    }
  }
}
