package com.google.dce.transforms;

import com.google.dce.options.BasicOptions;
import javax.jms.ConnectionFactory;
import org.apache.beam.sdk.io.jms.JmsIO;
import org.apache.beam.sdk.io.jms.JmsRecord;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.Flatten;
import org.apache.beam.sdk.transforms.Latest;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.transforms.windowing.AfterPane;
import org.apache.beam.sdk.transforms.windowing.GlobalWindows;
import org.apache.beam.sdk.transforms.windowing.Repeatedly;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.PCollectionView;

public class ReadLatestMessageSize extends PTransform<PBegin, PCollectionView<Integer>> {
  private final BasicOptions pipelineOptions;
  private final SerializableFunction<Void, ? extends ConnectionFactory> mqConnectionFactory;

  public ReadLatestMessageSize(
      BasicOptions pipelineOptions,
      SerializableFunction<Void, ? extends ConnectionFactory> mqConnectionFactory) {
    this.pipelineOptions = pipelineOptions;
    this.mqConnectionFactory = mqConnectionFactory;
  }

  @Override
  public PCollectionView<Integer> expand(PBegin input) {
    PCollection<JmsRecord> messageSizeJmsRecords =
        input.apply(
            "Subscribe to message size topic",
            JmsIO.read()
                .withConnectionFactoryProviderFn(mqConnectionFactory)
                .withUsername(pipelineOptions.getMqUsername())
                .withPassword(pipelineOptions.getMqPassword())
                .withTopic(pipelineOptions.getMessageSizeTopicName()));

    PCollection<Integer> messageSizes =
        messageSizeJmsRecords.apply("Map to int", ParDo.of(new JmsRecordToInteger()));
    PCollection<Integer> fallback = input.apply(Create.of(0));
    PCollection<Integer> messageSizesWithFallback =
        PCollectionList.of(messageSizes).and(fallback).apply(Flatten.pCollections());

    return messageSizesWithFallback
        .apply(
            "Setup triggering",
            Window.<Integer>into(new GlobalWindows())
                .discardingFiredPanes()
                .triggering(Repeatedly.forever(AfterPane.elementCountAtLeast(1))))
        .apply("Use latest", Latest.globally())
        .apply(View.asSingleton());
  }
}
