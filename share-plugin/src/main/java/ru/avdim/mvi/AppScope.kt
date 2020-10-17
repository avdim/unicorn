package ru.avdim.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

internal inline fun getAppScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + newSingleThreadContext("mySingleThreadContext"))

val APP_SCOPE by lazy { getAppScope() }

fun launchAppScope(block: suspend () -> Unit) {
    APP_SCOPE.launch {
        block()
    }
}
