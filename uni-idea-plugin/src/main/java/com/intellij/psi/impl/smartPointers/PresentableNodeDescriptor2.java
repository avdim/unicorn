// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.smartPointers;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.StartupUiUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public abstract class PresentableNodeDescriptor2<E> extends NodeDescriptor2<E> {
  private PresentationData myTemplatePresentation;
  private PresentationData myUpdatedPresentation;

  protected PresentableNodeDescriptor2(@Nullable NodeDescriptor2 parentDescriptor) {
    super(parentDescriptor);
  }

  @Override
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

  @Override
  public void applyFrom(@NotNull NodeDescriptor2 desc) {
    if (desc instanceof PresentableNodeDescriptor2) {
      apply(((PresentableNodeDescriptor2<?>)desc).getPresentation());
    }
    else {
      super.applyFrom(desc);
    }
  }

  protected final boolean apply(@NotNull PresentationData presentation, @Nullable PresentationData before) {
    setIcon(presentation.getIcon(false));
    myName = presentation.getPresentableText();
    myColor = presentation.getForcedTextForeground();
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

  public boolean isContentHighlighted() {
    return false;
  }

  public boolean isHighlightableContentNode(@NotNull PresentableNodeDescriptor2 kid) {
    return true;
  }

  public PresentableNodeDescriptor2<?> getChildToHighlightAt(int index) {
    return null;
  }

  public boolean isParentOf(@NotNull NodeDescriptor2 eachNode) {
    NodeDescriptor2 eachParent = eachNode.getParentDescriptor();
    while (eachParent != null) {
      if (eachParent == this) return true;
      eachParent = eachParent.getParentDescriptor();
    }
    return false;
  }

  public boolean isAncestorOrSelf(NodeDescriptor2 selectedNode) {
    NodeDescriptor2<?> node = selectedNode;
    while (node != null) {
      if (equals(node)) return true;
      node = node.getParentDescriptor();
    }
    return false;
  }

  @NotNull
  public Color getHighlightColor() {
    return StartupUiUtil.isUnderDarcula() ? ColorUtil.shift(UIUtil.getTreeBackground(), 1.1) : UIUtil.getTreeBackground().brighter();
  }

  public static class ColoredFragment2 {
    private final @NlsSafe String myText;
    private final @NlsSafe String myToolTip;
    private final SimpleTextAttributes myAttributes;

    public ColoredFragment2(@NlsSafe String aText, SimpleTextAttributes aAttributes) {
      this(aText, null, aAttributes);
    }

    public ColoredFragment2(@NlsSafe String aText, @NlsSafe String toolTip, SimpleTextAttributes aAttributes) {
      myText = aText == null? "" : aText;
      myAttributes = aAttributes;
      myToolTip = toolTip;
    }

    public @NlsSafe String getToolTip() {
      return myToolTip;
    }

    public @NlsSafe String getText() {
      return myText;
    }

    public SimpleTextAttributes getAttributes() {
      return myAttributes;
    }


    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final ColoredFragment2 that = (ColoredFragment2)o;

      if (myAttributes != null ? !myAttributes.equals(that.myAttributes) : that.myAttributes != null) return false;
      if (myText != null ? !myText.equals(that.myText) : that.myText != null) return false;
      if (myToolTip != null ? !myToolTip.equals(that.myToolTip) : that.myToolTip != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = myText != null ? myText.hashCode() : 0;
      result = 31 * result + (myToolTip != null ? myToolTip.hashCode() : 0);
      result = 31 * result + (myAttributes != null ? myAttributes.hashCode() : 0);
      return result;
    }
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
