package com.sample

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
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
  val name: String
)

suspend fun HttpClient.getGithubWorkflowRuns(token: Token<Permission.Repo>, owner: String, repo: String): Response<WorkflowRuns> =
  // https://docs.github.com/en/rest/reference/actions#workflow-runs
  tryStringRequest {
    request<String>(
      url = Url("https://api.github.com/repos/$owner/$repo/actions/runs")
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

