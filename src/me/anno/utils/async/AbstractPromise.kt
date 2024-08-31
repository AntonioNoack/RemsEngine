package me.anno.utils.async

import me.anno.cache.IgnoredException

open class AbstractPromise<V> {

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

    fun <W> then(map: (V) -> W): AbstractPromise<W> {
        ensureLoading()
        // not needed on all calls, BUT this is rarely called, so it shouldn't matter
        val result = AbstractPromise<W>()
        val callImmediately = synchronized(this) {
            val afterThis = callbacks
            afterThis?.add { _, _ -> thenMapIt(result, map) }
            afterThis == null
        }
        if (callImmediately) {
            thenMapIt(result, map)
        }
        return result
    }

    fun <W : Any> catch(map: (Exception?) -> Unit): AbstractPromise<V> {
        ensureLoading()
        // not needed on all calls, BUT this is rarely called, so it shouldn't matter
        val callImmediately = synchronized(this) {
            val afterThis = callbacks
            afterThis?.add { _, _ -> map(error) }
            afterThis == null
        }
        if (callImmediately) {
            map(error)
        }
        return this
    }

    private fun <W> thenMapIt(result: AbstractPromise<W>, map: (V) -> W) {
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
}