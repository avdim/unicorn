// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.smartPointers;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.LowMemoryWatcher;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

class SmartPointerTracker2 {
  private static final ReferenceQueue<SmartPsiElementPointerImpl<?>> ourQueue = new ReferenceQueue<>();

  private PointerReference[] references = new PointerReference[10];
//  private final MarkerCache markerCache = new MarkerCache(this);

  static {
    Application application = ApplicationManager.getApplication();
    if (!application.isDisposed()) {
      LowMemoryWatcher.register(() -> processQueue(), application);
    }
  }

  SmartPointerTracker2() {
  }

  synchronized void removeReference(@NotNull PointerReference reference) {
    int index = reference.index;
    if (index < 0) return;

    if (references[index] != reference) {
      throw new AssertionError("At " + index + " expected " + reference + ", found " + references[index]);
    }
    references[index].index = -1;
    references[index] = null;
  }

  static final class PointerReference extends WeakReference<SmartPsiElementPointerImpl<?>> {
    @NotNull final SmartPointerTracker2 tracker;
    private int index = -2;

    private PointerReference(@NotNull SmartPsiElementPointerImpl<?> pointer, @NotNull SmartPointerTracker2 tracker) {
      super(pointer, ourQueue);
      this.tracker = tracker;
//      pointer.pointerReference = this;
    }
  }

  static void processQueue() {
    while (true) {
      PointerReference reference = (PointerReference)ourQueue.poll();
      if (reference == null) break;

      if (reference.get() != null) {
        throw new IllegalStateException("Queued reference has referent!");
      }

      reference.tracker.removeReference(reference);
    }
  }

}
