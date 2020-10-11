package ru.gg.lib_gwt.data;

import ru.gg.lib_gwt.Const;
import ru.gg.lib_gwt.JsonBasic;
import ru.gg.lib_gwt.LibAllGwt;

import java.util.ArrayList;

public class Account extends JsonBasic {

  public Const.Id.Account id;
  public String mail;
  public String password;
  public String region;
  public String reserveMail;
  public int usage;
  public ArrayList<String> ratedApps = new ArrayList<>();
  public ArrayList<String> installedApps = new ArrayList<>();
  public int successTasks;
  public int failedTasks;

  @Override
  public int hashCode() {
    return mail.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    try {
      return LibAllGwt.strEquals((String) obj, this.mail);
    } catch (Error e) {
      return false;
    }
  }

}
