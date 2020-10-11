package ru.gg.lib_gwt;

import ru.gg.lib_gwt.data.*;

import java.io.Serializable;

public class Const {

  public static boolean ONLY_LOGIN = false;
  public static final String UNKNOWN_REGION = "UNKNOWN_REGION";
  public static final String RU = "RU";
  public static final String US = "US";
  public static final String ACTION = "action";
  public static final String RUN_PARAMS_JSON = "run_params.json";
  public static final String DEVICE_JSON = "device.json";
  public static final String ANDROID_LOG_SERVER = "http://10.0.2.2:54322/log";
  public static final String ANDROID_BREAK_SERVICE = "http://10.0.2.2:54322/break";
  public static final String BREAK = "break";
  public static final String ANDROID_BACK_SERVICE = "http://10.0.2.2:54322/back";
  public static final String CLIPBOARD_SERVICE = "http://10.0.2.2:54322/clipboard";
  public static final String CLIPBOARD_SERVICE_FROM_FILE = "http://10.0.2.2:54322/clipboard_file";
  public static final String CLIPBOARD_COMMENT_TITLE = "http://10.0.2.2:54322/clipboard_comment_title";
  public static final String CLIPBOARD_COMMENT_DESC = "http://10.0.2.2:54322/clipboard_comment_desc";
  public static final String BACK = "back";
  public static final String UTF_8 = "UTF-8";
  @Deprecated
  public static final int TARGET_RATING = 5;//todo bad

  public static enum ProxyType {
    socks5,
    socks4,
    https,
    http
  }

  public static enum AppPackageActionType {
    install,
    play,
    rate;
  }

  public static enum SmallTaskStatus {
    //        waiting,
    started,
    canceled,
    failed,
    completed
  }

  public static class JreAction {
    public static enum Type {
      start,
      finish,
      failed,
      getProxy,
      getAccount,
      getComment;
    }

    public static abstract class BasicReq extends JsonBasic {

    }

    public static abstract class BasicResp extends JsonBasic {

    }

    public static class Start {
      public static class Request extends BasicReq {
      }

      public static class Response extends BasicResp {
        public AndroidSmallTask androidTask;
      }
    }

    public static class GetAccount {
      public static class Request extends BasicReq {
        public Id.Account accountId;
      }

      public static class Response extends BasicResp {
        public Account account;
      }
    }

    public static class GetComment {
      public static class Request extends BasicReq {
        public Id.Comment commentId;
      }

      public static class Response extends BasicResp {
        public Comment comment;
      }
    }

    public static class Failed {
      public static class Request extends BasicReq {
        public Id.SmallTask smallTaskId;
      }

      public static class Response extends BasicResp {

      }
    }

    public static class Finish {
      public static class Request extends BasicReq {
        public Id.SmallTask smallTaskId;
      }

      public static class Response extends BasicResp {

      }
    }

    public static class GetProxy {
      public static class Request extends BasicReq {
        public String region;
      }

      public static class Response extends BasicResp {
        public ProxyDef proxy;
      }
    }

  }

  public static class Id implements Serializable {
    public int id;

    public Id(int id) {
      this.id = id;
    }

    public Id() {
    }

    @Override
    public String toString() {
      return String.valueOf(id);
    }

    @Override
    public boolean equals(Object obj) {
      try {
        return this.id == ((Id) obj).id;
      } catch (Error e) {
        return false;
      }

    }

    public static class BigTask extends Id {
      public BigTask(int id) {
        super(id);
      }

      public BigTask() {
        super();
      }
    }

    public static class SmallTask extends Id {
      public SmallTask(int id) {
        super(id);
      }

      public SmallTask() {
        super();
      }
    }

    public static class Comment extends Id {
      public Comment(int id) {
        super(id);
      }

      public Comment() {
        super();
      }
    }

    public static class Account extends Id {
      public Account(int id) {
        super(id);
      }

      public Account() {
        super();
      }
    }
  }


}
