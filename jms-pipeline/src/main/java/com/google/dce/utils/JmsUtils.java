package com.google.dce.utils;

import com.google.dce.options.BasicOptions;
import com.google.dce.options.PipelineVariant;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import javax.jms.JMSException;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsUtils {
  private static Logger LOG = LoggerFactory.getLogger(JmsUtils.class);

  @NonNull
  public static MQConnectionFactory getMqConnectionFactory(BasicOptions pipelineOptions)
      throws JMSException {
    return getMqConnectionFactory(
        pipelineOptions.getMqAppName(),
        pipelineOptions.getPipelineVariant(),
        pipelineOptions.getQueueManager(),
        pipelineOptions.getMqHostname(),
        pipelineOptions.getMqPort(),
        pipelineOptions.getChannel(),
        pipelineOptions.getQMgrX509Certificate());
  }

  @NonNull
  public static MQConnectionFactory getMqConnectionFactory(
      String mqAppName,
      PipelineVariant pipelineVariant,
      String queueManager,
      String mqHostName,
      int mqPort,
      String mqChannel,
      String pemCert)
      throws JMSException {
    LOG.info("creating a new MQConnectionFactory");
    MQConnectionFactory mqConnectionFactory = new MQConnectionFactory();
    mqConnectionFactory.setAppName(
        String.format("%s (%s)", mqAppName, pipelineVariant.toString().charAt(0)));
    mqConnectionFactory.setQueueManager(queueManager);
    mqConnectionFactory.setHostName(mqHostName);
    mqConnectionFactory.setPort(mqPort);
    mqConnectionFactory.setChannel(mqChannel);
    mqConnectionFactory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
    mqConnectionFactory.setUserAuthenticationMQCSP(true);
    mqConnectionFactory.setSSLCipherSuite("*TLS12ORHIGHER");
    mqConnectionFactory.setSSLSocketFactory(
        SSLUtils.createSSLSocketFactoryForCertUnchecked(pemCert));
    return mqConnectionFactory;
  }

  public static SerializableFunction<Void, MQConnectionFactory> getMqConnectionFactoryProviderFn(
      BasicOptions pipelineOptions) {
    String mqAppName = pipelineOptions.getMqAppName();
    PipelineVariant pipelineVariant = pipelineOptions.getPipelineVariant();
    String queueManger = pipelineOptions.getQueueManager();
    String mqHostName = pipelineOptions.getMqHostname();
    int mqPort = pipelineOptions.getMqPort();
    String channel = pipelineOptions.getChannel();
    String pemCert = pipelineOptions.getQMgrX509Certificate();

    return __ -> {
      try {
        return getMqConnectionFactory(
            mqAppName, pipelineVariant, queueManger, mqHostName, mqPort, channel, pemCert);
      } catch (JMSException e) {
        throw new RuntimeException(e);
      }
    };
  }
}
