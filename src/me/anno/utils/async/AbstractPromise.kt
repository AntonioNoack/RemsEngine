package me.anno.utils.async

import me.anno.cache.IgnoredException

open class AbstractPromise<V : Any> {

    var value: V? = null
        private set
        get() {
            ensureLoading()
            return field
        }

    var error: Exception? = null

    private var callbacks: ArrayList<Callback<V>>? = ArrayList()

    open fun ensureLoading() {}

    fun setValue(v: V?, err: Exception?) {
        value = v
        error = err
        val callbacks = synchronized(this) {
            val callbacks = callbacks
            this.callbacks = null
            callbacks
        }
        if (callbacks != null) {
            onSet(callbacks, v, err)
        }
    }

    private fun onSet(callbacks: List<Callback<V>>, v: V?, err: Exception?) {
        for (i in callbacks.indices) {
            try {
                callbacks[i].call(v, err)
            } catch (ignored: IgnoredException) {
            } catch (e: Exception) { // nothing we can do about it
                e.printStackTrace()
            }
        }
    }

    fun <W : Any> then(onSuccess: (V) -> W): AbstractPromise<W> {
        ensureLoading()
        // not needed on all calls, BUT this is rarely called, so it shouldn't matter
        val result = AbstractPromise<W>()
        val callImmediately = synchronized(this) {
            val afterThis = callbacks
            afterThis?.add { _, _ -> thenMapIt(result, onSuccess) }
            afterThis == null
        }
        if (callImmediately) {
            thenMapIt(result, onSuccess)
        }
        return result
    }

    fun <W : Any> thenAsync(onSuccess: (V, Callback<W>) -> Unit): AbstractPromise<W> {
        ensureLoading()
        // not needed on all calls, BUT this is rarely called, so it shouldn't matter
        val result = AbstractPromise<W>()
        val callImmediately = synchronized(this) {
            val afterThis = callbacks
            afterThis?.add { _, _ -> thenMapIt(result, onSuccess) }
            afterThis == null
        }
        if (callImmediately) {
            thenMapIt(result, onSuccess)
        }
        return result
    }

    fun catch(onError: (Exception?) -> Unit): AbstractPromise<V> {
        ensureLoading()
        // not needed on all calls, BUT this is rarely called, so it shouldn't matter
        val callImmediately = synchronized(this) {
            val afterThis = callbacks
            afterThis?.add { _, _ -> if (value == null) onError(error) }
            afterThis == null
        }
        if (callImmediately && value == null) {
            onError(error)
        }
        return this
    }

    private fun <W : Any> thenMapIt(result: AbstractPromise<W>, map: (V) -> W) {
        val value = value
        val newValue = if (value != null) {
            // we need to catch errors when mapping
            try {
                map(value)
            } catch (ignored: IgnoredException) {
                return // ignored completely
            } catch (err: Exception) {
                result.setValue(null, err)
                return
            }
        } else null
        result.setValue(newValue, error)
        return
    }

    private fun <W : Any> thenMapIt(result: AbstractPromise<W>, map: (V, Callback<W>) -> Unit) {
        val value = value
        if (value != null) {
            // we need to catch errors when mapping
            try {
                map(value, result::setValue)
            } catch (ignored: IgnoredException) {
            } catch (err: Exception) {
                result.setValue(null, err)
            }
        }
    }
}