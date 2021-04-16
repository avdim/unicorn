package com.sample

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
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
        for (workflowRun in currentData.data.workflowRuns) {
          if (lambda(workflowRun)) {
            emit(workflowRun)
          } else {
            break
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
): Response<WorkflowRuns> =
  // https://docs.github.com/en/rest/reference/actions#workflow-runs
  tryStringRequest {
    request<String>(
      url = Url("https://api.github.com/repos/$owner/$repo/actions/runs")
        .copy(
          parameters = parametersOf(
            "per_page" to listOf("100"),
            "page" to listOf("0")//todo page
          )
        )
    ) {
      method = HttpMethod.Get
      header("Authorization", "bearer ${token.tokenString}")
      header("Accept", "*/*")
//        contentType(/**/)
    }
  }.fromJson<WorkflowRuns>()

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
): Response<WorkflowRunJobs> =
  // https://docs.github.com/en/rest/reference/actions#list-jobs-for-a-workflow-run
  tryStringRequest {
    request<String>(
      url = Url("https://api.github.com/repos/$owner/$repo/actions/runs/$runId/jobs")
        .copy(
          parameters = parametersOf(
            "per_page" to listOf("100"),
            "page" to listOf("0")
          )
        )
    ) {
      method = HttpMethod.Get
      header("Authorization", "bearer ${token.tokenString}")
      header("Accept", "*/*")
//        contentType(/**/)
    }
  }
    .fromJson<WorkflowRunJobs>()

