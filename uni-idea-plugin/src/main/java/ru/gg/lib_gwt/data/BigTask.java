package ru.gg.lib_gwt.data;

import ru.gg.lib_gwt.Const;
import ru.gg.lib_gwt.JsonBasic;

import java.util.ArrayList;

public class BigTask extends JsonBasic {

  public Const.Id.BigTask id;
  public BigTaskDef def;
  public int installCount;
  public int rateCount;
  public int commentCount;
  public String info;
  public boolean pause;
  public float currentRating = Const.TARGET_RATING;
  public ArrayList<Integer> recentlyInstallUnixTimeSec = new ArrayList<>();
  public boolean ignoreHourLimit;
  public boolean ignoreDailyLimit;
}
