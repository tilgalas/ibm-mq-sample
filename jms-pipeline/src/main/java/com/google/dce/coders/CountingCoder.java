package com.google.dce.coders;

import com.google.common.io.CountingOutputStream;
import com.google.dce.utils.SerializableConsumerFn;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.beam.sdk.coders.Coder;
import org.apache.beam.sdk.coders.CustomCoder;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CountingCoder<T> extends CustomCoder<T> {

  private final Coder<T> delegate;
  private final SerializableConsumerFn<Long> messageSizeConsumer;

  public CountingCoder(Coder<T> delegate, SerializableConsumerFn<Long> messageSizeConsumer) {
    this.delegate = delegate;
    this.messageSizeConsumer = messageSizeConsumer;
  }

  @Override
  public void encode(T value, @NonNull OutputStream outStream) throws IOException {
    CountingOutputStream cos = new CountingOutputStream(outStream);
    delegate.encode(value, cos);
    messageSizeConsumer.accept(cos.getCount());
  }

  @Override
  public T decode(@NonNull InputStream inStream) throws IOException {
    return delegate.decode(inStream);
  }

  @Override
  public void verifyDeterministic() throws NonDeterministicException {
    delegate.verifyDeterministic();
  }
}
