package com.codetest.auth.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.norberg.automatter.jackson.AutoMatterModule;

public class ObjectMappers {

  private ObjectMappers() { }

  public static final ObjectMapper JSON = new ObjectMapper()
      .registerModule(new AutoMatterModule());

}
