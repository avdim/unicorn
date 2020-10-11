package com.unicorn.plugin.server

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.http.content.staticRootFolder
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.*
import io.ktor.server.netty.Netty
import java.io.File

@OptIn(EngineAPI::class)
fun startKtorServer() {
  embeddedServer(if (true) Netty else CIO, 12345, configure = {
    //конфигурация может быть специфичная для Netty или CIO
    connectionGroupSize
    workerGroupSize
    callGroupSize
//    requestQueueLimit
//    runningLimit
//    shareWorkGroup
  }) {
    install(AutoHeadResponse)
    routing {
      //health check for kubernetes
      get("/") {
        call.respondText { "hello from idea plugin" }
      }
      if (false) static("static") {
        staticRootFolder = File("/Users/dim/Desktop/static_web")
        files(".")
      }
    }
  }.start(wait = false)
}
