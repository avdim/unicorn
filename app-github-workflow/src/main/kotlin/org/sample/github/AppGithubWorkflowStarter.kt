package org.sample.github

import com.sample.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.reduce
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.time.Month

const val TUTU_ORGANIZATION = "tutu-ru-mobile"
const val IOS_REPO = "ios-core"

suspend fun main() {
  println("hello AppGithubWorkflowStarter")
  println("token: ${BuildConfig.SECRET_GITHUB_TOKEN}")
  val client = HttpClient(Apache)

  val githubMail = client.getGithubMail(Token(BuildConfig.SECRET_GITHUB_TOKEN))
  println("githubMail: $githubMail")

  val sec = client.getGithubWorkflowRunsPagesUntil(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, IOS_REPO) {
//    it.createdTime > LocalDateTime(2021, Month.MARCH, 29, 0, 0).toInstant(TimeZone.UTC)
    it.createdTime > LocalDateTime(2021, Month.APRIL, 15, 0, 0).toInstant(TimeZone.UTC)
  }.map { run ->
    client.getGithubWorkflowRunJobs(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, IOS_REPO, run.id)
  }.mapNotNull {
    if (it is Response.Success) {
      it
    } else null
  }.map {
    it.data.calcDuration()
  }.reduce { acc, value ->
    println("reducer acc: ${acc.seconds}")
    acc + value
  }.seconds

  println("sec: $sec")
  println("min: ${sec / 60}")
  println("finish")
}
