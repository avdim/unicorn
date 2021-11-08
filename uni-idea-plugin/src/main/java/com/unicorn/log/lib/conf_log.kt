package com.unicorn.log.lib

inline fun DEBUG_LEVEL_ENABLE(lambda: () -> Unit) {
//  lambda()
}

inline fun TODO_LEVEL_ENABLE(lambda: () -> Unit) {
//  lambda()
}

inline fun INFO_LEVEL_ENABLE(lambda: () -> Unit) {
  lambda()
}

inline fun WARNING_LEVEL_ENABLE(lambda: () -> Unit) {
  lambda()
}

inline fun ERROR_LEVEL_ENABLE(lambda: () -> Unit) {
  lambda()
}
