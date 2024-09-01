package me.anno.utils.pooling

import me.anno.Time
import me.anno.ecs.annotations.Docs
import me.anno.maths.Maths
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.structures.lists.Lists.arrayListOfNulls

abstract class BufferPool<V>(
    val size: Int,
    val elementSize: Int,
    val timeoutMillis: Long = 5000L
) {

    open fun prepare(buffer: V, size: Int) {}
    abstract fun clear(buffer: V, size: Int)
    abstract fun copy(src: V, dst: V, size: Int)
    abstract fun getSize(buffer: V): Int
    abstract fun createBuffer(size: Int): V
    open fun destroy(buffer: V) {}

    fun grow(buffer: V, newSize: Int): V {
        val oldSize = getSize(buffer)
        return if (oldSize < newSize) {
            val newBuffer = createBuffer(newSize)
            copy(buffer, newBuffer, oldSize)
            returnBuffer(buffer)
            newBuffer
        } else buffer
    }

    fun shrink(buffer: V, newSize: Int): V {
        return if (getSize(buffer) > newSize) {
            returnBuffer(buffer)
            createBuffer(newSize)
        } else buffer
    }

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

    private val available = arrayListOfNulls<Any?>(size)
    private val lastUsed = LongArray(size)
    private val smallSizes = HashMap<Int, ObjectPool<V>>()

    fun isSmallSize(size: Int): Boolean {
        return (size < 32) || (size < 1024 && (size and (size - 1)) == 0)
    }

    operator fun get(size: Int, clear: Boolean, exactMatchesOnly: Boolean): V {
        val reused = if (isSmallSize(size)) {
            smallSizes[size]?.create()
        } else findBigBuffer(size, exactMatchesOnly)
        if (reused != null) {
            prepare(reused, size, clear)
            return reused
        } else return createBuffer(size)
    }

    private fun findBigBuffer(size: Int, exactMatchesOnly: Boolean): V? {
        val maxSize = if (exactMatchesOnly) size else size * 2
        synchronized(this) {
            for (i in 0 until this.size) {
                val candidate = available.getOrNull(i)
                if (candidate != null) {
                    @Suppress("unchecked_cast")
                    candidate as V
                    if (getSize(candidate) in size..maxSize) {
                        available[i] = null
                        return candidate
                    }
                }
            }
        }
        return null
    }

    private fun prepare(candidate: V, size: Int, clear: Boolean) {
        prepare(candidate, size)
        if (clear) {
            clear(candidate, size)
        }
    }

    fun returnBuffer(buffer: V?): Nothing? {
        buffer ?: return null
        val size = getSize(buffer)
        if (isSmallSize(size)) {
            smallSizes.getOrPut(size) {
                ObjectPool { createBuffer(size) }
            }.destroy(buffer)
        } else returnBigBuffer(buffer)
        return null
    }

    private fun returnBigBuffer(buffer: V?): Nothing? {
        buffer ?: return null
        synchronized(this) {
            for (i in 0 until size) {
                if (available[i] === buffer) return null // mmh, error in the application using this
            }
            for (i in 0 until size) {
                if (available[i] == null) {
                    available[i] = buffer
                    lastUsed[i] = Time.nanoTime
                    return null
                }
            }
            val index = (Maths.random() * size).toInt()
            available[index % size] = buffer
        }
        return null
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
            for (pool in smallSizes.values) {
                pool.gc()
            }
            smallSizes.clear()
        }
    }
}