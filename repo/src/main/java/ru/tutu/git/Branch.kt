package ru.tutu.git

import org.eclipse.jgit.lib.Ref

class Branch(internal val jGitRef: Ref) {
    val name = jGitRef.name // name like in command: git branch --all, for example: develop, or remotes/origin/develop
            .removePrefix("refs/heads/")
            .removePrefix("refs/")

    override fun toString() = "Branch: $name"
}

val Branch.remote get():Boolean = this.name.startsWith("remotes/")

data class JiraTask(
        val project: String,
        val taskId: Int
) {
    override fun toString(): String = "$project-$taskId"
}

fun extractJiraTask(branchName: String): JiraTask? =
        branchName                   // "remotes/origin/feature/UMA-123_Description"
                .split("/").last()   // "UMA-123_Description"
                .split("_").first()  // "UMA-123"
                .split("-")          // ["UMA", "123"]
                .let {
                    if (it.size != 2) {
                        null
                    } else {
                        val project = it[0]               // "UMA"
                        val taskId = it[1].toIntOrNull()  // 123
                        if (taskId == null) {
                            null
                        } else {
                            JiraTask(project, taskId)
                        }
                    }
                }
