package com.github

import io.ktor.client.*

sealed class Response<out T> {
  class Success<T>(val data: T) : Response<T>()
  class Error(val message: String, val throwable: Throwable?) : Response<Nothing>()
}

inline fun <A, B> Response<A>.mapIfSuccess(lambda: (A) -> B): Response<B> =
  when (this) {
    is Response.Success<A> -> {
      try {
        Response.Success(lambda(this.data))
      } catch (t: Throwable) {
        Response.Error("mapping error in mapIfSuccess", t)
      }
    }
    is Response.Error -> {
      this
    }
  }

inline fun <T> Response<T>.ifError(lambda: (Response.Error) -> Unit): Response<T> {
  if (this is Response.Error) {
    lambda(this)
  }
  return this
}

inline fun HttpClient.tryStringRequest(lambda: HttpClient.() -> String): Response<String> =
  try {
    Response.Success(lambda())
  } catch (t: Throwable) {
    Response.Error("request error", t)
  }

