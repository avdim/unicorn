package com.github

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WorkflowRuns(
  @SerialName("total_count")
  val totalCount: Long,
  @SerialName("workflow_runs")
  val workflowRuns: List<WorkflowRun>,
)

@Serializable
class WorkflowRun(
  @SerialName("id")
  val id: Long,
  @SerialName("name")
  val name: String,
  @SerialName("created_at")
  val createdAt: String,
  @SerialName("html_url")
  val htmlUrl: String,
)

suspend fun HttpClient.getGithubWorkflowRunsPagesUntil(
  token: Token<Permission.Repo>,
  owner: String,
  repo: String,
  lambda: (WorkflowRun) -> Boolean,
): Flow<WorkflowRun> {
  return flow {
    val ELEMENTS_ON_PAGE = 100
    var page = 0
    do {
      val currentData = getGithubWorkflowRuns(token, owner, repo, page, ELEMENTS_ON_PAGE)
      if (currentData is Response.Success) {
        currentData.data.workflowRuns.forEach { workflowRun ->
          if (lambda(workflowRun)) {
            emit(workflowRun)
          } else {
            return@flow
          }
        }
      }
      page++
    } while (currentData is Response.Success && page * ELEMENTS_ON_PAGE < currentData.data.totalCount)
  }
}

suspend fun HttpClient.getGithubWorkflowRuns(
  token: Token<Permission.Repo>,
  owner: String,
  repo: String,
  page: Int = 0,
  perPage: Int = 100,
): Response<WorkflowRuns> = TODO()
//  // https://docs.github.com/en/rest/reference/actions#workflow-runs
//  tryStringRequest {
//    request<String>(
//      url = Url("https://api.github.com/repos/$owner/$repo/actions/runs")
//        .copy(
//          parameters = parametersOf(
//            "per_page" to listOf(perPage.toString()),
//            "page" to listOf(page.toString())
//          )
//        )
//    ) {
//      method = HttpMethod.Get
//      header("Authorization", "bearer ${token.tokenString}")
//      header("Accept", "*/*")
////        contentType(/**/)
//    }
//  }.fromJson<WorkflowRuns>()

@Serializable
class WorkflowRunJobs(
  @SerialName("total_count")
  val totalCount: Long,
  @SerialName("jobs")
  val jobs: List<WorkflowRunJob>
)

@Serializable
class WorkflowRunJob(
  @SerialName("id")
  val id: Long,
  @SerialName("html_url")
  val htmlUrl: String? = null,
  @SerialName("started_at")
  val startedAt: String? = null,
  @SerialName("completed_at")
  val completedAt: String? = null,
)

suspend fun HttpClient.getGithubWorkflowRunJobs(
  token: Token<Permission.Repo>,
  owner: String,
  repo: String,
  runId: Long
): Response<WorkflowRunJobs> = TODO()
//  // https://docs.github.com/en/rest/reference/actions#list-jobs-for-a-workflow-run
//  tryStringRequest {
//    request<String>(
//      url = Url("https://api.github.com/repos/$owner/$repo/actions/runs/$runId/jobs")
//        .copy(
//          parameters = parametersOf(
//            "per_page" to listOf("100"),
//            "page" to listOf("0")
//          )
//        )
//    ) {
//      method = HttpMethod.Get
//      header("Authorization", "bearer ${token.tokenString}")
//      header("Accept", "*/*")
////        contentType(/**/)
//    }
//  }
//    .fromJson<WorkflowRunJobs>()

@Serializable
data class WorkflowRunTiming(
  @SerialName("billable")
  val billable: Billable
) {
  @Serializable
  data class Billable(
    @SerialName("UBUNTU")
    val ubuntu: TimingContainer? = null,
    @SerialName("MACOS")
    val macOS: TimingContainer? = null,
    @SerialName("WINDOWS")
    val windows: TimingContainer? = null,
  ) {
    @Serializable
    data class TimingContainer(
      @SerialName("total_ms")
      val totalMs: Long,
      @SerialName("jobs")
      val jobs: Int
    )
  }
}

fun WorkflowRunTiming.Billable.TimingContainer?.orEmpty() =
  this ?: WorkflowRunTiming.Billable.TimingContainer(0, 0)

/**
 * @param workflowId The ID of the workflow. You can also pass the workflow file name as a string.
 */
suspend fun HttpClient.getGithubWorkflowRunTiming(
  token: Token<Permission.Repo>,
  owner: String,
  repo: String,
  runId: Long
): Response<WorkflowRunTiming> = TODO()
//  // https://docs.github.com/en/rest/reference/actions#get-workflow-run-usage
//  tryStringRequest {
//    request<String>(
//      url = Url("https://api.github.com/repos/$owner/$repo/actions/runs/$runId/timing")
//    ) {
//      method = HttpMethod.Get
//      header("Authorization", "bearer ${token.tokenString}")
//      header("Accept", "*/*")
////        contentType(/**/)
//    }
//  }
//    .fromJson<WorkflowRunTiming>()
//    .ifError {
//      println("error $it")
//    }

val WorkflowRunTiming.billMinutes
  get(): Int {
    val totalMs = billable.macOS.orEmpty().totalMs * 10 + billable.windows.orEmpty().totalMs * 2 + billable.ubuntu.orEmpty().totalMs
    return (totalMs / 1000 / 60).toInt()
  }
