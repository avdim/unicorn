// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.treeView;

import com.intellij.openapi.util.Condition;
import com.intellij.ide.projectView.impl.nodes.AbstractTreeNod2;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.*;

public class UpdaterTreeState2 {
  private final AbstractTreeUi2 myUi;
  private final Map<Object, Object> myToSelect = new WeakHashMap<>();
  private Map<Object, Condition> myAdjustedSelection = new WeakHashMap<>();
  private final Map<Object, Object> myToExpand = new WeakHashMap<>();
  private int myProcessingCount;

  private boolean myCanRunRestore = true;

  private final WeakHashMap<Object, Object> myAdjustmentCause2Adjustment = new WeakHashMap<>();

  UpdaterTreeState2(AbstractTreeUi2 ui) {
    myUi = ui;
    final JTree tree = myUi.getTree();
    putAll(addPaths(tree.getSelectionPaths()), myToSelect);
    putAll(addPaths(tree.getExpandedDescendants(new TreePath(tree.getModel().getRoot()))), myToExpand);
  }

  private static void putAll(final Set<Object> source, final Map<Object, Object> target) {
    for (Object o : source) {
      target.put(o, o);
    }
  }

  private Set<Object> addPaths(Object[] elements) {
    Set<Object> set = new HashSet<>();
    if (elements != null) {
      ContainerUtil.addAll(set, elements);
    }

    return addPaths(set);
  }

  private Set<Object> addPaths(Enumeration elements) {
    ArrayList<Object> elementArray = new ArrayList<>();
    if (elements != null) {
      while (elements.hasMoreElements()) {
        Object each = elements.nextElement();
        elementArray.add(each);
      }
    }

    return addPaths(elementArray);
  }

  private Set<Object> addPaths(Collection elements) {
    Set<Object> target = new HashSet<>();

    if (elements != null) {
      for (Object each : elements) {
        final Object node = ((TreePath)each).getLastPathComponent();
        if (node instanceof DefaultMutableTreeNode) {
          final Object descriptor = ((DefaultMutableTreeNode)node).getUserObject();
          if (descriptor instanceof NodeDescriptor) {
            final Object element = myUi.getElementFromDescriptor((AbstractTreeNod2)descriptor);
            if (element != null) {
              target.add(element);
            }
          }
        }
      }
    }
    return target;
  }

  public Object /*@NotNull*/ [] getToSelect() {
    return ArrayUtil.toObjectArray(myToSelect.keySet());
  }

  public void process(@NotNull Runnable runnable) {
    try {
      setProcessingNow(true);
      runnable.run();
    }
    finally {
      setProcessingNow(false);
    }
  }

  boolean isProcessingNow() {
    return myProcessingCount > 0;
  }

  public void addAll(@NotNull UpdaterTreeState2 state) {
    myToExpand.putAll(state.myToExpand);

    Object[] toSelect = state.getToSelect();
    for (Object each : toSelect) {
      if (!myAdjustedSelection.containsKey(each)) {
        myToSelect.put(each, each);
      }
    }

    myCanRunRestore = state.myCanRunRestore;
  }

  public boolean restore(@Nullable DefaultMutableTreeNode actionNode) {
    return true;
  }

  void beforeSubtreeUpdate() {
    myCanRunRestore = true;
  }

  void clearExpansion() {
    myToExpand.clear();
  }

  public void addSelection(final Object element) {
    myToSelect.put(element, element);
  }

  void addAdjustedSelection(final Object element, Condition isExpired, @Nullable Object adjustmentCause) {
    myAdjustedSelection.put(element, isExpired);
    if (adjustmentCause != null) {
      myAdjustmentCause2Adjustment.put(adjustmentCause, element);
    }
  }

  @NonNls
  @Override
  public String toString() {
    return "UpdaterState toSelect " +
           myToSelect + " toExpand=" +
           myToExpand + " processingNow=" + isProcessingNow() + " canRun=" + myCanRunRestore;
  }

  private void setProcessingNow(boolean processingNow) {
    if (processingNow) {
      myProcessingCount++;
    } else {
      myProcessingCount--;
    }
    if (!isProcessingNow()) {
      myUi.maybeReady();
    }
  }

}
