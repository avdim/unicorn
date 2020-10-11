/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonReferenceExpression
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.JsonValue
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import ru.tutu.git.GitDir
import ru.tutu.git.branches
import ru.tutu.git.isGit
import ru.tutu.git.tags


class JsonRepoCompletion : CompletionContributor() {
  override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
    val position: LeafPsiElement = parameters.position as? LeafPsiElement ?: return
    position.parent
    parameters.originalFile//todo

    println("before parent, position: $position")
    val parent = position.parent
    println("after parent, parent: $parent")
    println("parent.parent: ${parent.parent}")
    if (parent is JsonStringLiteral || parent is JsonReferenceExpression) {
      val parentParent = parent.parent as? JsonProperty ?: return
      handleJsonProperty(position, parentParent, parameters, result)
    } else if (parent is JsonProperty) {
      handleJsonProperty(position, parent, parameters, result)
    }
//    val gitDirPath = position.parent.jsonGet("dir")?.strValue()
  }

  private fun handleJsonProperty(
    element: PsiElement,
    jsonProp: JsonProperty,
    parameters: CompletionParameters,
    result: CompletionResultSet
  ) {
    if (parameters.completionType != CompletionType.BASIC) return
    println("property.name: ${jsonProp.name}")
    val gitDirPath = jsonProp.parent.jsonGet("dir")?.strValue()
    if (gitDirPath != null) {
      val virtualFile: PsiDirectory? = parameters.originalFile.parent?.findSubdirectory(gitDirPath)
      val gitDir = virtualFile?.let { GitDir(it.toJavaFile()) }
      if (gitDir?.isGit() == true) {
        val whiteListTags = gitDir.tags()
        val whiteListBranches = gitDir.branches()
        val value: JsonValue? = jsonProp.value
        if (value != null && jsonProp.name == "ref") {
          result.addAllElements(
            (whiteListTags + whiteListBranches).map {
              LookupElementBuilder.create(it).bold()
            }
          )
        }
      }
    }
//    val presentNamePart: String = ThemeJsonUtil.getParentNames(property)
//    val shouldSurroundWithQuotes = !element.text.startsWith("\"")
//    val lookupElements: Iterable<LookupElement> = getLookupElements(presentNamePart, shouldSurroundWithQuotes)
//    result.addAllElements(lookupElements)
  }
}
