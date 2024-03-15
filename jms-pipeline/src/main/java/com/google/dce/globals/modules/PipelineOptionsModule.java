package com.google.dce.globals.modules;

import com.google.dce.options.BasicOptions;
import com.google.inject.AbstractModule;

public class PipelineOptionsModule extends AbstractModule {
  private final BasicOptions basicOptions;

  public PipelineOptionsModule(BasicOptions basicOptions) {
    this.basicOptions = basicOptions;
  }

  @Override
  protected void configure() {
    bind(BasicOptions.class).toInstance(this.basicOptions);
  }
}
