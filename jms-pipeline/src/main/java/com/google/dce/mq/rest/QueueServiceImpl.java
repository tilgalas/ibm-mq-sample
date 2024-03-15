package com.google.dce.mq.rest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.openapitools.client.api.QueueApi;
import org.openapitools.client.model.QueueAttributeGetPojo;
import org.openapitools.client.model.QueueStatusAttributePojo;

public class QueueServiceImpl implements QueueService {

  private final QueueApi queueApi;

  public QueueServiceImpl(QueueApi queueApi) {
    this.queueApi = queueApi;
  }

  public static <T> Optional<T> listHead(List<T> list) {
    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }

  @Nullable
  @Override
  public Integer getQueueCurrentDepth(String queueManager, String queueName) throws IOException {
    return Optional.ofNullable(
            queueApi
                .getQueue(queueManager, queueName, Map.of("status", "status.currentDepth"))
                .getQueue())
        .flatMap(QueueServiceImpl::listHead)
        .map(QueueAttributeGetPojo::getStatus)
        .map(QueueStatusAttributePojo::getCurrentDepth)
        .orElse(null);
  }
}
