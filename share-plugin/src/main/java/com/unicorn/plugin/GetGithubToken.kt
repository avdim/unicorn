package com.unicorn.plugin

import com.unicorn.share.BuildConfig
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

val AUTH_TOKEN_URL = "https://tutu-ci.herokuapp.com/github_token_localhost"

@OptIn(EngineAPI::class)
fun getGithubToken(
  tokenScope: String = "user:email",
  port: Int = 55555,
  callback: (token: String) -> Unit
) {
  var mutableCallbackPipe: (token: String) -> Unit = {
    callback(it)
  }
  var server: BaseApplicationEngine? = null
  //todo уничтожать сервер
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
          val response: HttpResponse = client.request(
            url = Url(AUTH_TOKEN_URL).copy(
              parameters = parametersOf(
                "client_id" to listOf(BuildConfig.GITHUB_CLIENT_ID),
                "code" to listOf(githubAuthCode)
              )
            )
          ) {
            method = HttpMethod.Get
          }

          val githubToken =
            if (true) {
              response.readText()
            } else {
              //На всякий случай оставил тут запасной код, но в идеале просто серверный кусок heroku положить сюда
              val tokenResponse: String = client.post("https://github.com/login/oauth/access_token") {
                body = TextContent(
                  """
                    {
                      "client_id": "${BuildConfig.GITHUB_CLIENT_ID}",
                      "client_secret": ${throw Error("GITHUB_CLIENT_SECRET")},
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
              resultParams.get("access_token")
            }
          mutableCallbackPipe(githubToken!!)
        }
      }

    }//routing
  }
  server.start(wait = false)
  MainScope().launch {
    val authHref = authHref(tokenScope)
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
