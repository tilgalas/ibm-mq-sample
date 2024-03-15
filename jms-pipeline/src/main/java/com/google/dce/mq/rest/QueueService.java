package com.google.dce.mq.rest;

import java.io.IOException;
import javax.annotation.Nullable;

public interface QueueService {
  @Nullable
  Integer getQueueCurrentDepth(String queueManager, String queueName) throws IOException;
}
