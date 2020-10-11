package ru.gg.lib_gwt.data;

import ru.gg.lib_gwt.JsonBasic;

public class BoolOrStrResult extends JsonBasic {

  public boolean success;
  public String error;

  public BoolOrStrResult() {
  }

  public BoolOrStrResult(boolean success, String error) {
    this.success = success;
    this.error = error;
  }
}
