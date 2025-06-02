package me.anno.utils.async

import me.anno.cache.IgnoredException
import me.anno.utils.assertions.assertNull

@Deprecated(USE_DEFERRED_INSTEAD)
open class Promise<V : Any> {

    var value: V? = null
        private set
        get() {
            ensureLoading()
            return field
        }

    var error: Exception? = null

    private var hasResultToBePassedOn = false
    private var onError: ((Exception?) -> Unit)? = null
    private var onSuccess: ((V) -> Unit)? = null
    private var nextPromise: Promise<*>? = null
    private var prevPromise: Promise<*>? = null

    open fun ensureLoading() {}

    fun setValue(v: V?, err: Exception?) {
        synchronized(this) {
            value = v
            error = err
            hasResultToBePassedOn = true
            if (v != null) handleValue(v)
            else handleError(err)
        }
    }

    fun handleValue(value: V) {
        val onSuccess = onSuccess
        if (onSuccess != null) {
            callSuccess(value, onSuccess)
            this.onSuccess = null
        }
    }

    fun handleError(err: Exception?, allowNext: Boolean = true) {
        synchronized(this) {
            this.error = err
            val onError = onError
            if (onError != null) {
                this.onError = null // first set null to prevent recursive issues
                onError(err)
            }
        }

        val nextPromise = nextPromise
        if (allowNext && nextPromise != null) {
            nextPromise.handleError(err, true)
            return
        }

        prevPromise?.handleError(err, false)
    }

    fun then(onSuccess: Callback<V>): Promise<Unit> {
        return then(onSuccess::ok)
    }

    fun <W : Any> then(mapSuccess: (V) -> W): Promise<W> {
        ensureLoading()
        // not needed on all calls, BUT this is rarely called, so it shouldn't matter
        val result = Promise<W>()
        append(result) { value -> result.setValue(mapSuccess(value), null) }
        return result
    }

    fun <W : Any> thenAsync(onSuccess: (V, Callback<W>) -> Unit): Promise<W> {
        ensureLoading()
        // not needed on all calls, BUT this is rarely called, so it shouldn't matter
        val result = Promise<W>()
        append(result) { value -> onSuccess(value, result::setValue) }
        return result
    }

    fun thenFulfill(mapSuccess: Promise<V>) {
        // then { value -> mapSuccess.setValue(value, null) }
        synchronized(this) {
            if (hasResultToBePassedOn) {
                mapSuccess.setValue(value, error)
            } else {
                nextPromise = mapSuccess
            }
        }
    }

    private fun <T> joinCallbacks(prevCallback: ((T) -> Unit)?, currCallback: (T) -> Unit): (T) -> Unit {
        return if (prevCallback != null) {
            { v ->
                prevCallback(v)
                currCallback(v)
            }
        } else currCallback
    }

    private fun append(result: Promise<*>, onSuccess1: (V) -> Unit) {
        synchronized(this) {
            linkToNext(result)
            if (hasResultToBePassedOn) {
                result.hasResultToBePassedOn = true
                val value = value
                if (value != null) {
                    callSuccess(value, onSuccess1)
                } else {
                    // register the error, so future calls on result immediately know about it
                    // registering the success-callback isn't necessary, because the process failed
                    result.error = error
                }
            } else {
                onSuccess = joinCallbacks(onSuccess, onSuccess1)
            }
        }
    }

    private fun callSuccess(value: V, onSuccess1: (V) -> Unit) {
        try {
            onSuccess1(value)
        } catch (_: IgnoredException) {
        } catch (e: Exception) {
            setValue(null, e)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <W : Any> linkToNext(next1: Promise<W>) {
        assertNull(next1.prevPromise, "next1.prevPromise must be null")
        val next0 = nextPromise as? Promise<W>
        if (next0 != null) {
            // this -> [next0...] -> next1
            next0.linkToNext(next1)
        } else {
            // this -> next1
            linkToNextUnsafe(next1)
        }
    }

    private fun linkToNextUnsafe(result1: Promise<*>) {
        this.nextPromise = result1
        result1.prevPromise = this
    }

    fun catch(onError1: (Exception?) -> Unit): Promise<V> {
        ensureLoading()
        synchronized(this) {
            if (hasResultToBePassedOn && value == null) {
                onError1(error)
            } else {
                onError = joinCallbacks(onError, onError1)
            }
        }
        return this
    }
}