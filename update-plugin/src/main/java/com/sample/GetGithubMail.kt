package com.sample

import com.unicorn.update.BuildConfig
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@OptIn(EngineAPI::class)
fun getGithubMail(
    port: Int = 55555,
    callback: (mail: String) -> Unit
) {
    var mutableCallbackPipe: (mail: String) -> Unit = {
        callback(it)
    }
    var server: BaseApplicationEngine? = null
    server = GlobalScope.embeddedServer(if (true) Netty else CIO, port, configure = {
        //конфигурация может быть специфичная для Netty или CIO
        connectionGroupSize
        workerGroupSize
        callGroupSize
        //requestQueueLimit
        //runningLimit
        //shareWorkGroup
    }) {
        routing {
            get("/") {
                val githubAuthCode = context.parameters["code"]
                if (githubAuthCode != null) {
                    val client: HttpClient = HttpClient(Apache)
                    val tokenResponse: String = client.post("https://github.com/login/oauth/access_token") {
                        body = TextContent(
                            """
                                  {
                                    "client_id": "${BuildConfig.GITHUB_CLIENT_ID}",
                                    "client_secret": "${BuildConfig.GITHUB_CLIENT_SECRET}",
                                    "code": "$githubAuthCode"
                                  }              
                                """.trimIndent(),
                            ContentType.Application.Json
                        )
                    }
                    val resultParams: Map<String, String> = tokenResponse.split("&").associate {
                        val split = it.split("=")
                        val key = split[0]
                        val value = split[1]
                        key to value
                    }

                    val tokenStr = resultParams.get("access_token")
                    if (tokenStr != null) {
                        val token = Token<MyPermission>(tokenStr)
                        val mail = client.getGithubMail(token)
                        val releases = client.getGithubRepoReleases("avdim", "unicorn")
                        releases.flatMap { it.assets }.map { it.browser_download_url }
                        println("releases: $releases")
                        mutableCallbackPipe(mail)
                    }
                }
            }
        }
    }
    server.start(wait = false)
    MainScope().launch {
        val authHref = authHref("user:email")
        val closable = openBrowserJBCeffOrDefault(authHref)
        val oldCallback = mutableCallbackPipe
        mutableCallbackPipe = {
            oldCallback(it)
            closable.close()
        }
    }
}

/**
 * https://developer.github.com/apps/building-oauth-apps/understanding-scopes-for-oauth-apps/
 */
private fun authHref(tokenScope: String): String =
    Url("https://github.com/login/oauth/authorize/")
        .copy(
            parameters = parametersOf(
                "client_id" to listOf(BuildConfig.GITHUB_CLIENT_ID),
                "scope" to listOf(tokenScope)
            )
        ).toString()
