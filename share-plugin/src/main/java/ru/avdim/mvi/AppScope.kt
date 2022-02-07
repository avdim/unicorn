package ru.avdim.mvi

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing

internal inline fun getAppScope(): CoroutineScope = if(true) CoroutineScope(SupervisorJob() + Dispatchers.Swing) else MainScope() + Job()
//    CoroutineScope(SupervisorJob() + newSingleThreadContext("mySingleThreadContext"))

val APP_SCOPE by lazy { getAppScope() }

fun launchAppScope(block: suspend () -> Unit) {
    APP_SCOPE.launch {
        block()
    }
}
