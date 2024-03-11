package me.anno.utils.pooling

import me.anno.Time
import me.anno.ecs.annotations.Docs
import me.anno.maths.Maths
import me.anno.maths.Maths.MILLIS_TO_NANOS

abstract class BufferPool<V>(
    val size: Int,
    val elementSize: Int,
    val timeoutMillis: Long = 5000L
) {

    open fun prepare(buffer: V, size: Int) {}
    abstract fun clear(buffer: V, size: Int)
    abstract fun getSize(buffer: V): Int
    abstract fun createBuffer(size: Int): V
    open fun destroy(buffer: V) {}

    val totalSize: Long
        get() {
            var length = 0L
            // no lock required
            for (i in available.indices) {
                val element = available[i]
                if (element != null) {
                    @Suppress("unchecked_cast")
                    length += getSize(element as V)
                }
            }
            return length * elementSize
        }

    private val available = arrayOfNulls<Any?>(size)
    private val lastUsed = LongArray(size)
    operator fun get(size: Int, clear: Boolean, exactMatchesOnly: Boolean): V {
        val maxSize = if (exactMatchesOnly) size else size * 2
        synchronized(this) {
            for (i in 0 until this.size) {
                val candidate = available.getOrNull(i)
                if (candidate != null) {
                    @Suppress("unchecked_cast")
                    candidate as V
                    if (getSize(candidate) in size..maxSize) {
                        available[i] = null
                        prepare(candidate, size)
                        if (clear) {
                            clear(candidate, size)
                        }
                        return candidate
                    }
                }
            }
        }
        return createBuffer(size)
    }

    fun returnBuffer(buffer: V?) {
        buffer ?: return
        synchronized(this) {
            for (i in 0 until size) {
                if (available[i] === buffer) return // mmh, error in the application using this
            }
            for (i in 0 until size) {
                if (available[i] == null) {
                    available[i] = buffer
                    lastUsed[i] = Time.nanoTime
                    return
                }
            }
            val index = (Maths.random() * size).toInt()
            available[index % size] = buffer
        }
    }

    operator fun plusAssign(buffer: V?) {
        returnBuffer(buffer)
    }

    fun freeUnusedEntries() {
        synchronized(this) {
            val time = Time.nanoTime
            val timeoutNanos = timeoutMillis * MILLIS_TO_NANOS
            for (i in available.indices) {
                val element = available[i]
                if (element != null && time - lastUsed[i] > timeoutNanos) {
                    @Suppress("unchecked_cast")
                    destroy(element as V)
                    available[i] = null
                }
            }
        }
    }

    @Docs("Remove all pooled entries")
    fun gc() {
        synchronized(this) {
            for (i in available.indices) {
                val element = available[i]
                if (element != null) {
                    @Suppress("unchecked_cast")
                    destroy(element as V)
                    available[i] = null
                }
            }
        }
    }

}