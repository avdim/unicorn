package ru.gg.lib_gwt.data;

import ru.gg.lib_gwt.JsonBasic;

public class BigTaskDef extends JsonBasic {

  public String name;
  public String region;
  public String appPackage;
  public float rating;
  public float rateRatio = 1.0f;
  public float commentRatio;
  public int dailyLimit;
  public int installCount;

}
