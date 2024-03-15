package com.google.dce;

import com.google.dce.globals.GuiceInitializer;
import com.google.dce.options.BasicOptions;
import com.google.dce.pipelines.PipelineBuilders;
import com.google.dce.utils.JmsUtils;
import com.ibm.mq.jms.MQConnectionFactory;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsPipeline {
  private static final Logger LOG = LoggerFactory.getLogger(JmsPipeline.class);

  public static void main(String[] args) throws Exception {
    PipelineOptionsFactory.register(BasicOptions.class);

    BasicOptions pipelineOptions =
        PipelineOptionsFactory.fromArgs(args).withValidation().as(BasicOptions.class);

    // a pre-flight initialization to detect guice configuration problems
    new GuiceInitializer().beforeProcessing(pipelineOptions);

    SerializableFunction<Void, MQConnectionFactory> mqConnectionFactory =
        JmsUtils.getMqConnectionFactoryProviderFn(pipelineOptions);
    Pipeline p = Pipeline.create(pipelineOptions);

    LOG.info("selected pipeline variant {}", pipelineOptions.getPipelineVariant());
    switch (pipelineOptions.getPipelineVariant()) {
      case CONSUMER:
        PipelineBuilders.consumerPipeline(p, pipelineOptions, mqConnectionFactory);
        break;
      case PRODUCER:
        PipelineBuilders.producerPipeline(p, pipelineOptions, mqConnectionFactory);
        break;
    }

    p.run();
  }
}
