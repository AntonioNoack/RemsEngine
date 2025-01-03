package me.anno.utils.async


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
    private var prevPromise: Promise<*>? = null
    private var nextPromise: Promise<*>? = null

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
        onSuccess?.invoke(value)
        onSuccess = null
    }

    fun handleError(err: Exception?, allowPrev: Boolean = true) {
        synchronized(this) {
            this.error = err

            val onError = onError
            if (onError != null) {
                onError(err)
                this.onError = null
            }
        }

        val prevPromise = prevPromise
        if (allowPrev && prevPromise != null) {
            prevPromise.handleError(err, true)
            return
        }

        nextPromise?.handleError(err, false)
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
                    onSuccess1(value)
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

    private fun linkToNext(result: Promise<*>) {
        if (prevPromise != null || result.nextPromise != null) {
            throw IllegalStateException()
        }
        prevPromise = result
        result.nextPromise = this
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