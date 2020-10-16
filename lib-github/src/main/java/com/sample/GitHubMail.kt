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

suspend fun HttpClient.getGithubMail(token: Token<Permission.Mail>): String {
    val json = request<String>(
        url = Url("https://api.github.com/user/emails")
    ) {
        method = HttpMethod.Get
        header("Authorization", "bearer ${token.tokenString}")
        header("Accept", "*/*")
//        contentType(/**/)
    }
    val result: List<GitHubMail> = Json.decodeFromString(json)

    val mail: String = result.firstOrNull()?.email ?: "no visible mails"
    return mail
}

class Token<out T : Permission>(val tokenString: String)

inline fun <reified T : Permission> requestToken(): Token<T> {
    println("scopes: " + scopes<T>())
    val responseToken: String = "todo token value"//todo request token
    return Token(responseToken)
}

interface MyPermission : Permission.Mail, Permission.Todo
