package com.google.dce.autoscaler;

import static org.apache.beam.sdk.io.UnboundedSource.UnboundedReader.BACKLOG_UNKNOWN;

import com.google.dce.globals.GuiceInitializer;
import com.google.dce.globals.modules.AverageMetricModule;
import com.google.dce.metrics.AverageMetric;
import com.google.dce.mq.rest.QueueService;
import java.io.IOException;
import java.util.Optional;
import org.apache.beam.sdk.io.jms.AutoScaler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestAutoScaler implements AutoScaler {

  private static final Logger LOG = LoggerFactory.getLogger(RestAutoScaler.class);

  private final String queueManager;
  private final String queueName;
  private QueueService queueService;
  private AverageMetric<Long> averageMetric;

  public RestAutoScaler(String queueManager, String queueName) {
    this.queueManager = queueManager;
    this.queueName = queueName;
  }

  @Override
  public void start() {
    queueService = GuiceInitializer.getInstance(QueueService.class);
    averageMetric = GuiceInitializer.getInstance(AverageMetricModule.Key.INSTANCE);
  }

  @Override
  public long getTotalBacklogBytes() {
    try {
      LOG.info("Querying status of the queue {} in queue manager {}", queueName, queueManager);
      long queueDepth =
          Optional.ofNullable(queueService.getQueueCurrentDepth(queueManager, queueName))
              .map(Integer::longValue)
              .orElse(BACKLOG_UNKNOWN);

      Optional<Double> avgOpt = getAndLogAvgOpt(averageMetric, queueDepth);

      return avgOpt.map(avg -> Math.round(queueDepth * avg)).orElse(BACKLOG_UNKNOWN);
    } catch (IOException e) {
      LOG.warn(
          "Unable to query status of the queue {} in queue manager {}", queueName, queueManager, e);
      return BACKLOG_UNKNOWN;
    }
  }

  private Optional<Double> getAndLogAvgOpt(AverageMetric<Long> averageMetric, long queueDepth) {
    Optional<Double> avgOpt = averageMetric.getAvgOpt();

    avgOpt.ifPresentOrElse(
        avg ->
            LOG.info(
                String.format(
                    "queue depth for queue %s in queue manager %s is %s, with average message"
                        + " size %.2f B, giving %.1f KiB of backlog",
                    queueName, queueManager, queueDepth, avg, queueDepth * avg / 1024d)),
        () ->
            LOG.info(
                "queue depth for queue {} in queue manager {} is {}, no data for average message"
                    + " size, reporting unknown backlog size",
                queueName,
                queueManager,
                queueDepth));
    return avgOpt;
  }

  @Override
  public void stop() {}
}
