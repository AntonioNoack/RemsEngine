package me.anno.utils.pooling

open class FloatArrayPool(val size: Int, val exactMatchesOnly: Boolean) {

    val available = arrayOfNulls<FloatArray>(size)
    operator fun get(size: Int): FloatArray {
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
        return FloatArray(size)
    }

    fun returnBuffer(buffer: FloatArray?) {
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

    operator fun plusAssign(buffer: FloatArray?) {
        returnBuffer(buffer)
    }

}