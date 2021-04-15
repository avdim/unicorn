package org.sample.github

import com.sample.Token
import com.sample.getGithubMail
import com.sample.getGithubWorkflowRuns
import io.ktor.client.*
import io.ktor.client.engine.apache.*

const val TUTU_ORGANIZATION = "tutu-ru-mobile"

suspend fun main() {
  println("hello AppGithubWorkflowStarter")
  println("token: ${BuildConfig.SECRET_GITHUB_TOKEN}")
  val client = HttpClient(Apache)

  val githubMail = client.getGithubMail(Token(BuildConfig.SECRET_GITHUB_TOKEN))
  println("githubMail: $githubMail")
  client.getGithubWorkflowRuns(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, "ios-core")
}
