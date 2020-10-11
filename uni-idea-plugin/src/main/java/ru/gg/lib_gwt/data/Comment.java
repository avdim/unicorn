package ru.gg.lib_gwt.data;

import ru.gg.lib_gwt.Const;
import ru.gg.lib_gwt.JsonBasic;

public class Comment extends JsonBasic {

  public Const.Id.Comment id;
  public String title;
  public String desc;
  public int rating;
  public String region;
  public int usage;
  public int lastUsageUnixTime;

}
