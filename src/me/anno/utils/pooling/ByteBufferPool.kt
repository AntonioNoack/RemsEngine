package me.anno.utils.pooling

import java.nio.ByteBuffer

open class ByteBufferPool(val size: Int, val exactMatchesOnly: Boolean) {

    val available = arrayOfNulls<ByteBuffer>(size)
    operator fun get(size: Int, clear: Boolean): ByteBuffer {
        val maxSize = if (exactMatchesOnly) size else size * 2
        synchronized(this) {
            for (i in 0 until this.size) {
                val candidate = available[i]
                if (candidate != null) {
                    val candidateSize = candidate.capacity()
                    if (candidateSize in size..maxSize) {
                        available[i] = null
                        candidate.position(0)
                        if (clear) {
                            for (j in 0 until size) {
                                candidate.put(j, 0)
                            }
                        }
                        return candidate
                    }
                }
            }
        }
        return ByteBuffer.allocateDirect(size)
    }

    fun returnBuffer(buffer: ByteBuffer?) {
        buffer ?: return
        synchronized(this) {
            for (i in 0 until size) {
                if (available[i] === buffer) return // mmh
            }
            for (i in 0 until size) {
                if (available[i] == null) {
                    available[i] = buffer
                    return
                }
            }
            val index = (Math.random() * size).toInt()
            available[index % size] = buffer
        }
    }

}