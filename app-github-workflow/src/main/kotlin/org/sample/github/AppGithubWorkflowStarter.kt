package org.sample.github

import com.sample.Token
import com.sample.getGithubMail
import io.ktor.client.*
import io.ktor.client.engine.apache.*

suspend fun main() {
  println("hello AppGithubWorkflowStarter")
  println("token: ${BuildConfig.SECRET_GITHUB_TOKEN}")
  val client = HttpClient(Apache)

  val githubMail = client.getGithubMail(Token(BuildConfig.SECRET_GITHUB_TOKEN))
  println("githubMail: $githubMail")
}
