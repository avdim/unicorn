// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.smartPointers.AbstractTreeNod2
import com.intellij.util.PlatformIcons
import com.unicorn.Uni.todoDefaultProject

abstract class AbstractProjectNode2 protected constructor() : AbstractTreeNod2<Project>(todoDefaultProject) {

  public override fun update(presentation: PresentationData) {
        presentation.setIcon(PlatformIcons.PROJECT_ICON)
        presentation.presentableText = "todo_presentable_text"
    }

    /**
     * Returns the virtual file represented by this node or one of its children.
     *
     * @return the virtual file instance, or null if the project view node doesn't represent a virtual file.
     */
    public override fun getVirtualFile(): VirtualFile? {
        return null
    }
}