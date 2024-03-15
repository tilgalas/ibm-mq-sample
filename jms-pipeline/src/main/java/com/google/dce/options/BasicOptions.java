package com.google.dce.options;

import com.ibm.msg.client.wmq.WMQConstants;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.Validation;

@SuppressWarnings("unused")
public interface BasicOptions extends PipelineOptions {

  @Description("pipeline variant to run (CONSUMER/PRODUCER)")
  @Default.Enum("CONSUMER")
  PipelineVariant getPipelineVariant();

  void setPipelineVariant(PipelineVariant value);

  @Description("IBM MQ Application name")
  @Default.String("Dataflow JMS")
  String getMqAppName();

  void setMqAppName(String value);

  @Description("MQ Queue Manager name")
  @Default.String("QM1")
  String getQueueManager();

  void setQueueManager(String value);

  @Description("MQ Channel name")
  @Default.String("DEV.APP.SVRCONN")
  String getChannel();

  void setChannel(String value);

  @Description("MQ hostname")
  @Validation.Required
  String getMqHostname();

  void setMqHostname(String value);

  @Description("MQ port")
  @Default.Integer(WMQConstants.WMQ_DEFAULT_CLIENT_PORT)
  int getMqPort();

  void setMqPort(int value);

  @Description("MQ username")
  @Validation.Required
  String getMqUsername();

  void setMqUsername(String value);

  @Description("MQ password")
  String getMqPassword();

  void setMqPassword(String value);

  @Description("Queue name")
  @Default.String("DEV.QUEUE.1")
  String getQueueName();

  void setQueueName(String value);

  @Description("Message size topic name")
  @Default.String("dev/")
  String getMessageSizeTopicName();

  void setMessageSizeTopicName(String value);

  @Default.String("https://localhost:9443")
  @Description("MQ REST API base path")
  String getMqApiBasePath();

  void setMqApiBasePath(String value);

  @Description("A Basic auth user name to access MQ WEB REST API")
  @Default.String("mqreader")
  String getMqApiBasicAuthUser();

  void setMqApiBasicAuthUser(String value);

  @Description("A Basic auth password to access MQ WEB REST API")
  @Default.String("mqreader")
  String getMqApiBasicAuthPassword();

  void setMqApiBasicAuthPassword(String value);

  @Description("A PEM encoded X509 certificate of the MQ WEB REST API server")
  String getMqApiX509Certificate();

  void setMqApiX509Certificate(String value);

  @Description("A PEM encoded X509 certificate of the MQ Queue Manager")
  String getQMgrX509Certificate();

  void setQMgrX509Certificate(String value);

  @Default.Long(180L)
  Long getConsumeElementRate();

  void setConsumeElementRate(Long value);

  @Default.Long(200L)
  Long getProduceElementRate();

  void setProduceElementRate(Long value);

  @Default.Integer(300)
  Integer getAutoScalerMetricWindowDurationSecs();

  void setAutoScalerMetricWindowDurationSecs(Integer value);
}
