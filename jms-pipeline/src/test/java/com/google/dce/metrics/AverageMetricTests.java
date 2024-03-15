package com.google.dce.metrics;

import static org.junit.jupiter.api.Assertions.*;

import com.google.dce.clock.MutableClock;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class AverageMetricTests {

  @Test
  public void testCorrectlySumsValues() {
    Clock clock = Clock.fixed(Instant.ofEpochMilli(0L), ZoneId.of("UTC"));
    AverageMetric<Integer> averageMetric = new AverageMetric<>(Duration.ofMinutes(1), clock);
    averageMetric.addDataPoint(1);
    averageMetric.addDataPoint(2);
    averageMetric.addDataPoint(2);
    averageMetric.addDataPoint(10);
    assertMetric(averageMetric, 15d, 15L, 4);
  }

  public static void assertMetric(
      AverageMetric metric, double sum, long sumLong, int dataPointCount) {
    assertEquals(sum, metric.getSum());
    assertEquals(sumLong, metric.getSumLong());
    assertEquals(sum / dataPointCount, metric.getAvg());
  }

  @Test
  public void testCorrectlyRemovesOldDataPoints() {
    MutableClock clock = new MutableClock(Instant.ofEpochMilli(0L), ZoneId.of("UTC"));
    AverageMetric<Integer> averageMetric = new AverageMetric<>(Duration.ofMinutes(1), clock);
    averageMetric.addDataPoint(1);
    assertMetric(averageMetric, 1d, 1L, 1);
    clock.advance(Duration.ofSeconds(35));
    averageMetric.addDataPoint(2);
    assertMetric(averageMetric, 3d, 3L, 2);
    clock.advance(Duration.ofSeconds(35));
    averageMetric.addDataPoint(10);
    assertMetric(averageMetric, 12d, 12L, 2);
  }

  @Test
  public void concurrencyTest() throws InterruptedException {
    int parallelism = 5;
    int dataPointsToInsert = 100_000;
    Duration windowSize = Duration.ofSeconds(2L);
    MutableClock mutableClock = new MutableClock(Instant.ofEpochMilli(0), ZoneId.of("UTC"));
    Random randomSeed = new Random();
    Random[] randoms = new Random[parallelism];
    Random[] randoms2 = new Random[parallelism];
    Thread[] threads = new Thread[parallelism];
    Runnable[] runnables = new Runnable[parallelism];
    AverageMetric<Integer> averageMetricConcurrent = new AverageMetric<>(windowSize, mutableClock);
    AverageMetric<Integer> averageMetricSerial = new AverageMetric<>(windowSize, mutableClock);
    for (int i = 0; i < parallelism; i += 1) {
      long seed = randomSeed.nextLong();
      randoms[i] = new Random(seed);
      randoms2[i] = new Random(seed);
      Random r = randoms[i];
      runnables[i] =
          () -> {
            for (int j = 0; j < dataPointsToInsert; j += 1) {
              averageMetricConcurrent.addDataPoint(r.nextInt());
            }
          };
    }
    Runnable serial =
        () -> {
          for (int i = 0; i < parallelism; i += 1) {
            for (int j = 0; j < dataPointsToInsert; j += 1) {
              averageMetricSerial.addDataPoint(randoms2[i].nextInt());
            }
          }
        };

    for (int u = 0; u < 100; u += 1) {
      for (int i = 0; i < parallelism; i += 1) {
        threads[i] = new Thread(runnables[i]);
        threads[i].start();
      }
      Thread serialThread = new Thread(serial);
      serialThread.start();
      for (int i = 0; i < parallelism; i += 1) {
        threads[i].join();
      }
      serialThread.join();
      assertEquals(averageMetricSerial.getAvg(), averageMetricConcurrent.getAvg());
      mutableClock.advance(Duration.ofMillis(100));
    }
  }
}
