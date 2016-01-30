package com.codetest.auth.storage;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import io.norberg.automatter.AutoMatter;

@AutoMatter
@JsonInclude(JsonInclude.Include.NON_NULL)
public interface UserData {

  String username();
  String password();
  String salt();
  String fullname();
  List<String> accessTimes();

}
