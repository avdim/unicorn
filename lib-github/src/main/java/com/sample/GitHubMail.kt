package com.sample

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
class GitHubMail(
    val email: String?,
    val primary: Boolean,
    val verified: Boolean,
    val visibility: String
)

suspend fun HttpClient.getGithubMail(token: String): String {
    val json = request<String>(
        url = Url("https://api.github.com/user/emails")
    ) {
        method = HttpMethod.Get
        header("Authorization", "bearer $token")
        header("Accept", "*/*")
//        contentType(/**/)
    }
    val result:List<GitHubMail> = Json.decodeFromString(json)

    val mail: String = result.firstOrNull()?.email ?: "no visible mails"
    return mail
}

