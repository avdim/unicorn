// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.RootsProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2

/**
 * A node in the project view tree.
 */
abstract class ProjectViewNode2<T:Any> constructor(value: T) : AbstractTreeNod2<T>(value) {

}
