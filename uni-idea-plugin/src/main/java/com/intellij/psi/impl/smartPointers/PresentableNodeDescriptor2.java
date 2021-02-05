// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.smartPointers;

import com.intellij.ide.projectView.PresentationData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
