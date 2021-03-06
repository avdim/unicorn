// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.treeView;

import com.intellij.openapi.util.NamedRunnable;
import org.jetbrains.annotations.NotNull;

abstract class TreeRunnable2 extends NamedRunnable {
  TreeRunnable2(@NotNull String name) {
    super(name);
  }

  protected abstract void perform();

  @Override
  public final void run() {
    trace("started");
    try {
      perform();
    }
    finally {
      trace("finished");
    }
  }

}
