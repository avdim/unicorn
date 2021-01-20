// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide;

import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ProjectViewNodeDecorator;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.ProjectExtensionPointName;
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Allows a plugin to modify the structure of a project as displayed in the project view.
 *
 * @see ProjectViewNodeDecorator
 */
public interface TreeStructureProvider2 {

  @Deprecated
  ExtensionPointName<TreeStructureProvider2> EP_NAME = ExtensionPointName.create("com.intellij.treeStructureProvider");

}
