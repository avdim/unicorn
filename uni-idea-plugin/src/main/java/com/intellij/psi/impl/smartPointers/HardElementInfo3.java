/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.impl.smartPointers;

import com.intellij.openapi.util.Segment;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;

class HardElementInfo3 extends SmartPointerElementInfo2 {
  @NotNull
  private final PsiElement myElement;

  HardElementInfo3(@NotNull PsiElement element) {
    Uni.getLog().warning("use HardElementInfo3");
    myElement = element;
  }

  @Override
  PsiElement restoreElement() {
    return myElement;
  }

  @Override
  PsiFile restoreFile(@NotNull SmartPointerManagerImpl2 manager) {
    return myElement.isValid() ? myElement.getContainingFile() : null;
  }

  @Override
  int elementHashCode() {
    return myElement.hashCode();
  }

  @Override
  boolean pointsToTheSameElementAs(@NotNull final SmartPointerElementInfo2 other) {
    return other instanceof HardElementInfo3 && myElement.equals(((HardElementInfo3)other).myElement);
  }

  @Override
  VirtualFile getVirtualFile() {
    return PsiUtilCore.getVirtualFile(myElement);
  }

  @Override
  Segment getRange(@NotNull SmartPointerManagerImpl2 manager) {
    return myElement.getTextRange();
  }

  @Override
  Segment getPsiRange(@NotNull SmartPointerManagerImpl2 manager) {
    return getRange(manager);
  }

  @Override
  public String toString() {
    return "hard{" + myElement + " of " + myElement.getClass() + "}";
  }
}
