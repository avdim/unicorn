package com.sample

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

val jsonParser = Json {
  ignoreUnknownKeys = true
}

inline fun <reified T> Response<String>.fromJson(): Response<T> {
  return mapIfSuccess {
    jsonParser.decodeFromString(it)
  }
}
