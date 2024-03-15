package com.google.dce.utils;

import java.io.Serializable;
import java.util.function.Consumer;

public interface SerializableConsumerFn<T> extends Serializable, Consumer<T> {}
