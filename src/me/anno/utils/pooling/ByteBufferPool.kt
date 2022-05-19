package me.anno.utils.pooling

import me.anno.Engine
import org.lwjgl.system.MemoryUtil
import java.nio.*
import java.util.concurrent.atomic.AtomicLong

/**
 * there should be only a few instances of this
 * which cache byte buffers for a similar purpose (e.g. textures or audio buffers)
 *
 * the instances should be regularly checked whether they are still in use
 * */
open class ByteBufferPool(val size: Int, var entryTimeoutNanos: Long = 5_000_000_000L) {

    fun freeUnusedEntries() {
        val time = Engine.gameTime
        for (i in 0 until size) {
            if (available[i] != null && time > lastUsed[i] + entryTimeoutNanos) {
                free(i)
            }
        }
    }

    private fun free(entryIndex: Int) {
        synchronized(this) {
            val buffer = available[entryIndex]
            if (buffer != null) {
                available[entryIndex] = null
                lastUsed[entryIndex] = 0L
                MemoryUtil.memFree(buffer)
                allocated.addAndGet(-buffer.capacity().toLong())
            }
        }
    }

    val available = arrayOfNulls<ByteBuffer>(size)
    val lastUsed = LongArray(size)

    operator fun get(size: Int, clear: Boolean, exactMatchesOnly: Boolean): ByteBuffer {
        val maxSize = if (exactMatchesOnly) size else size * 2 - 1
        synchronized(this) {
            for (i in 0 until this.size) {
                val candidate = available[i]
                if (candidate != null) {
                    val candidateSize = candidate.capacity()
                    if (candidateSize in size..maxSize) {
                        available[i] = null
                        if (clear) {
                            for (j in 0 until size) {
                                candidate.put(j, 0)
                            }
                        }
                        candidate.position(0)
                        candidate.limit(size)
                        return candidate
                    }
                }
            }
        }
        // this can cause a segfault
        // ByteBuffer.allocateDirect(size)
        // while that one is safe :)
        val bytes = MemoryUtil.memAlloc(size)
            .order(ByteOrder.nativeOrder())
        allocated.addAndGet(bytes.capacity().toLong())
        return bytes
    }

    // these cause issues, so always keep the original bytebuffer
    /* fun returnBuffer(buffer: DoubleBuffer?){
         buffer ?: return
         returnBuffer(MemoryUtil.memByteBuffer(buffer))
     }

     fun returnBuffer(buffer: FloatBuffer?){
         buffer ?: return
         returnBuffer(MemoryUtil.memByteBuffer(buffer))
     }

     fun returnBuffer(buffer: IntBuffer?){
         buffer ?: return
         returnBuffer(MemoryUtil.memByteBuffer(buffer))
     }

     fun returnBuffer(buffer: ShortBuffer?){
         buffer ?: return
         returnBuffer(MemoryUtil.memByteBuffer(buffer))
     }*/

    fun returnBuffer(buffer: ByteBuffer?) {
        buffer ?: return
        synchronized(this) {
            // security checks, could be disabled in the final build
            for (i in 0 until size) {
                if (available[i] === buffer) {
                    throw IllegalStateException("You must not return a buffer twice!")
                }
            }
            // find a free spot
            for (i in 0 until size) {
                if (available[i] == null) {
                    available[i] = buffer
                    lastUsed[i] = Engine.gameTime
                    return
                }
            }
            // there is no free spot -> just free the buffer
            MemoryUtil.memFree(buffer)
            allocated.addAndGet(-buffer.capacity().toLong())
        }
    }

    companion object {

        /**
         * memory that was allocated on the c side; is not counted by the JVM
         * */
        fun getAllocated(): Long = allocated.get()

        private val allocated = AtomicLong(0L)

        fun allocateDirect(size: Int): ByteBuffer {
            val bytes = MemoryUtil.memAlloc(size)
                .order(ByteOrder.nativeOrder())
            allocated.addAndGet(bytes.capacity().toLong())
            return bytes
        }

        fun free(buffer: ByteBuffer?) {
            buffer ?: return
            MemoryUtil.memFree(buffer)
            allocated.addAndGet(-buffer.capacity().toLong())
        }

        fun free(buffer: ShortBuffer?) {
            buffer ?: return
            MemoryUtil.memFree(buffer)
            allocated.addAndGet(-buffer.capacity().toLong() * 2L)
        }

        fun free(buffer: IntBuffer?) {
            buffer ?: return
            MemoryUtil.memFree(buffer)
            allocated.addAndGet(-buffer.capacity().toLong() * 4L)
        }

        fun free(buffer: FloatBuffer?) {
            buffer ?: return
            MemoryUtil.memFree(buffer)
            allocated.addAndGet(-buffer.capacity().toLong() * 4L)
        }

    }

}