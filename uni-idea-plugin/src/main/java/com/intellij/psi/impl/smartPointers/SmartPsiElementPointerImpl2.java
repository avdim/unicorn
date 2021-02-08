/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import com.intellij.extapi.psi.ASTDelegatePsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.FreeThreadedFileViewProvider;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

class SmartPsiElementPointerImpl2<E extends PsiElement> implements SmartPointerEx<E> {
  private static final Logger LOG = Logger.getInstance(SmartPsiElementPointerImpl2.class);

  private Reference<E> myElement;
  public final SmartPointerElementInfo2 myElementInfo;
  protected final SmartPointerManagerImpl2 myManager;
  private byte myReferenceCount = 1;

  SmartPsiElementPointerImpl2(@NotNull SmartPointerManagerImpl2 manager,
                              @NotNull E element,
                              @Nullable PsiFile containingFile,
                              boolean forInjected) {
    this(manager, element, createElementInfo(manager, element, containingFile, forInjected));
  }
  SmartPsiElementPointerImpl2(@NotNull SmartPointerManagerImpl2 manager,
                              @NotNull E element,
                              @NotNull SmartPointerElementInfo2 elementInfo) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    myElementInfo = elementInfo;
    myManager = manager;
    cacheElement(element);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof SmartPsiElementPointer && pointsToTheSameElementAs(this, (SmartPsiElementPointer<?>)obj);
  }

  @Override
  public int hashCode() {
    return myElementInfo.elementHashCode();
  }

  @Override
  @NotNull
  public Project getProject() {
    return myManager.getProject();
  }

  @Override
  @Nullable
  public E getElement() {
    if (getProject().isDisposed()) return null;

    E element = getCachedElement();
    if (element == null || !element.isValid()) {
      element = doRestoreElement();
      cacheElement(element);
    }
    return element;
  }

  @Nullable
  E doRestoreElement() {
    //noinspection unchecked
    E element = (E)myElementInfo.restoreElement(myManager);
    if (element != null && !element.isValid()) {
      return null;
    }
    return element;
  }

  void cacheElement(@Nullable E element) {
    myElement = element == null ? null : 
                PsiManagerEx.getInstanceEx(getProject()).isBatchFilesProcessingMode() ? new WeakReference<>(element) :
                new SoftReference<>(element);
  }

  @Override
  public E getCachedElement() {
    return com.intellij.reference.SoftReference.dereference(myElement);
  }

  @Override
  public PsiFile getContainingFile() {
    PsiFile file = getElementInfo().restoreFile(myManager);

    if (file != null) {
      return file;
    }

    final Document doc = myElementInfo.getDocumentToSynchronize();
    if (doc == null) {
      final E resolved = getElement();
      return resolved == null ? null : resolved.getContainingFile();
    }
    return PsiDocumentManager.getInstance(getProject()).getPsiFile(doc);
  }

  @Override
  public VirtualFile getVirtualFile() {
    return myElementInfo.getVirtualFile();
  }

  @Override
  public Segment getRange() {
    return myElementInfo.getRange(myManager);
  }

  @Nullable
  @Override
  public Segment getPsiRange() {
    return myElementInfo.getPsiRange(myManager);
  }

  @NotNull
  private static <E extends PsiElement> SmartPointerElementInfo2 createElementInfo(@NotNull SmartPointerManagerImpl2 manager,
                                                                                  @NotNull E element,
                                                                                  @Nullable PsiFile containingFile,
                                                                                  boolean forInjected) {
    SmartPointerElementInfo2 elementInfo = doCreateElementInfo(manager.getProject(), element, containingFile, forInjected);
    if (ApplicationManager.getApplication().isUnitTestMode() && !ApplicationInfoImpl.isInStressTest()) {
      PsiElement restored = elementInfo.restoreElement(manager);
      if (!element.equals(restored)) {
        // likely cause: PSI having isPhysical==true, but which can't be restored by containing file and range. To fix, make isPhysical return false
        LOG.error("Cannot restore " + element + " of " + element.getClass() + " from " + elementInfo +
                  "; restored=" + restored + (restored == null ? "" : " of "+restored.getClass())+" in " + element.getProject());
      }
    }
    return elementInfo;
  }

  @NotNull
  private static <E extends PsiElement> SmartPointerElementInfo2 doCreateElementInfo(@NotNull Project project,
                                                                                    @NotNull E element,
                                                                                    @Nullable PsiFile containingFile,
                                                                                    boolean forInjected) {
    if (element instanceof PsiDirectory) {
      return new DirElementInfo2((PsiDirectory)element);
    }
//    if (element instanceof PsiCompiledElement || containingFile == null) {
//      if (element instanceof StubBasedPsiElement && element instanceof PsiCompiledElement) {
//        if (element instanceof PsiFile) {
//          return new FileElementInfo2((PsiFile)element);
//        }
//        PsiAnchor.StubIndexReference stubReference = PsiAnchor.createStubReference(element, containingFile);
//        if (stubReference != null) {
//          Uni.getLog().error("stubReference != null, stubReference: " + stubReference);
//        }
//      }
//      return new HardElementInfo2(element);
//    }

//    FileViewProvider viewProvider = containingFile.getViewProvider();
//    if (viewProvider instanceof FreeThreadedFileViewProvider && hasReliableRange(element, containingFile)) {
//      PsiLanguageInjectionHost hostContext = InjectedLanguageManager.getInstance(containingFile.getProject()).getInjectionHost(containingFile);
//      TextRange elementRange = element.getTextRange();
//      if (hostContext != null && elementRange != null) {
//        SmartPsiElementPointer<PsiLanguageInjectionHost> hostPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(hostContext);
//        Uni.getLog().error("removed InjectedSelfElementInfo2.java");
//      }
//    }

//    VirtualFile virtualFile = viewProvider.getVirtualFile();
    if (containingFile != null) {
      VirtualFile virtualFile = containingFile.getVirtualFile();
      if (element instanceof PsiFile) {
        FileViewProvider restored = PsiManager.getInstance(project).findViewProvider(virtualFile);
        return restored != null && restored.getPsi(LanguageUtil.getRootLanguage(element)) == element
          ? new FileElementInfo2((PsiFile)element)
          : new HardElementInfo2(element);
      }
    }
    return new HardElementInfo3(element);

//    if (!hasReliableRange(element, containingFile)) {
//      return new HardElementInfo2(element);
//    }
//
//    Document document = FileDocumentManager.getInstance().getCachedDocument(virtualFile);
//    if (document != null &&
//        ((PsiDocumentManagerBase)PsiDocumentManager.getInstance(project)).getSynchronizer().isDocumentAffectedByTransactions(document)) {
//      LOG.error("Smart pointers must not be created during PSI changes");
//    }
//
//    TextRange elementRange = element.getTextRange();
//    if (elementRange == null) {
//      return new HardElementInfo2(element);
//    }
//    Identikit.ByType identikit = Identikit.fromPsi(element, LanguageUtil.getRootLanguage(element));
//    if (elementRange.isEmpty() &&
//        identikit.findPsiElement(containingFile, elementRange.getStartOffset(), elementRange.getEndOffset()) != element) {
//      // PSI has empty range, no text, but complicated structure (e.g. PSI built on C-style macro expansions). It can't be reliably
//      // restored by just one offset in a file, so hold it on a hard reference
//      return new HardElementInfo2(element);
//    }
//
//    if (!containingFile.isPhysical() && document == null) {
//      // there's no document whose events could be tracked and used for restoration by offset
//      return new HardElementInfo2(element);
//    }
//
//    ProperTextRange proper = ProperTextRange.create(elementRange);
//    return new SelfElementInfo2(proper, identikit, containingFile, forInjected);
  }

  // check it's not some fake PSI that overrides getContainingFile/getTextRange/isPhysical/etc and confuses everyone
  private static boolean hasReliableRange(@NotNull PsiElement element, @NotNull PsiFile containingFile) {
    return (element instanceof ASTDelegatePsiElement || element instanceof ASTNode) && !isFakePsiInNormalFile(element, containingFile);
  }

  private static boolean isFakePsiInNormalFile(@NotNull PsiElement element, @NotNull PsiFile containingFile) {
    if (element.isPhysical()) return false;
    if (containingFile.isPhysical()) return true; // non-physical PSI in physical file, suspicious

    // in normal non-physical files there might also be fake PSI, so let's (expensively!) check we can find it by offset
    // hopefully in some future we'll prohibit such fake PSI
    TextRange range = element.getTextRange();
    return range == null ||
           PsiTreeUtil.findElementOfClassAtRange(containingFile, range.getStartOffset(), range.getEndOffset(), element.getClass()) != element;
  }

  @NotNull
  SmartPointerElementInfo2 getElementInfo() {
    return myElementInfo;
  }

  static boolean pointsToTheSameElementAs(@NotNull SmartPsiElementPointer pointer1, @NotNull SmartPsiElementPointer pointer2) {
    if (pointer1 == pointer2) return true;
    ProgressManager.checkCanceled();
    if (pointer1 instanceof SmartPsiElementPointerImpl2 && pointer2 instanceof SmartPsiElementPointerImpl2) {
      SmartPsiElementPointerImpl2 impl1 = (SmartPsiElementPointerImpl2)pointer1;
      SmartPsiElementPointerImpl2 impl2 = (SmartPsiElementPointerImpl2)pointer2;
      SmartPointerElementInfo2 elementInfo1 = impl1.getElementInfo();
      SmartPointerElementInfo2 elementInfo2 = impl2.getElementInfo();
      if (!elementInfo1.pointsToTheSameElementAs(elementInfo2, ((SmartPsiElementPointerImpl2)pointer1).myManager)) return false;
      PsiElement cachedElement1 = impl1.getCachedElement();
      PsiElement cachedElement2 = impl2.getCachedElement();
      return cachedElement1 == null || cachedElement2 == null || Comparing.equal(cachedElement1, cachedElement2);
    }
    return Comparing.equal(pointer1.getElement(), pointer2.getElement());
  }

  synchronized int incrementAndGetReferenceCount(int delta) {
    if (myReferenceCount == Byte.MAX_VALUE) return Byte.MAX_VALUE; // saturated
    if (myReferenceCount == 0) return -1; // disposed, not to be reused again
    return myReferenceCount += delta;
  }

  @Override
  public String toString() {
    return myElementInfo.toString();
  }
}
