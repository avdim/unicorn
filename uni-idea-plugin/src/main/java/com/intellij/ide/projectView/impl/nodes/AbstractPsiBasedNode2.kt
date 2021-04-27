// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.ValidateableNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.StatePreservingNavigatable
import com.intellij.util.AstLoadingFilter
import com.intellij.util.IconUtil
import com.unicorn.Uni

/**
 * Class for node descriptors based on PsiElements. Subclasses should define
 * method that extract PsiElement from V.
 *
 * @param <V> V of node descriptor
</V> */
abstract class AbstractPsiBasedNode2<V : VirtualFile>(value: V) : AbstractTreeNod2<V>(value),
  StatePreservingNavigatable //todo redundant