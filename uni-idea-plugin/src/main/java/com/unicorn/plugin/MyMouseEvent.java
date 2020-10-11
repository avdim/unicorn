package com.unicorn.plugin;

import java.awt.*;
import java.awt.event.MouseEvent;

public class MyMouseEvent extends MouseEvent {
  public MyMouseEvent(Component source, int id, long time, int modifiers, int x, int y, int clickCount, boolean popupTrigger, int button) {
    super(source, id, time, modifiers, x, y, clickCount, popupTrigger, button);
  }
}
