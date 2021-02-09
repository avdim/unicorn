// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.*;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewSettings;
import com.intellij.ide.projectView.ViewSettings;

import com.intellij.ide.util.treeView.ValidateableNode;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.INativeFileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.pom.StatePreservingNavigatable;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.psi.impl.smartPointers.DebugBlackFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.AstLoadingFilter;
import com.intellij.util.PlatformIcons;
import com.unicorn.Uni;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Class for node descriptors based on PsiElements. Subclasses should define
 * method that extract PsiElement from Value.
 *
 * @param <Value> Value of node descriptor
 */
public abstract class AbstractPsiBasedNode2<Value> extends ProjectViewNode2<Value> implements ValidateableNode, StatePreservingNavigatable {
  private static final Logger LOG = Logger.getInstance(AbstractPsiBasedNode2.class.getName());

  protected AbstractPsiBasedNode2(@NotNull Value value) {
    super(value);
  }

  @Nullable
  protected abstract PsiElement extractPsiFromValue();

  @Nullable
  protected abstract Collection<AbstractTreeNod2<?>> getChildrenImpl();

  protected abstract void updateImpl(@NotNull PresentationData data);

  @Override
  @NotNull
  public final Collection<? extends AbstractTreeNod2<?>> getChildren() {
    return AstLoadingFilter.disallowTreeLoading(this::doGetChildren);
  }

  @NotNull
  private Collection<? extends AbstractTreeNod2<?>> doGetChildren() {
    final PsiElement psiElement = extractPsiFromValue();
    if (psiElement == null) {
      return new ArrayList<>();
    }
    if (!psiElement.isValid()) {
      LOG.error(new IllegalStateException("Node contains invalid PSI: "
        + "\n" + getClass() + " [" + this + "]"
        + "\n" + psiElement.getClass() + " [" + psiElement + "]"));
      return Collections.emptyList();
    }

    Collection<AbstractTreeNod2<?>> children = getChildrenImpl();
    return children != null ? children : Collections.emptyList();
  }

  @Override
  public boolean isValid() {
    final PsiElement psiElement = extractPsiFromValue();
    return psiElement != null && psiElement.isValid();
  }

  protected boolean isMarkReadOnly() {
    final AbstractTreeNod2<?> parent = getParent();
    if (parent == null) {
      return false;
    }
    if (parent instanceof AbstractPsiBasedNode2) {
      final PsiElement psiElement = ((AbstractPsiBasedNode2<?>) parent).extractPsiFromValue();
      return psiElement instanceof PsiDirectory;
    }

    final Object parentValue = parent.getValue();
    return parentValue instanceof PsiDirectory || parentValue instanceof Module;
  }

  protected static FileStatus computeFileStatus(@Nullable VirtualFile virtualFile) {
//  also look at FileStatusProvider and VcsFileStatusProvider
    if (virtualFile != null && virtualFile.getFileSystem() instanceof NonPhysicalFileSystem) {
      return FileStatus.SUPPRESSED;  // do not leak light files via cache
    }
    return FileStatus.NOT_CHANGED;
  }

  @Nullable
  private VirtualFile getVirtualFileForValue() {
    PsiElement psiElement = extractPsiFromValue();
    if (psiElement == null) {
      return null;
    }
    return PsiUtilCore.getVirtualFile(psiElement);
  }

  // Should be called in atomic action

  @Override
  public void update(@NotNull final PresentationData data) {
    AstLoadingFilter.disallowTreeLoading(() -> doUpdate(data));
  }

  private void doUpdate(@NotNull PresentationData data) {
    ApplicationManager.getApplication().runReadAction(() -> {
      if (!validate()) {
        return;
      }

      final PsiElement value = extractPsiFromValue();
      LOG.assertTrue(value.isValid());

      int flags = getIconableFlags();

      try {
        Icon icon = value.getIcon(flags);
        data.setIcon(icon);
      } catch (IndexNotReadyException ignored) {
      }
      data.setPresentableText(myName);
      updateImpl(data);
      data.setIcon(patchIcon(data.getIcon(true), getVirtualFile()));
    });
  }

  @Iconable.IconFlags
  protected int getIconableFlags() {
    int flags = 0;
    if (Uni.fileManagerConf2.isShowVisibilityIcons) {
      flags |= Iconable.ICON_FLAG_VISIBILITY;
    }
    if (isMarkReadOnly()) {
      flags |= Iconable.ICON_FLAG_READ_STATUS;
    }
    return flags;
  }

