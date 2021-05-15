package me.anno.utils.pooling

open class ByteArrayPool(val size: Int, val exactMatchesOnly: Boolean) {

    val available = arrayOfNulls<ByteArray>(size)
    operator fun get(size: Int): ByteArray {
        val maxSize = if (exactMatchesOnly) size else size * 2
        synchronized(this) {
            for (i in 0 until this.size) {
                val candidate = available[i]
                if (candidate != null) {
                    if (candidate.size in size..maxSize) {
                        available[i] = null
                        return candidate
                    }
                }
            }
        }
        return ByteArray(size)
    }

    fun returnBuffer(buffer: ByteArray?) {
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

    operator fun plusAssign(buffer: ByteArray?) {
        returnBuffer(buffer)
    }

}