package com.sample

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

suspend fun HttpClient.getGithubWorkflowRuns(token: Token<Permission.Repo>, owner: String, repo: String): String {
  // https://docs.github.com/en/rest/reference/actions#workflow-runs
  val response = tryStringRequest {
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
  }

  if (response is Response.Success) {
    println(response.data)
  }
//  val result: List<GitHubMail> = jsonParser.decodeFromString(jsonStr)
  return "TODO"
}