  @Nullable
  public static Icon patchIcon(@Nullable Icon original, @Nullable VirtualFile file) {
    if (file == null || original == null) return original;

    Icon icon = original;

    if (file.is(VFileProperty.SYMLINK)) {
      icon = LayeredIcon.create(icon, PlatformIcons.SYMLINK_ICON);
    }

    return icon;
  }

  @Nullable
  public NavigationItem getNavigationItem() {
    final PsiElement psiElement = extractPsiFromValue();
    return psiElement instanceof NavigationItem ? (NavigationItem) psiElement : null;
  }

  @Override
  public void navigate(boolean requestFocus, boolean preserveState) {
    if (canNavigate()) {
      if (requestFocus || preserveState) {
        openFileWithPsiElement(getVirtualFile(), extractPsiFromValue(), requestFocus, requestFocus);
      } else {
        getNavigationItem().navigate(false);
      }
    }
  }

  @Override
  public void navigate(boolean requestFocus) {
    navigate(requestFocus, false);
  }

  @Override
  public boolean canNavigate() {
    final NavigationItem item = getNavigationItem();
    return item != null && item.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    final NavigationItem item = getNavigationItem();
    return item != null && item.canNavigateToSource();
  }

  public boolean validate() {
    final PsiElement psiElement = extractPsiFromValue();
    DebugBlackFile.doDebug(getEqualityObject(), psiElement);
    if (psiElement == null || !psiElement.isValid()) {
      setValue(null);
    }

    return getValue() != null;
  }

  @NotNull
  public static TextRange getRangeToUnfoldOnNavigation(@NotNull Editor editor) {
    final int offset = editor.getCaretModel().getOffset();
    int line = editor.getDocument().getLineNumber(offset);
    int start = editor.getDocument().getLineStartOffset(line);
    int end = editor.getDocument().getLineEndOffset(line);
    return new TextRange(start, end);
  }

  protected static void unfoldCurrentLine(@NotNull final Editor editor) {
    final FoldRegion[] allRegions = editor.getFoldingModel().getAllFoldRegions();
    final TextRange range = getRangeToUnfoldOnNavigation(editor);
    editor.getFoldingModel().runBatchFoldingOperation(() -> {
      for (FoldRegion region : allRegions) {
        if (!region.isExpanded() && range.intersects(TextRange.create(region))) {
          region.setExpanded(true);
        }
      }
    });
  }

  private static boolean openFileWithPsiElement(VirtualFile file, PsiElement element, boolean searchForOpen, boolean requestFocus) {
    boolean openAsNative = false;
    if (element instanceof PsiFile) {
      VirtualFile virtualFile = ((PsiFile) element).getVirtualFile();
      if (virtualFile != null) {
        FileType type = virtualFile.getFileType();
        openAsNative = type instanceof INativeFileType || type instanceof UnknownFileType;
      }
    }

    if (searchForOpen) {
      element.putUserData(FileEditorManager.USE_CURRENT_WINDOW, null);
    } else {
      element.putUserData(FileEditorManager.USE_CURRENT_WINDOW, true);
    }

    Ref<Boolean> resultRef = new Ref<>();
    boolean openAsNativeFinal = openAsNative;
    // all navigation inside should be treated as a single operation, so that 'Back' action undoes it in one go
    if (false) {
      @NotNull Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
    }
    Project proj = Uni.todoUseOpenedProject(element.getProject());
    CommandProcessor.getInstance().executeCommand(proj, () -> {
      if (openAsNativeFinal || !activatePsiElementIfOpen(proj, element, searchForOpen, requestFocus)) {
        final NavigationItem navigationItem = (NavigationItem) element;
        if (!navigationItem.canNavigate()) {
          resultRef.set(Boolean.FALSE);
        } else {
          navigate2(proj, file, requestFocus);
//            navigationItem.navigate(requestFocus);
          resultRef.set(Boolean.TRUE);
        }
      }
    }, "", null);

    if (!resultRef.isNull()) return resultRef.get();

    element.putUserData(FileEditorManager.USE_CURRENT_WINDOW, null);
    return false;
  }

