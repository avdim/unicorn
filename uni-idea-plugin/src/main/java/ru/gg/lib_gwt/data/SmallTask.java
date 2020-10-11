package ru.gg.lib_gwt.data;

import ru.gg.lib_gwt.Const;
import ru.gg.lib_gwt.JsonBasic;

import java.util.Date;

public class SmallTask extends JsonBasic {

  public Const.Id.SmallTask id;
  public Const.SmallTaskStatus status;
  public Const.Id.BigTask bigTaskId;
  public Const.Id.Account accountId;
  public Rate rate;
  public Date startTime;

}
