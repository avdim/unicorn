// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.smartPointers;

import com.intellij.ide.util.treeView.WeighedItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;

public abstract class NodeDescriptor2<E> {
  public static final NodeDescriptor2<?>[] EMPTY_ARRAY = new NodeDescriptor2[0];
  public static final int DEFAULT_WEIGHT = 30;

  private final NodeDescriptor2<?> myParentDescriptor;

  protected @NlsSafe String myName;
  @Nullable protected Icon myClosedIcon;

  /**
   * @deprecated Unused. Left for API compatibility.
   */
  @Deprecated
  protected Icon myOpenIcon;
  protected Color myColor;

  private int myIndex = -1;

  private long myChildrenSortingStamp = -1;
  private long myUpdateCount;

  private boolean myWasDeclaredAlwaysLeaf;

  public NodeDescriptor2(@Nullable NodeDescriptor2<?> parentDescriptor) {
    myParentDescriptor = parentDescriptor;
  }

  @Nullable
  public NodeDescriptor2<?> getParentDescriptor() {
    return myParentDescriptor;
  }

  public int getIndex() {
    return myIndex;
  }

  public void setIndex(int index) {
    myIndex = index;
  }

  /**
   * Make sure the descriptor is up to date with its content
   *
   * @return true if any descriptor's properties changed during the update
   */
  public abstract boolean update();

  public abstract E getElement();

  @Override
  public @NlsSafe String toString() {
    // NB!: this method may return null if node is not valid
    // it contradicts the specification, but the fix breaks existing behaviour
    // see com.intellij.ide.util.FileStructurePopup#getSpeedSearchText
    return myName;
  }

  /**
   * @deprecated Use {@link #getIcon()} instead
   */
  @Deprecated
  public final Icon getOpenIcon() {
    return getIcon();
  }

  /**
   * @deprecated Use {@link #getIcon()} instead
   */
  @Deprecated
  public final Icon getClosedIcon() {
    return getIcon();
  }

  @Nullable
  public final Icon getIcon() {
    return myClosedIcon;
  }

  public final Color getColor() {
    return myColor;
  }

  public boolean expandOnDoubleClick() {
    return true;
  }

  public int getWeight() {
    E element = getElement();
    if (element instanceof WeighedItem) {
      return ((WeighedItem) element).getWeight();
    }
    return DEFAULT_WEIGHT;
  }

  public final long getChildrenSortingStamp() {
    return myChildrenSortingStamp;
  }

  public final void setChildrenSortingStamp(long stamp) {
    myChildrenSortingStamp = stamp;
  }

  public final long getUpdateCount() {
    return myUpdateCount;
  }

  public final void setUpdateCount(long updateCount) {
    myUpdateCount = updateCount;
  }

  public boolean isWasDeclaredAlwaysLeaf() {
    return myWasDeclaredAlwaysLeaf;
  }

  public void setWasDeclaredAlwaysLeaf(boolean leaf) {
    myWasDeclaredAlwaysLeaf = leaf;
  }

  public void applyFrom(@NotNull NodeDescriptor2<?> desc) {
    setIcon(desc.getIcon());
    myName = desc.myName;
    myColor = desc.myColor;
  }

  public void setIcon(@Nullable Icon closedIcon) {
    myClosedIcon = closedIcon;
  }

  public abstract static class NodeComparator<T extends NodeDescriptor2<?>> implements Comparator<T> {
    private long myStamp;

    public final void setStamp(long stamp) {
      myStamp = stamp;
    }

    public long getStamp() {
      return myStamp;
    }

    public void incStamp() {
      setStamp(getStamp() + 1);
    }

    public static final class Delegate<T extends NodeDescriptor2<?>> extends NodeComparator<T> {
      @NotNull
      private NodeComparator<? super T> myDelegate;

      public Delegate(@NotNull NodeComparator<? super T> delegate) {
        myDelegate = delegate;
      }

      public void setDelegate(@NotNull NodeComparator<? super T> delegate) {
        myDelegate = delegate;
      }

      @Override
      public long getStamp() {
        return myDelegate.getStamp();
      }

      @Override
      public void incStamp() {
        myDelegate.incStamp();
      }

      @Override
      public int compare(T o1, T o2) {
        return myDelegate.compare(o1, o2);
      }
    }
  }
}
