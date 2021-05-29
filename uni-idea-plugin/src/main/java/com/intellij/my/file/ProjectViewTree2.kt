// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.my.file

import com.intellij.ui.DirtyUI
import com.intellij.ui.JBColor
import com.intellij.ui.popup.HintUpdateSupply
import com.unicorn.Uni
import java.awt.Color
import javax.swing.tree.TreeModel

open class ProjectViewTree2(model: TreeModel?) : DnDAwareTree2(null as TreeModel?/*TODO null*/) {

  init {
    isLargeModel = true
    setModel(model)
    HintUpdateSupply.installDataContextHintUpdateSupply(this)
  }

  override fun setToggleClickCount(count: Int) {
    if (count != 2) {
      Uni.log.error {
        "setToggleClickCount: unexpected count = $count"
      }
    }
    super.setToggleClickCount(count)
  }

  override fun isFileColorsEnabled(): Boolean {
    return true
  }

  @DirtyUI
  override fun getFileColorFor(): Color = JBColor.WHITE

}
