package org.sample.github

import com.sample.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import java.nio.file.Files
import java.time.Month
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText

const val TUTU_ORGANIZATION = "tutu-ru-mobile"
const val IOS_REPO = "ios-core"

@OptIn(ExperimentalPathApi::class)
suspend fun main() {
  println("hello AppGithubWorkflowStarter")
  println("token: ${BuildConfig.SECRET_GITHUB_TOKEN}")
  val client = HttpClient(Apache)

  val githubMail = client.getGithubMail(Token(BuildConfig.SECRET_GITHUB_TOKEN))
  println("githubMail: $githubMail")

  val jobs = client.getGithubWorkflowRunsPagesUntil(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, IOS_REPO) {
//    it.createdTime > LocalDateTime(2021, Month.MARCH, 29, 0, 0).toInstant(TimeZone.UTC)
    it.createdTime > LocalDateTime(2021, Month.MARCH, 29, 0, 0).toInstant(TimeZone.UTC)
  }.flatMapMerge { run ->
    flow {
      emit(
        client.getGithubWorkflowRunJobs(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, IOS_REPO, run.id)
      )
    }
  }.mapNotNull {
    if (it is Response.Success) it else null
  }.completeToList()
    .flatMap {
      it.data.jobs
    }

  val htmlFile = Files.createTempFile("github_time", ".html")
  println("file://${htmlFile.absolutePathString()}")
  htmlFile
    .writeText(
      buildString {
        appendHTML().html {
          head {
            style {
              unsafe {
                raw(
                  """
                    table, th, td {
                      border: 1px solid black;
                    }
                  """.trimIndent()
                )
              }
            }
          }
          body {
            table {
              jobs.forEach { job ->
                tr {
                  td {
                    val sec = job.calcDuration().seconds
                    +"$sec"
                  }
                  td {
                    val start = job.startedAt
                    +"$start"
                  }
                  td {
                    a(job.htmlUrl) {
                      target = ATarget.blank
                      +"logs"
                    }
                  }
                }
              }
            }
          }
        }
      }
    )

}

suspend inline fun <T> Flow<T>.completeToList(): List<T> =
  fold(emptyList<T>()) { acc, value ->
    acc + value
  }