  public static void navigate2(Project project, @NotNull VirtualFile virtualFile, boolean requestFocus) {
    if (!virtualFile.isValid()) {
      throw new IllegalStateException("target not valid");
    }

    if (!virtualFile.isDirectory()) {
      if (navigateInEditorOrNativeApp(project, virtualFile, requestFocus)) return;
    }

    if (navigateInProjectView(project, virtualFile, requestFocus)) return;

    String message = IdeBundle.message("error.files.of.this.type.cannot.be.opened", ApplicationNamesInfo.getInstance().getProductName());
    Messages.showErrorDialog(project, message, IdeBundle.message("title.cannot.open.file"));
  }

  private static boolean navigateInEditorOrNativeApp(Project project, VirtualFile file, boolean requestFocus) {
    FileType type = FileTypeManager.getInstance().getKnownFileTypeOrAssociate(file, project);
    if (type == null || !file.isValid()) return false;

    if (type instanceof INativeFileType) {
      return ((INativeFileType) type).openFileInAssociatedApplication(project, file);
    }

    return navigateInEditor(project, file, requestFocus);
  }

  public static boolean navigateInEditor(Project project, VirtualFile file, boolean requestFocus) {
    return navigateInRequestedEditor(project, file) || navigateInAnyFileEditor(project, file, requestFocus);
  }

  protected static boolean navigateInAnyFileEditor(Project project, VirtualFile file, boolean focusEditor) {
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);

    FileEditor[] editors = fileEditorManager.openFile(file, focusEditor);
    for (FileEditor editor : editors) {
      if (editor instanceof TextEditor) {
        Editor e = ((TextEditor) editor).getEditor();
        fileEditorManager.runWhenLoaded(e, () -> {
          unfoldCurrentLine(e);
          if (focusEditor) {
            IdeFocusManager.getInstance(project).requestFocus(e.getContentComponent(), true);
            Window ancestor = SwingUtilities.getWindowAncestor(e.getContentComponent());
            if (ancestor != null) {
              ancestor.toFront();
            }
          }
        });
      }
    }
    return editors.length > 0;
  }

  private static boolean navigateInRequestedEditor(Project project, @NotNull VirtualFile file) {
    @SuppressWarnings("deprecation") DataContext ctx = DataManager.getInstance().getDataContext();
    Editor e = OpenFileDescriptor.NAVIGATE_IN_EDITOR.getData(ctx);
    if (e == null) return false;
    if (e.isDisposed()) {
      Logger.getInstance(OpenFileDescriptor.class).error("Disposed editor returned for NAVIGATE_IN_EDITOR from " + ctx);
      return false;
    }
    if (!Comparing.equal(FileDocumentManager.getInstance().getFile(e.getDocument()), file)) return false;

    navigateInEditor(project, e);
    return true;
  }

  protected static void navigateInEditor(Project project, @NotNull Editor e) {
    CaretModel caretModel = e.getCaretModel();
    boolean caretMoved = false;
    if (caretMoved) {
      e.getSelectionModel().removeSelection();
      FileEditorManager.getInstance(project).runWhenLoaded(e, () -> {
        e.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        unfoldCurrentLine(e);
      });
    }
  }

  private static boolean navigateInProjectView(@NotNull Project project, @NotNull VirtualFile file, boolean requestFocus) {
    SelectInContext context = new FileSelectInContext(project, file, null);
    for (SelectInTarget target : SelectInManager.getInstance(project).getTargetList()) {
      if (context.selectIn(target, requestFocus)) {
        return true;
      }
    }
    return false;
  }

  private static boolean activatePsiElementIfOpen(Project project, @NotNull PsiElement elt, boolean searchForOpen, boolean requestFocus) {
    if (!elt.isValid()) return false;
    elt = elt.getNavigationElement();
    final PsiFile file = elt.getContainingFile();
    if (file == null || !file.isValid()) return false;

    VirtualFile vFile = file.getVirtualFile();
    if (vFile == null) return false;

    if (!EditorHistoryManager.getInstance(project).hasBeenOpen(vFile)) return false;

    final FileEditorManager fem = FileEditorManager.getInstance(project);
    if (!fem.isFileOpen(vFile)) {
      fem.openFile(vFile, requestFocus, searchForOpen);
    }

    final TextRange range = elt.getTextRange();
    if (range == null) return false;

    final FileEditor[] editors = fem.getEditors(vFile);
    for (FileEditor editor : editors) {
      if (editor instanceof TextEditor) {
        final Editor text = ((TextEditor) editor).getEditor();
        final int offset = text.getCaretModel().getOffset();

        if (range.containsOffset(offset)) {
          // select the file
          fem.openFile(vFile, requestFocus, searchForOpen);
          return true;
        }
      }
    }

    return false;
  }

}
