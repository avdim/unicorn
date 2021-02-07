// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.smartPointers;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.reference.SoftReference;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.unicorn.Uni;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentMap;

public final class SmartPointerManagerImpl2 extends SmartPointerManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(SmartPointerManagerImpl2.class);
  private final Project myProject;
  private final PsiDocumentManagerBase myPsiDocManager;
  private final Key<WeakReference<SmartPointerTracker2>> LIGHT_TRACKER_KEY;
  private final ConcurrentMap<VirtualFile, SmartPointerTracker2> myPhysicalTrackers = ContainerUtil.createConcurrentWeakValueMap();

  synchronized public static SmartPointerManagerImpl2 getInstance(Project project) {
    Uni.getLog().todo("cache by project");
    return new SmartPointerManagerImpl2(project);
  }

  public SmartPointerManagerImpl2(@NotNull Project project) {
    myProject = project;
    myPsiDocManager = (PsiDocumentManagerBase)PsiDocumentManager.getInstance(project);
    LIGHT_TRACKER_KEY = Key.create("SMART_POINTERS " + (project.isDefault() ? "default" : project.hashCode()));
  }

  @Override
  public void dispose() {
    SmartPointerTracker2.processQueue();
  }

  @NotNull
  private static @NonNls String anonymize(@NotNull Project project) {
    return
      (project.isDisposed() ? "(Disposed)" : "") +
      (project.isDefault() ? "(Default)" : "") +
      project.hashCode();
  }

  private static final Key<Reference<SmartPsiElementPointerImpl2<?>>> CACHED_SMART_POINTER_KEY = Key.create("CACHED_SMART_POINTER_KEY_2");
  @Override
  @NotNull
  public <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer(@NotNull E element) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    PsiFile containingFile = element.getContainingFile();
    return createSmartPsiElementPointer(element, containingFile);
  }
  @Override
  @NotNull
  public <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer(@NotNull E element, PsiFile containingFile) {
    return createSmartPsiElementPointer(element, containingFile, false);
  }

  @NotNull
  public <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer(@NotNull E element,
                                                                                       PsiFile containingFile,
                                                                                       boolean forInjected) {
    ensureValid(element, containingFile);
    SmartPointerTracker2.processQueue();
    ensureMyProject(containingFile != null ? containingFile.getProject() : element.getProject());
    SmartPsiElementPointerImpl2<E> pointer = getCachedPointer(element);
    if (pointer != null &&
        (!(pointer.getElementInfo() instanceof SelfElementInfo2) || ((SelfElementInfo2)pointer.getElementInfo()).isForInjected() == forInjected) &&
        pointer.incrementAndGetReferenceCount(1) > 0) {
      return pointer;
    }

    pointer = new SmartPsiElementPointerImpl2<>(this, element, containingFile, forInjected);
    if (containingFile != null) {
      trackPointer(pointer, containingFile.getViewProvider().getVirtualFile());
    }
    element.putUserData(CACHED_SMART_POINTER_KEY, new SoftReference<>(pointer));
    return pointer;
  }

  private void ensureMyProject(@NotNull Project project) {
    if (project != myProject) {
      throw new IllegalArgumentException("Element from alien project: "+anonymize(project)+" expected: "+anonymize(myProject));
    }
  }

  private static void ensureValid(@NotNull PsiElement element, @Nullable PsiFile containingFile) {
    boolean valid = containingFile != null ? containingFile.isValid() : element.isValid();
    if (!valid) {
      PsiUtilCore.ensureValid(element);
      if (containingFile != null && !containingFile.isValid()) {
        throw new PsiInvalidElementAccessException(containingFile, "Element " + element.getClass() + "(" + element.getLanguage() + ")" + " claims to be valid but returns invalid containing file ");
      }
    }
  }

  private static <E extends PsiElement> SmartPsiElementPointerImpl2<E> getCachedPointer(@NotNull E element) {
    Reference<SmartPsiElementPointerImpl2<?>> data = element.getUserData(CACHED_SMART_POINTER_KEY);
    SmartPsiElementPointerImpl2<?> cachedPointer = SoftReference.dereference(data);
    if (cachedPointer != null) {
      PsiElement cachedElement = cachedPointer.getElement();
      if (cachedElement != element) {
        return null;
      }
    }
    //noinspection unchecked
    return (SmartPsiElementPointerImpl2<E>)cachedPointer;
  }

  @Override
  @NotNull
  public SmartPsiFileRange createSmartPsiFileRangePointer(@NotNull PsiFile file, @NotNull TextRange range) {
    return createSmartPsiFileRangePointer(file, range, false);
  }

  @NotNull
  public SmartPsiFileRange createSmartPsiFileRangePointer(@NotNull PsiFile file,
                                                          @NotNull TextRange range,
                                                          boolean forInjected) {
    Uni.getLog().error("createSmartPsiFileRangePointer not implemented");
    throw new UnsupportedOperationException("createSmartPsiFileRangePointer");
//    PsiUtilCore.ensureValid(file);
//    SmartPointerTracker2.processQueue();
//    SmartPsiFileRangePointerImpl2 pointer = new SmartPsiFileRangePointerImpl2(this, file, ProperTextRange.create(range), forInjected);
//    trackPointer(pointer, file.getViewProvider().getVirtualFile());
//    return pointer;
  }

  private <E extends PsiElement> void trackPointer(@NotNull SmartPsiElementPointerImpl2<E> pointer, @NotNull VirtualFile containingFile) {
    SmartPointerElementInfo2 info = pointer.getElementInfo();
    if (!(info instanceof SelfElementInfo2)) return;

    SmartPointerTracker2 tracker = getTracker(containingFile);
    if (tracker == null) {
      tracker = getOrCreateTracker(containingFile);
    }
    Uni.getLog().todo("pointer: " + pointer + ", tracker: " + tracker + ", containingFile: " + containingFile);
//    tracker.addReference(pointer);
  }

  @Override
  public void removePointer(@NotNull SmartPsiElementPointer<?> pointer) {
    if (!(pointer instanceof SmartPsiElementPointerImpl) || myProject.isDisposed()) {
      return;
    }
    ensureMyProject(pointer.getProject());
    int refCount = ((SmartPsiElementPointerImpl<?>)pointer).incrementAndGetReferenceCount(-1);
    if (refCount == -1) {
      LOG.error("Double smart pointer removal");
      return;
    }

    if (refCount == 0) {
      PsiElement element = ((SmartPointerEx<?>)pointer).getCachedElement();
      if (element != null) {
        element.putUserData(CACHED_SMART_POINTER_KEY, null);
      }

      SmartPointerElementInfo info = ((SmartPsiElementPointerImpl<?>)pointer).getElementInfo();
      info.cleanup();
    }
  }

  @Nullable
  SmartPointerTracker2 getTracker(@NotNull VirtualFile file) {
    return file instanceof LightVirtualFile ? SoftReference.dereference(file.getUserData(LIGHT_TRACKER_KEY)) : myPhysicalTrackers.get(file);
  }

  @NotNull
  private SmartPointerTracker2 getOrCreateTracker(@NotNull VirtualFile file) {
    synchronized (myPhysicalTrackers) {
      SmartPointerTracker2 tracker = getTracker(file);
      if (tracker == null) {
        tracker = new SmartPointerTracker2();
        if (file instanceof LightVirtualFile) {
          file.putUserData(LIGHT_TRACKER_KEY, new WeakReference<>(tracker));
        } else {
          myPhysicalTrackers.put(file, tracker);
        }
      }
      return tracker;
    }
  }

  @Override
  public boolean pointToTheSameElement(@NotNull SmartPsiElementPointer<?> pointer1, @NotNull SmartPsiElementPointer<?> pointer2) {
    return SmartPsiElementPointerImpl.pointsToTheSameElementAs(pointer1, pointer2);
  }

  @NotNull
  Project getProject() {
    return myProject;
  }

  @NotNull
  PsiDocumentManagerBase getPsiDocumentManager() {
    return myPsiDocManager;
  }
}
