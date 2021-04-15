package org.sample.github

import com.sample.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

const val TUTU_ORGANIZATION = "tutu-ru-mobile"
const val IOS_REPO = "ios-core"

suspend fun main() {
  println("hello AppGithubWorkflowStarter")
  println("token: ${BuildConfig.SECRET_GITHUB_TOKEN}")
  val client = HttpClient(Apache)

  val githubMail = client.getGithubMail(Token(BuildConfig.SECRET_GITHUB_TOKEN))
  println("githubMail: $githubMail")
  val githubWorkflowRuns = client.getGithubWorkflowRuns(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, IOS_REPO)
  val totalDuration = githubWorkflowRuns.mapIfSuccess {
    val jobs = coroutineScope {
      it.workflowRuns.map { run ->
        async {
          client.getGithubWorkflowRunJobs(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, IOS_REPO, run.id)
        }
      }.awaitAll()
    }
    jobs.mapNotNull { if (it is Response.Success) it else null }
      .map { it.data.calcDuration() }
      .sum()
  }
  if (totalDuration is Response.Success) {
    val sec = totalDuration.data.seconds
    println("sec: $sec")
    println("min: ${sec / 60}")
    println("finish")
  }

}
