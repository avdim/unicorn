package ru.tutu.idea.file

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Alarm
import javax.swing.JTree

fun JTree.addSelectionListener(listener: (VirtualFile) -> Unit) {
  val myAutoScrollAlarm = Alarm()
  addTreeSelectionListener {
    if (isShowing) {
      myAutoScrollAlarm.cancelAllRequests()
      myAutoScrollAlarm.addRequest(
        {
          if (isShowing) { //for tests
            if (hasFocus()) {
              ApplicationManager.getApplication().invokeLater {
                val vFile = CommonDataKeys.VIRTUAL_FILE.getData(DataManager.getInstance().getDataContext(this))
                if (vFile != null) {
                  listener(vFile)
                }
              }
            }
          }
        },
        200
      )
    }
  }
}
