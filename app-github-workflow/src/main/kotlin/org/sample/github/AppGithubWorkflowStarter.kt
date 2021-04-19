package org.sample.github

import com.github.*
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
val SINCE_TIME = LocalDateTime(2021, Month.MARCH, 30, 0, 0).toInstant(TimeZone.UTC)

enum class RepoType {
  IOS,
  ANDROID
}

data class RepoWithType(
  val repo: String,
  val type: RepoType
)

data class TableData(val repoWithType: RepoWithType, val rows: List<TableRow>)
data class TableRow(val timing:WorkflowRunTiming, val workflowRun: WorkflowRun)

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
  return
  val client = HttpClient(Apache)

  if (false) {
    val githubMail = client.getGithubMail(Token(BuildConfig.SECRET_GITHUB_TOKEN))
    println("githubMail: $githubMail")
  }

  val allRepos = coroutineScope {
    REPOS.map { repoWithType ->
      async {
        TableData(
          repoWithType = repoWithType,
          rows = client.getGithubWorkflowRunsPagesUntil(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, repoWithType.repo) {
            it.createdTime > SINCE_TIME
          }.flatMapMerge { run ->
            flow {
              emit(
                client.getGithubWorkflowRunTiming(Token(BuildConfig.SECRET_GITHUB_TOKEN), TUTU_ORGANIZATION, repoWithType.repo, run.id)
                  .mapIfSuccess {
                    TableRow(it, run)
                  }
              )
            }
          }.mapNotNull { if (it is Response.Success) it else null }
            .completeToList().map { it.data}
        )
      }
    }.awaitAll()
  }

  val totalDuration = allRepos.sumBy { it.rows.billDuration() }

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
              +"time: ${repo.rows.billDuration()} in repo ${repo.repoWithType.repo}"
              br {}
            }
            allRepos.forEach { repo ->
              br {}
              br {}
              h2 {
                +"time: ${repo.rows.billDuration()} in repo ${repo.repoWithType.repo}"
              }
              br {}
              table {
                repo.rows.sortedByDescending {
                  it.timing.billMinutes
                }.forEach { row ->
                  val mins = row.timing.billMinutes
                  tr {
                    td {
                      +"$mins m"
                    }
                    td {
                      val create = row.workflowRun.createdAt
                      +"$create"
                    }
                    td {
                      a(row.workflowRun.htmlUrl) {
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

fun Collection<TableRow>.billDuration() =
  sumBy { it.timing.billMinutes }

suspend inline fun <T> Flow<T>.completeToList(): List<T> =
  fold(emptyList<T>()) { acc, value ->
    acc + value
  }
