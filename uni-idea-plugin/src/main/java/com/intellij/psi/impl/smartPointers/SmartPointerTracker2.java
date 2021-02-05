// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.smartPointers;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.LowMemoryWatcher;

class SmartPointerTracker2 {
//  private final MarkerCache markerCache = new MarkerCache(this);

  static {
    Application application = ApplicationManager.getApplication();
    if (!application.isDisposed()) {
      LowMemoryWatcher.register(() -> processQueue(), application);
    }
  }

  SmartPointerTracker2() {
  }

  static void processQueue() {
  }

}
