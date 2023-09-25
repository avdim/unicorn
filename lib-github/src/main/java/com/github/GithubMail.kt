package com.github

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

@Serializable
class Release(
  val url: String,
  val html_url: String,
  val assets: List<ReleaseAsset>,
)

@Serializable
class ReleaseAsset(
  val browser_download_url: String
)

@Serializable
class GitHubMail(
  val email: String?,
  val primary: Boolean,
  val verified: Boolean,
  val visibility: String?
)

suspend fun HttpClient.getGithubMail(token: Token<Permission.Mail>): String {
  TODO()
//  val result = tryStringRequest {
//    request<String>(
//      url = Url("https://api.github.com/user/emails")
//    ) {
//      method = HttpMethod.Get
//      header("Authorization", "bearer ${token.tokenString}")
//      header("Accept", "*/*")
////        contentType(/**/)
//    }
//  }.fromJson<List<GitHubMail>>()
//
//  return when (result) {
//    is Response.Success -> {
//      result.data.firstOrNull()?.email ?: "no emails at account"
//    }
//    is Response.Error -> {
//      return "error: ${result.message}"
//    }
//  }
}

suspend fun HttpClient.getGithubRepoReleases(owner: String, repo: String): List<Release> {
  TODO()
//  //https://docs.github.com/en/free-pro-team@latest/rest/reference/repos#list-releases
//  val jsonStr = request<String>(
//    url = Url("https://api.github.com/repos/$owner/$repo/releases")
//  ) {
//    method = HttpMethod.Get
//    header("Accept", "application/vnd.github.v3+json")
////        contentType(/**/)
//  }
//  val result: List<Release> = jsonParser.decodeFromString(jsonStr)
//  return result
}

class Token<out T : Permission>(val tokenString: String)

inline fun <reified T : Permission> requestToken(): Token<T> {
  println("scopes: " + scopes<T>())
  val responseToken: String = "todo token value"//todo request token
  return Token(responseToken)
}

interface MyPermission : Permission.Mail, Permission.Repo
