package com.unicorn.plugin.action

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import kotlin.math.roundToInt

suspend fun HttpClient.downloadFile(file: File, url: Url): Flow<DownloadResult> {
    return flow {
        get<HttpStatement>(url).execute { response ->
            val channel = response.receive<ByteReadChannel>()
            val data = ByteArray(response.contentLength()!!.toInt())
            var offset = 0
            do {
                val currentRead = channel.readAvailable(data, offset, data.size / 1)
                offset += currentRead
                val progress = (offset * 100f / data.size).roundToInt()
                println("emit progress: $progress")
                emit(DownloadResult.Progress(progress))
            } while (currentRead > 0)
            if (response.status.isSuccess()) {
                file.writeBytes(data)
                emit(DownloadResult.Success)
            } else {
                emit(DownloadResult.Error("File not downloaded"))
            }
        }
    }
}

sealed class DownloadResult {
    object Success : DownloadResult()
    data class Error(val message: String, val cause: Exception? = null) : DownloadResult()
    data class Progress(val progress: Int): DownloadResult()
}
