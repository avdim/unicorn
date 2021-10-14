package ru.tutu.repo

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
//import com.intellij.testFramework.TestActionEvent
import com.unicorn.git4idea2.branch.GitBranchPopup
import com.unicorn.plugin.ui.IPopupAction
import com.unicorn.plugin.ui.chooseActionInPopup
import com.unicorn.plugin.ui.showPanelDialog
import com.unicorn.git4idea2.branch.GitBranchUtil
import git4idea.repo.GitRepository
import java.awt.event.MouseEvent

class RepoGutter : LineMarkerProvider {
  override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
    if (!element.containingFile.name.contains("repo.json")) {
      return null
    }

//    if (element.containingFile.language is JsonLanguage) {
//      if (element is JsonProperty) {
//        if (element.name == "dir") {
////          return NavigationGutterIconBuilder.create(AllIcons.General.Note)
//          return LineMarkerInfo(
//            element,
//            element.getTextRange(),
//            AllIcons.General.Information,
//            null,
//            { e: MouseEvent, elt: JsonProperty ->
//              val dir: PsiDirectory? = element.containingFile.parent?.findSubdirectory(element.strValue())
//              if (dir != null) {
//                val vFile = dir.virtualFile
//                chooseActionInPopup(
//                  TestActionEvent(),
//                  "dir ${vFile.name}",
//                  listOf(
//                    object : IPopupAction {
//                      override fun execute(e: AnActionEvent) {
//                        ProjectViewSelectInTarget.select(element.project, vFile, ProjectViewPane.ID, null, vFile, true)
//                      }
//
//                      override val text: String = "select in project view"
//                    },
//                    object : IPopupAction {
//                      override fun execute(e: AnActionEvent) {
////                        ActionManager.getInstance().getAction("Git.Menu").perform()
//                        val project: Project = element.project
//                        val file: VirtualFile? = dir.virtualFile//e.getData(CommonDataKeys.VIRTUAL_FILE)
//                        val repository: GitRepository? =
//                          GitBranchUtil.getRepositoryOrGuess(project, file)
//
//                        if (repository != null) {
//                          GitBranchPopup.getInstance(project, repository, e.getDataContext()).asListPopup().showCenteredInCurrentWindow(project)
//                        }
//                      }
//
//                      override val text: String = "git"
//                    }
//                  ),
//                  e
//                )
//              }
//              if (false) showPanelDialog {
//                row {
//                  label("hello gutter")
//                  label("${element.name}: ${element.strValue()}")
//                  label("isDirectory: ${dir?.isDirectory}")
//
//                }
//              }
//            },
//            GutterIconRenderer.Alignment.LEFT
//          )
//        } else if (element.name == "gitUri") {
//          return LineMarkerInfo(
//            element,
//            element.getTextRange(),
//            AllIcons.Vcs.Vendors.Github,
//            null,
//            { e: MouseEvent, elt: JsonProperty ->
//              chooseActionInPopup(
//                TestActionEvent(),
//                "choose action",
//                listOf(
//                  object : IPopupAction {
//                    override fun execute(e: AnActionEvent) {
//                      val gitHubUrl = "https://" + element.strValue()
//                        .replace("git@", "")
//                        .replace(":", "/")
//                        .replace(".git", "")
//                      BrowserUtil.browse(gitHubUrl)
//                    }
//                    override val text: String = "open in browser"
//                  }
//                ),
//                e
//              )
//            },
//            GutterIconRenderer.Alignment.LEFT
//          )
//
//        }
//      }
//    }

    return null
  }


}

fun JsonProperty.strValue() = value?.text?.replace("\"", "") ?: ""
