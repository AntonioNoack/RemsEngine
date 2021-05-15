package me.anno.gpu.pooling

import java.nio.ByteBuffer

class ByteBufferPool(val size: Int) {

    val available = arrayOfNulls<ByteBuffer>(size)
    fun get(size: Int): ByteBuffer {
        for (i in 0 until this.size) {
            val candidate = available[i]
            if (candidate != null && candidate.capacity() >= size) {
                available[i] = null
                candidate.position(0)
                return candidate
            }
        }
        return ByteBuffer.allocateDirect(size)
    }

    fun returnBuffer(buffer: ByteBuffer?) {
        buffer ?: return
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