/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.tutu.repo

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.json.psi.JsonElementVisitor
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonValue
import com.intellij.json.psi.impl.JsonStringLiteralImpl
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.unicorn.plugin.showPanelDialog
import org.jdesktop.swingx.combobox.ListComboBoxModel
import ru.tutu.git.*
import java.io.File

private val REF_WHITE_LIST = listOf("tag1", "tag2").map { QuickFixRefElement(it) }

class QuickFixRefElement(val str: String)

class JsonRepoInspection : LocalInspectionTool() {
  override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {

    return object : JsonElementVisitor() {
      override fun visitObject(o: JsonObject) {
        if (!o.containingFile.name.contains("repo.json")) {
          return
        }
        o.propertyList.forEach { jsonProp: JsonProperty ->
          val gitDirPath = jsonProp.parent.jsonGet("dir")?.strValue()
          if (gitDirPath != null) {
            val virtualFile: PsiDirectory? = o.containingFile.parent?.findSubdirectory(gitDirPath)
            val gitDir = virtualFile?.let { GitDir(it.toJavaFile()) }
            if (gitDir?.isGit() == true) {
              val whiteListTags = gitDir.tags()
              val whiteListBranches = gitDir.branches()
              val value: JsonValue? = jsonProp.value
              if (value != null && jsonProp.name == "ref") {
                val currentRefValue: String = value.text.replace("\"", "")
                if ((currentRefValue in (whiteListTags + whiteListBranches)).not()) {
                  holder.registerProblem(
                    value,
                    "not found branch or tag",
                    ProblemHighlightType.ERROR,
                    object : LocalQuickFixAndIntentionActionOnPsiElement(value) {
                      override fun getFamilyName() = "quick fix family name"
                      override fun getText() = "choose"
                      override fun invoke(
                        project: Project,
                        file: PsiFile,
                        editor: Editor?,
                        startElement: PsiElement,
                        endElement: PsiElement
                      ) {
                        var resultValue: String = currentRefValue
                        showPanelDialog {
                          row {
                            label("tags:")
                            comboBox(
                              ListComboBoxModel<String>(whiteListTags),
                              {
                                null
                              },
                              {
                                resultValue = it.toString()
                              }
                            )
                          }
                          row {
                            label("branches:")
                            comboBox(
                              ListComboBoxModel<String>(whiteListBranches),
                              {
                                null
                              },
                              {
                                resultValue = it.toString()
                              }
                            )
                          }
                          if(false) {
                            for (tag in whiteListTags) {
                              row {
                                button("use tag: $tag") {
                                  resultValue = tag
                                }
                              }
                            }
                            for (branch in whiteListBranches) {
                              row {
                                button("use branch: $branch") {
                                  resultValue = branch
                                }
                              }
                            }
                          }
                        }

                        if (value is JsonStringLiteralImpl) {
                          value.updateText("\"${resultValue}\"")
                        }

                      }
                    },
                    object : LocalQuickFixAndIntentionActionOnPsiElement(value) {
                      override fun getFamilyName(): String = "quick fix family name"
                      override fun getText(): String = "create tag '$currentRefValue'"
                      override fun invoke(
                        project: Project,
                        file: PsiFile,
                        editor: Editor?,
                        startElement: PsiElement,
                        endElement: PsiElement
                      ) {
                        gitDir.createTag(currentRefValue)
                        EditorFactory.getInstance().refreshAllEditors()
                        if (value is JsonStringLiteralImpl) {
                          value.updateText("\"${currentRefValue}\"")
                        }
                      }
                    },
                    object : LocalQuickFixAndIntentionActionOnPsiElement(value) {
                      override fun getFamilyName(): String = "quick fix family name"
                      override fun getText(): String = "create branch '$currentRefValue'"
                      override fun invoke(
                        project: Project,
                        file: PsiFile,
                        editor: Editor?,
                        startElement: PsiElement,
                        endElement: PsiElement
                      ) {
                        //todo create branch
                      }
                    }
                  )
                }
              }
            } else {
              //todo not a git dir
            }
          }
        }
      }
    }
  }

}

fun PsiElement.jsonGet(key: String): JsonProperty? = children.mapNotNull { it as? JsonProperty }.firstOrNull { it.name == key }

fun PsiDirectory.toJavaFile() = File(toString().replace("PsiDirectory:", ""))
