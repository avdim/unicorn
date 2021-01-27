// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.smartPointers;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.reference.SoftReference;
import com.intellij.ui.tree.LeafState;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.lang.ref.Reference;
import java.util.Collection;
import java.util.Map;

public abstract class AbstractTreeNod2<T> extends PresentableNodeDescriptor2<AbstractTreeNod2<T>>
  implements NavigationItem, Queryable.Contributor, LeafState.Supplier {

  private static final TextAttributesKey FILESTATUS_ERRORS = TextAttributesKey.createTextAttributesKey("FILESTATUS_ERRORS");
  private static final Logger LOG = Logger.getInstance(AbstractTreeNod2.class);
  private AbstractTreeNod2<?> myParent;
  private Object myValue;
  private boolean myNullValueSet;
  private final boolean myNodeWrapper;
  public static final Object TREE_WRAPPER_VALUE = new Object();

  protected AbstractTreeNod2(Project project, @NotNull T value) {
    super(project, null);
    myNodeWrapper = setInternalValue(value);
  }

  @NotNull
  public abstract Collection<? extends AbstractTreeNod2<?>> getChildren();

  protected boolean hasProblemFileBeneath() {
    return false;
  }

  protected boolean valueIsCut() {
    return CopyPasteManager.getInstance().isCutElement(getValue());
  }

  @Override
  public PresentableNodeDescriptor2 getChildToHighlightAt(int index) {
    final Collection<? extends AbstractTreeNod2<?>> kids = getChildren();
    int i = 0;
    for (final AbstractTreeNod2<?> kid : kids) {
      if (i == index) return kid;
      i++;
    }

    return null;
  }

  @Override
  protected void postprocess(@NotNull PresentationData presentation) {
    if (hasProblemFileBeneath() ) {
      presentation.setAttributesKey(FILESTATUS_ERRORS);
    }

    setForcedForeground(presentation);
  }

  private void setForcedForeground(@NotNull PresentationData presentation) {
    final FileStatus status = getFileStatus();
    Color fgColor = getFileStatusColor(status);
    fgColor = fgColor == null ? status.getColor() : fgColor;

    if (valueIsCut()) {
      fgColor = CopyPasteManager.CUT_COLOR;
    }

    if (presentation.getForcedTextForeground() == null) {
      presentation.setForcedTextForeground(fgColor);
    }
  }

  @Override
  protected boolean shouldUpdateData() {
    return !myProject.isDisposed() && getEqualityObject() != null;
  }

  @NotNull
  @Override
  public LeafState getLeafState() {
    if (isAlwaysShowPlus()) return LeafState.NEVER;
    if (isAlwaysLeaf()) return LeafState.ALWAYS;
    return LeafState.DEFAULT;
  }

  public boolean isAlwaysShowPlus() {
    return false;
  }

  public boolean isAlwaysLeaf() {
    return false;
  }

  public boolean isAlwaysExpand() {
    return false;
  }

  @Override
  @Nullable
  public final AbstractTreeNod2<T> getElement() {
    return getEqualityObject() != null ? this : null;
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) return true;
    if (object == null || !object.getClass().equals(getClass())) return false;
    // we should not change this behaviour if value is set to null
    return Comparing.equal(myValue, ((AbstractTreeNod2<?>)object).myValue);
  }

  @Override
  public int hashCode() {
    // we should not change hash code if value is set to null
    Object value = myValue;
    return value == null ? 0 : value.hashCode();
  }

  public final AbstractTreeNod2 getParent() {
    return myParent;
  }

  public final void setParent(AbstractTreeNod2 parent) {
    myParent = parent;
  }

  @Override
  public final NodeDescriptor getParentDescriptor() {
    return myParent;
  }

  public final T getValue() {
    Object value = getEqualityObject();
    return value == null ? null : (T) retrieveElement(value);
  }

  @Nullable
  public static Object retrieveElement(@NotNull final Object pointer) {
    if (pointer instanceof SmartPsiElementPointer) {
      if(!(pointer instanceof SmartPsiElementPointerImpl2)) {
        Uni.getLog().error("!(pointer instanceof SmartPsiElementPointerImpl2)");
      }
      return ReadAction.compute(() -> ((SmartPsiElementPointerImpl2<?>)pointer).getElement());//по факту тут тотлько SmartPsiElementPointerImpl2
    }
    return pointer;
  }

  public final void setValue(T value) {
    boolean debug = !myNodeWrapper && LOG.isDebugEnabled();
    int hash = !debug ? 0 : hashCode();
    myNullValueSet = value == null || setInternalValue(value);
    if (debug && hash != hashCode()) {
      LOG.warn("hash code changed: " + myValue);
    }
  }

  /**
   * Stores the anchor to new value if it is not {@code null}
   *
   * @param value a new value to set
   * @return {@code true} if the specified value is {@code null} and the anchor is not changed
   */
  private boolean setInternalValue(@NotNull T value) {
    if (value == TREE_WRAPPER_VALUE) return true;

    myValue = createAnchor(value);
    return false;
  }

  public Object createAnchor(@NotNull Object element) {
    if (element instanceof PsiElement) {
      PsiElement psi = (PsiElement)element;
      return ReadAction.compute(() -> {
        if (!psi.isValid()) return psi;
        SmartPsiElementPointer<PsiElement> psiResult;
        SmartPsiElementPointer<PsiElement> oldPsiResult = SmartPointerManager.getInstance(psi.getProject()).createSmartPsiElementPointer(psi);
        SmartPsiElementPointer<PsiElement> newPsiResult = createSmartPsiElementPointer(psi);
        if (true) {
          psiResult = newPsiResult;
        } else {
          psiResult = oldPsiResult;
        }

        return psiResult;
      });
    }
    return element;
  }

  @NotNull
  public static <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer(@NotNull E element) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    PsiFile containingFile = element.getContainingFile();
    return createSmartPsiElementPointer(element, containingFile);
  }

  @NotNull
  public static <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer(@NotNull E element, PsiFile containingFile) {
    return createSmartPsiElementPointer(element, containingFile, false);
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

  @NotNull
  public static <E extends PsiElement> SmartPsiElementPointer<E> createSmartPsiElementPointer(@NotNull E element,
                                                                                       PsiFile containingFile,
                                                                                       boolean forInjected) {
    ensureValid(element, containingFile);
//    SmartPointerTracker.processQueue();
//    ensureMyProject(containingFile != null ? containingFile.getProject() : element.getProject());
    SmartPsiElementPointerImpl2<E> pointer = getCachedPointer(element);
    if (pointer != null &&
      (!(pointer.getElementInfo() instanceof SelfElementInfo2) || ((SelfElementInfo2)pointer.getElementInfo()).isForInjected() == forInjected) &&
      pointer.incrementAndGetReferenceCount(1) > 0) {
      return pointer;
    }

    pointer = new SmartPsiElementPointerImpl2<E>((SmartPointerManagerImpl) SmartPointerManager.getInstance(ProjectManager.getInstance().getDefaultProject() ), element, containingFile, forInjected);
    if (containingFile != null) {
      trackPointer();
    }
    element.putUserData(CACHED_SMART_POINTER_KEY, new SoftReference<>(pointer));
    return pointer;
  }
  private static final Key<Reference<SmartPsiElementPointerImpl2<?>>> CACHED_SMART_POINTER_KEY = Key.create("CACHED_SMART_POINTER_KEY_2");
  private static void ensureValid(@NotNull PsiElement element, @Nullable PsiFile containingFile) {
    boolean valid = containingFile != null ? containingFile.isValid() : element.isValid();
    if (!valid) {
      PsiUtilCore.ensureValid(element);
      if (containingFile != null && !containingFile.isValid()) {
        throw new PsiInvalidElementAccessException(containingFile, "Element " + element.getClass() + "(" + element.getLanguage() + ")" + " claims to be valid but returns invalid containing file ");
      }
    }
  }

  private static <E extends PsiElement> void trackPointer() {
//    Uni.getLog().error("should not use: trackPointer(pointer= " + pointer +  ", containingFile: " + containingFile);
//    SmartPsiElementPointerImpl2 info = pointer.getElementInfo();
//    if (!(info instanceof SelfElementInfo)) return;
//
//    SmartPointerTracker tracker = getTracker(containingFile);
//    if (tracker == null) {
//      tracker = getOrCreateTracker(containingFile);
//    }
//    tracker.addReference(pointer);
  }

  public final Object getEqualityObject() {
    return myNullValueSet ? null : myValue;
  }

  @Override
  public void apply(@NotNull Map<String, String> info) {
  }

  public Color getFileStatusColor(final FileStatus status) {
    if (FileStatus.NOT_CHANGED.equals(status) && myProject != null && !myProject.isDefault()) {
      final VirtualFile vf = getVirtualFile();
      if (vf != null && vf.isDirectory()) {
        return FileStatusManager.getInstance(myProject).getRecursiveStatus(vf).getColor();
      }
    }
    return status.getColor();
  }

  protected VirtualFile getVirtualFile() {
    return null;
  }

  public FileStatus getFileStatus() {
    return FileStatus.NOT_CHANGED;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public void navigate(boolean requestFocus) {
  }

  @Override
  public boolean canNavigate() {
    return false;
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }

  @Nullable
  protected final Object getParentValue() {
    AbstractTreeNod2<?> parent = getParent();
    return parent == null ? null : parent.getValue();
  }


  public boolean canRepresent(final Object element) {
    return Comparing.equal(getValue(), element);
  }

  /**
   * @deprecated use {@link #getPresentation()} instead
   */
  @Deprecated
  protected String getToolTip() {
    return getPresentation().getTooltip();
  }
}
