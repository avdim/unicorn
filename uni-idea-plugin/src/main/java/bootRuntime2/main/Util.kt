package bootRuntime2.main

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.unicorn.Uni

fun getProjectOrDefault(e: AnActionEvent): Project {
     return e.project ?: Uni.todoDefaultProject
}
