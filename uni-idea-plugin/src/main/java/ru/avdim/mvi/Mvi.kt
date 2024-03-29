package ru.avdim.mvi

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

typealias Reducer<S, A> = suspend (S, A) -> S

interface Store<S, A> {
    fun send(action: A)
    val stateFlow: StateFlow<S>
    val state get() = stateFlow.value
}

/**
 * Самая простая реализация MVI архитектуры для слоя представления.
 */
fun <S, A> createStore(init: S, reducer: Reducer<S, A>): Store<S, A> {
    val mutableStateFlow = MutableStateFlow(init)
    val channel: Channel<A> = Channel(Channel.UNLIMITED)

    return object : Store<S, A> {
        init {
            //https://m.habr.com/ru/company/kaspersky/blog/513364/
            //or alternative in jvm use fun CoroutineScope.actor(...)
            APP_SCOPE.launch {
                channel.consumeAsFlow().collect { action ->
                    mutableStateFlow.value = reducer(mutableStateFlow.value, action)
                }
            }
        }

        override fun send(action: A) {
          APP_SCOPE.launch {
            channel.trySend(action)//mutableStateFlow.value = reducer(mutableStateFlow.value, action)
          }
        }

        override val stateFlow: StateFlow<S> = mutableStateFlow
    }
}

typealias ReducerSE<S, A, SE> = suspend (S, A) -> ReducerResult<S, SE>

class ReducerResult<S, SE>(val state: S, val sideEffects: List<SE> = emptyList())

/**
 * MVI по типу ELM с обработкой SideEffect-ов
 */
fun <S, A, SE> createStoreWithSideEffect(
    init: S,
    effectHandler: (store: Store<S, A>, sideEffect: SE) -> Unit,
    reducer: ReducerSE<S, A, SE>
): Store<S, A> {
    lateinit var store: Store<S, A>
    store = createStore(init) { state, action ->
        val result = reducer(state, action)

        result.sideEffects.forEach {
            effectHandler(store, it)
        }

        result.state
    }
    return store
}
