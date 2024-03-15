package com.google.dce.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AverageMetric<T extends Number & Comparable<T>> {

  private final NavigableMap<Instant, List<T>> dataPointWindow =
      Collections.synchronizedNavigableMap(new TreeMap<>());
  private final Duration windowSize;
  private final Clock clock;

  private double sum = 0.0;
  private long sumLong = 0L;
  private int count = 0;
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  public AverageMetric(Duration windowSize, Clock clock) {
    this.windowSize = windowSize;
    this.clock = clock;
  }

  public AverageMetric(Duration duration) {
    this(duration, Clock.systemDefaultZone());
  }

  public void addDataPoint(T dataPoint) {
    addDataPoint(clock.instant(), dataPoint);
  }

  private void updateStats(double addDouble, long addLong, int addCount) {
    lock.writeLock().lock();
    try {
      sum += addDouble;
      sumLong += addLong;
      count += addCount;
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void addDataPoint(Instant i, T datapoint) {
    Instant oldThreshold = clock.instant().minus(windowSize);
    pruneOld(oldThreshold);
    oldThreshold = clock.instant().minus(windowSize);
    if (!i.isBefore(oldThreshold)) {
      updateStats(datapoint.doubleValue(), datapoint.longValue(), 1);
      computeDatapointList(i, datapoint);
    }
  }

  private void computeDatapointList(Instant i, T datapoint) {
    dataPointWindow.compute(
        i,
        (k, l) -> {
          if (l == null) {
            l = new ArrayList<>();
          }
          l.add(datapoint);
          return l;
        });
  }

  private void pruneOld(Instant oldThreshold) {

    for (Map.Entry<Instant, List<T>> firstEntry = dataPointWindow.firstEntry();
        firstEntry != null && firstEntry.getKey().isBefore(oldThreshold);
        firstEntry = dataPointWindow.firstEntry()) {
      List<T> removed = dataPointWindow.remove(firstEntry.getKey());

      if (removed != null) {
        double sumRemoved = removed.stream().mapToDouble(Number::doubleValue).sum();
        long sumLongRemoved = removed.stream().mapToLong(Number::longValue).sum();
        updateStats(-1 * sumRemoved, -1 * sumLongRemoved, -1 * removed.size());
      }
    }
  }

  public double getSum() {
    return sum;
  }

  public long getSumLong() {
    return sumLong;
  }

  public int getCount() {
    return count;
  }

  public double getAvg() {
    return getAvgOpt()
        .orElseThrow(() -> new IllegalStateException("no elements in data point window"));
  }

  public Optional<Double> getAvgOpt() {
    pruneOld(clock.instant().minus(windowSize));
    lock.readLock().lock();
    try {
      if (getCount() == 0) {
        return Optional.empty();
      }
      return Optional.of(getSum() / getCount());
    } finally {
      lock.readLock().unlock();
    }
  }
}
