// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.smartPointers;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.impl.nodes.ProjectViewNode2;
import com.intellij.ide.util.treeView.WeighedItem;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class PresentableNodeDescriptor2<E> {
  public static final int DEFAULT_WEIGHT = 30;
  protected @NlsSafe String myName;
  @Nullable protected Icon myClosedIcon;
  private PresentationData myTemplatePresentation;
  private PresentationData myUpdatedPresentation;
  private int myIndex = -1;
  private long myChildrenSortingStamp = -1;
  private long myUpdateCount;
  private boolean myWasDeclaredAlwaysLeaf;

  protected PresentableNodeDescriptor2() {

  }

  public final boolean update() {
    if (shouldUpdateData()) {
      PresentationData before = getPresentation().clone();
      PresentationData updated = getUpdatedPresentation();
      return shouldApply() && apply(updated, before);
    }
    return false;
  }

  protected final boolean apply(@NotNull PresentationData presentation) {
    return apply(presentation, null);
  }

  public void applyFrom(@NotNull PresentableNodeDescriptor2 desc) {
    if (desc instanceof PresentableNodeDescriptor2) {
      apply(((PresentableNodeDescriptor2<?>)desc).getPresentation());
    }
    else {
      setIcon(desc.getIcon());
      myName = desc.myName;
    }
  }

  protected final boolean apply(@NotNull PresentationData presentation, @Nullable PresentationData before) {
    setIcon(presentation.getIcon(false));
    myName = presentation.getPresentableText();
    boolean updated = !presentation.equals(before);

    if (myUpdatedPresentation == null) {
      myUpdatedPresentation = createPresentation();
    }

    myUpdatedPresentation.copyFrom(presentation);

    if (myTemplatePresentation != null) {
      myUpdatedPresentation.applyFrom(myTemplatePresentation);
    }

    updated |= myUpdatedPresentation.isChanged();
    myUpdatedPresentation.setChanged(false);

    return updated;
  }

  @NotNull
  private PresentationData getUpdatedPresentation() {
    PresentationData presentation = myUpdatedPresentation != null ? myUpdatedPresentation : createPresentation();
    myUpdatedPresentation = presentation;
    presentation.clear();
    update(presentation);

    if (shouldPostprocess()) {
      postprocess(presentation);
    }

    return presentation;
  }

  @NotNull
  protected PresentationData createPresentation() {
    return new PresentationData();
  }

  protected void postprocess(@NotNull PresentationData date) {

  }

  protected boolean shouldPostprocess() {
    return true;
  }

  protected boolean shouldApply() {
    return true;
  }

  protected boolean shouldUpdateData() {
    return true;
  }

  protected abstract void update(@NotNull PresentationData presentation);

  @NotNull
  public final PresentationData getPresentation() {
    return myUpdatedPresentation == null ? getTemplatePresentation() : myUpdatedPresentation;
  }

  @NotNull
  protected final PresentationData getTemplatePresentation() {
    if (myTemplatePresentation == null) {
      myTemplatePresentation = createPresentation();
    }

    return myTemplatePresentation;
  }

  @Nullable
  abstract public PresentableNodeDescriptor2 getParentDescriptor();

  public int getIndex() {
    return myIndex;
  }

  public void setIndex(int index) {
    myIndex = index;
  }

  public abstract ProjectViewNode2 getElement();

  @Override
  public @NlsSafe String toString() {
    // NB!: this method may return null if node is not valid
    // it contradicts the specification, but the fix breaks existing behaviour
    // see com.intellij.ide.util.FileStructurePopup#getSpeedSearchText
    return myName;
  }

  @Nullable
  public final Icon getIcon() {
    return myClosedIcon;
  }

  public int getWeight() {
    ProjectViewNode2 element = getElement();
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

  public void setIcon(@Nullable Icon closedIcon) {
    myClosedIcon = closedIcon;
  }

  /*public @NlsSafe String getName() {
    if (!getPresentation().getColoredText().isEmpty()) {
      StringBuilder result = new StringBuilder();
      for (PresentableNodeDescriptor.ColoredFragment each : getPresentation().getColoredText()) {
        result.append(each.getText());
      }
      return result.toString();
    }
    return myName;
  }*/
}
