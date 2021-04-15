package org.sample.github

import com.sample.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*

const val TUTU_ORGANIZATION = "tutu-ru-mobile"
const val IOS_REPO = "ios-core"

suspend fun main() {
  println("hello AppGithubWorkflowStarter")
  println("token: ${BuildConfig.SECRET_GITHUB_TOKEN}")
  val client = HttpClient(Apache)

  val githubMail = client.getGithubMail(Token(BuildConfig.SECRET_GITHUB_TOKEN))
  println("githubMail: $githubMail")
  val githubWorkflowRuns = client.getGithubWorkflowRuns(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, IOS_REPO)
  if (githubWorkflowRuns is Response.Success) {
    val runId = githubWorkflowRuns.data.workflowRuns[0].id
    val result = client.getGithubWorkflowRunJobs(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, IOS_REPO, runId)
    println("result: $result")
  }
}
