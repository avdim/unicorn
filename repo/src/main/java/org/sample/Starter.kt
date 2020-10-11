package org.sample

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import ru.tutu.git.*
import java.io.File

suspend fun main(args: Array<String>) {
  val jsonPath: String? = args.getOrNull(0) ?: "repo.json"
  println("repo tool")
  if (jsonPath != null) {
    val file = File(jsonPath)
    if (file.exists().not()) {
      file.createNewFile()
    }
    val repoObj: RepoJson = parseJsonToRepoJson(file.readText())
    println("file: ${file.absolutePath}")
    repoObj.update(file.absoluteFile.parentFile, SshAgentConfig())
//    println(repoObj)
  }
}

fun parseJsonToRepoJson(jsonStr: String): RepoJson =
  Json.decodeFromString(RepoJson.serializer(), jsonStr)

@Serializable
data class RepoJson(
  val remote: String = "origin",
  val dirs: List<RepoDir>
)

@Serializable
data class RepoDir(
  val dir: String,
  val gitUri: String,
  val ref: String
)
