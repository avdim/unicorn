package ru.tutu.repo

import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.WeighedFileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class RepoJsonEditorProvider : WeighedFileEditorProvider() {
  companion object {
    private const val EDITOR_TYPE_ID = "repo-json-editor"
  }

  override fun getEditorTypeId() = EDITOR_TYPE_ID

  override fun accept(project: Project, file: VirtualFile): Boolean {
    return file.name.contains("repo.json")
  }

  override fun createEditor(project: Project, file: VirtualFile) = RepoFileEditor(project, file)

  override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}