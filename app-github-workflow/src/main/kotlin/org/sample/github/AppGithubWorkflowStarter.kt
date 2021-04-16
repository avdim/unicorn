package org.sample.github

import com.sample.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
val SINCE_TIME = LocalDateTime(2021, Month.MARCH, 29, 0, 0).toInstant(TimeZone.UTC)

enum class RepoType {
  IOS,
  ANDROID
}

data class RepoWithType(
  val repo: String,
  val type: RepoType
)

class RepoWithJobs(
  val jobs: List<WorkflowRunJob>,
  val repoWithType: RepoWithType
)

val REPOS = listOf(
  RepoWithType("ios-core", RepoType.IOS),
  RepoWithType("feed-ios", RepoType.IOS),
  RepoWithType("ios-solution", RepoType.IOS),
  RepoWithType("ios-tutu-id", RepoType.IOS),
  RepoWithType("ios-foundation", RepoType.IOS),
  RepoWithType("ios-designkit", RepoType.IOS),
  RepoWithType("iosApp_Etrain", RepoType.IOS),
)

@OptIn(ExperimentalPathApi::class)
suspend fun main() {
  println("hello AppGithubWorkflowStarter")
  println("token: ${BuildConfig.SECRET_GITHUB_TOKEN}")
  val client = HttpClient(Apache)

  if (false) {
    val githubMail = client.getGithubMail(Token(BuildConfig.SECRET_GITHUB_TOKEN))
    println("githubMail: $githubMail")
  }

  val allRepos = coroutineScope {
    REPOS.map { repoWithType ->
      async {
        val jobs = client.getGithubWorkflowRunsPagesUntil(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, repoWithType.repo) {
          it.createdTime > SINCE_TIME
        }.flatMapMerge { run ->
          flow {
            emit(
              client.getGithubWorkflowRunJobs(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, repoWithType.repo, run.id)
            )
          }
        }.mapNotNull {
          if (it is Response.Success) it else null
        }.completeToList()
          .flatMap {
            it.data.jobs
          }
        RepoWithJobs(jobs, repoWithType)
      }
    }.awaitAll()
  }

  val totalDuration = allRepos.fold(WrapDuration(0)) { acc, value ->
    acc + value.jobs.calcDuration()
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
            h3 {
              +"total duration ${totalDuration} since $SINCE_TIME"
            }
            allRepos.forEach { repo ->
              br {}
              br {}
              h2 {
                +"time: ${repo.jobs.calcDuration()} in repo ${repo.repoWithType.repo}"
              }
              br {}
              table {
                repo.jobs.forEach { job ->
                  val sec = job.calcDuration().seconds
                  tr {
                    td {
                      +"$sec"
                    }
                    td {
                      +"${sec / 60 + 1} m"
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
      }
    )

}

fun Collection<WorkflowRunJob>.calcDuration() =
  map { it.calcDuration() }
    .fold(WrapDuration(0)) { acc, value ->
      acc + value
    }

suspend inline fun <T> Flow<T>.completeToList(): List<T> =
  fold(emptyList<T>()) { acc, value ->
    acc + value
  }
