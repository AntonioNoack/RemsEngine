package me.anno.utils.pooling

import me.anno.Time
import me.anno.ecs.annotations.Docs
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.InternalAPI
import me.anno.utils.assertions.assertGreaterThanEquals
import me.anno.utils.structures.lists.Lists.arrayListOfNulls
import me.anno.utils.types.Ints.isPowerOf2
import org.apache.logging.log4j.LogManager
import speiger.primitivecollections.IntToObjectHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

abstract class BufferPool<V>(
    val size: Int,
    val elementSize: Int,
    val timeoutMillis: Long = 250L // idk what would be good...
) {
    companion object {
        private val LOGGER = LogManager.getLogger(BufferPool::class)
        private val DO_NOTHING: (Any?) -> Unit = {}
    }

    private val allocated = AtomicLong()
    private val available = arrayListOfNulls<V?>(size)
    private val lastUsed = LongArray(size)
    private val smallSizes = IntToObjectHashMap<ObjectPool<V>>()

    open fun prepare(buffer: V, size: Int) {}

    // functions for child classes, not to be used by any external class!!!
    @InternalAPI
    abstract fun implClear(buffer: V, size: Int)

    @InternalAPI
    abstract fun implCopyTo(src: V, dst: V, size: Int)

    @InternalAPI
    abstract fun implGetSize(buffer: V): Int

    @InternalAPI
    abstract fun implCreateBuffer(size: Int): V

    @InternalAPI
    open fun implDestroy(buffer: V) {
    }

    private fun safeCreateBuffer(size: Int): V {
        allocated.addAndGet(size.toLong() * elementSize)
        return implCreateBuffer(size)
    }

    fun grow(buffer: V, newSize: Int): V {
        return if (implGetSize(buffer) < newSize) resizeTo(buffer, newSize) else buffer
    }

    fun shrink(buffer: V, newSize: Int): V {
        return if (implGetSize(buffer) > newSize) resizeTo(buffer, newSize) else buffer
    }

    fun resizeTo(buffer: V, newSize: Int): V {
        val oldSize = implGetSize(buffer)
        return if (oldSize != newSize) {
            val newBuffer = get(newSize, clear = false, exactMatchesOnly = true)
            implCopyTo(buffer, newBuffer, min(oldSize, newSize))
            returnBuffer(buffer)
            newBuffer
        } else buffer
    }

    val freeSize: Long
        get() = freeSizeSmall() + freeSizeBig()

    val totalSize: Long
        get() = allocated.get()

    private fun freeSizeSmall(): Long {
        var sum = 0L
        // lock is required to avoid ConcurrencyExceptions
        synchronized(this) {
            smallSizes.forEach { size, value ->
                sum += size.toLong() * value.size
            }
        }
        return sum * elementSize
    }

    private fun freeSizeBig(): Long {
        var sum = 0L
        // no lock required
        for (i in available.indices) {
            val element = available[i]
            if (element != null) {
                sum += implGetSize(element)
            }
        }
        return sum * elementSize
    }

    fun isSmallSize(size: Int): Boolean {
        return (size < 32) || (size < 1024 && size.isPowerOf2())
    }

    operator fun get(size: Int, clear: Boolean, exactMatchesOnly: Boolean): V {
        val reused = if (isSmallSize(size)) smallSizes[size]?.create()
        else findBigBuffer(size, exactMatchesOnly)
        if (reused != null) {
            assertGreaterThanEquals(implGetSize(reused), size)
            prepare(reused, size, clear)
            return reused
        } else return safeCreateBuffer(size)
    }

    private fun findBigBuffer(size: Int, exactMatchesOnly: Boolean): V? {
        val maxSize = if (exactMatchesOnly) size else size * 2
        synchronized(this) {
            for (i in 0 until this.size) {
                val candidate = available.getOrNull(i)
                if (candidate != null) {
                    if (implGetSize(candidate) in size..maxSize) {
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
            implClear(candidate, size)
        }
    }

    /**
     * Return the buffer back, so it can be used by whoever wants it next.
     * buffer must not be used after that.
     * Returns null, so it can be used with the following style:
     * container.data = Pool.returnBuffer(container.data)
     * */
    fun returnBuffer(buffer: V?): Nothing? {
        if (buffer != null) {
            val size = implGetSize(buffer)
            if (isSmallSize(size)) returnSmallBuffer(buffer, size)
            else returnBigBuffer(buffer)
        }
        return null
    }

    private val freeSmallBufferImpl: (V) -> Unit = { buffer ->
        doDestroy(-1, buffer)
    }

    private fun returnSmallBuffer(buffer: V, size: Int) {
        synchronized(this) {
            smallSizes.getOrPut(size) {
                ObjectPool(
                    { safeCreateBuffer(size) },
                    DO_NOTHING, DO_NOTHING,
                    freeSmallBufferImpl, checkDoubleReturns = true
                )
            }
        }.destroy(buffer)
    }

    private fun returnBigBuffer(buffer: V) {
        synchronized(this) {
            // check whether this buffer was returned already
            for (i in 0 until size) {
                if (available[i] === buffer) {
                    LOGGER.error("Returned big buffer twice!")
                    return
                }
            }
            // find empty slot
            for (i in 0 until size) {
                if (available[i] == null) {
                    available[i] = buffer
                    lastUsed[i] = Time.nanoTime
                    return
                }
            }
            // no slot found -> we need to delete that buffer
            doDestroy(-1, buffer)
        }
    }

    private fun doDestroy(i: Int, element: V) {
        allocated.addAndGet(-implGetSize(element).toLong() * elementSize)
        implDestroy(element)
        if (i >= 0) available[i] = null
    }

    fun freeUnusedEntries() {
        synchronized(this) {
            val time = Time.nanoTime
            val timeoutNanos = timeoutMillis * MILLIS_TO_NANOS
            for (i in available.indices) {
                val element = available[i]
                if (element != null && time - lastUsed[i] > timeoutNanos) {
                    doDestroy(i, element)
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
                    doDestroy(i, element)
                }
            }
            for (pool in smallSizes.values) {
                pool.gc()
            }
            smallSizes.clear()
        }
    }
}