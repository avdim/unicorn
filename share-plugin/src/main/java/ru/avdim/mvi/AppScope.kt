package ru.avdim.mvi

import kotlinx.coroutines.*

internal inline fun getAppScope(): CoroutineScope = MainScope() + Job()
//    CoroutineScope(SupervisorJob() + newSingleThreadContext("mySingleThreadContext"))

val APP_SCOPE by lazy { getAppScope() }

fun launchAppScope(block: suspend () -> Unit) {
    APP_SCOPE.launch {
        block()
    }
}
