// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.my.file;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author ven
 * */

public abstract class ProjectTreeStructure2 extends AbstractProjectTreeStructure2 {

  public ProjectTreeStructure2(@NotNull Project project) {
    super(project);
  }
}