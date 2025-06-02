package me.anno.utils.async

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD
import java.io.IOException

@Suppress("RedundantSuspendModifier")
@Deprecated(USE_COROUTINES_INSTEAD)
suspend fun <R : Any> waitForCallback(generator: (Callback<R>) -> Unit): Result<R> {
    var value: R? = null
    var error: Exception? = null
    var hasAnswer = false
    promise(generator)
        .then {
            value = it
            hasAnswer = true
        }
        .catch {
            error = it
            if (it == null) error = IOException("Returned null")
            hasAnswer = true
        }
    Sleep.waitUntil(true) {
        hasAnswer
    }
    return if (error != null) {
        Result.failure(error!!)
    } else {
        Result.success(value!!)
    }
}

fun <R> Result<R>.unpack(): Pair<R?, Throwable?> {
    return Pair(getOrNull(), exceptionOrNull())
}

fun <R> pack(value: R?, error: Throwable?): Result<R> {
    return if (value != null) Result.success(value)
    else Result.failure(error ?: IOException("?"))
}

@Deprecated("This method blocks inside suspend, which is horrible!!")
fun <V> suspendToValue(async: Boolean, suspendFunc: suspend () -> Result<V>): V? {
    if (async) {
        var result: Result<V>? = null
        GlobalScope.launch {
            launch {
                result = suspendFunc()
            }
        }
        Sleep.waitUntil(true) { result != null }
        return result?.getOrNull()
    } else {
        var result: Result<V>? = null
        runBlocking {
            launch {
                result = suspendFunc()
            }
        }
        return result?.getOrNull()
    }
}

@Deprecated("This method blocks inside suspend, which is horrible!!")
fun <V> deferredToValue(deferred: Deferred<Result<V>>, async: Boolean): V? {
    return suspendToValue(async) {
        deferred.await()
    }
}

@Deprecated(USE_COROUTINES_INSTEAD)
fun <V> suspendToCallback(suspendFunc: suspend () -> Result<V>, callback: Callback<V>) {
    GlobalScope.launch {
        launch {
            val result = suspendFunc()
            val (value, err) = result.unpack()
            callback.call(value, err as? Exception)
        }
    }
}

fun <V> deferredToCallback(deferred: Deferred<Result<V>>, callback: Callback<V>) {
    suspendToCallback({
        deferred.await()
    }, callback)
}

fun <K, V> Result<K>.castFailure(): Result<V> {
    assertTrue(isFailure)
    @Suppress("UNCHECKED_CAST")
    return this as Result<V>
}

inline fun <K, V> Result<K>.mapSuccess(map: (K) -> Result<V>): Result<V> {
    return if (isSuccess) {
        map(getOrThrow())
    } else castFailure()
}

inline fun <K, V> Result<K>.mapSuccess2(map: (K) -> V): Result<V> {
    return if (isSuccess) {
        Result.success(map(getOrThrow()))
    } else castFailure()
}

suspend inline fun <V> Result<V>.orElse(call: suspend () -> Result<V>): Result<V> {
    return if (isSuccess) this
    else call()
}