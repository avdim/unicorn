package com.github

data class GithubProject(
  val user: String,
  val repo: String,
)

fun parseGithubUrl(uri: String): GithubProject {
  val isSsh = uri.startsWith("git@")
  try {
    if (isSsh) {
      val pathArgs = uri.split(":")[1].split("/")
      val user = pathArgs[0]
      val repo = pathArgs[1].removeSuffix(".git")
      return GithubProject(user, repo)
    } else {
      val pathArgs = uri.split("/").takeLast(2)
      val user = pathArgs[0]
      val repo = pathArgs[1].removeSuffix(".git")
      return GithubProject(user, repo)
    }
  } catch (t: Throwable) {
    println("fail parse parseGithubUrl")
    throw t
  }
}

fun GithubProject.toSshUrl(): String {
  return "git@github.com:$user/$repo.git"
}

fun GithubProject.toHttpsUrl(): String {
  return "https://github.com/$user/$repo.git"
}

