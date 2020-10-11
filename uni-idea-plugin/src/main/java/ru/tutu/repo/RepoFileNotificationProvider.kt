package ru.tutu.repo

import com.intellij.ide.actions.SynchronizeCurrentFileAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.intellij.ui.layout.panel
import org.sample.RepoJson
import org.sample.parseJsonToRepoJson
import org.sample.update
import ru.tutu.git.IdRsaSshConfig
import java.io.File

class RepoFileNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {

  override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
    if (file.name.contains("repo.json")) {
      val editorPanel = EditorNotificationPanel()
      editorPanel.add(panel {
        row {
          button("repo update") {
            val repoJson = parseJsonToRepoJson(String(file.inputStream.readBytes()))
            val path = file.parent.path
            println("path: $path")
            repoJson.update(File(path), IdRsaSshConfig())
            SynchronizeCurrentFileAction::class //todo обновить дерево файлов
          }
        }
      })
      return editorPanel
    } else {
      return null
    }
  }

  companion object {
    private val KEY = Key.create<EditorNotificationPanel>("repo.json.status")
  }

  override fun getKey(): Key<EditorNotificationPanel> = KEY
}
