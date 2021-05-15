package me.anno.gpu.pooling

class ByteArrayPool(val size: Int) {

    val available = arrayOfNulls<ByteArray>(size)
    operator fun get(size: Int): ByteArray {
        for (i in 0 until this.size) {
            val candidate = available[i]
            if (candidate != null && candidate.size >= size) {
                available[i] = null
                return candidate
            }
        }
        return ByteArray(size)
    }

    fun returnBuffer(buffer: ByteArray?) {
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

    operator fun plusAssign(buffer: ByteArray?){
        returnBuffer(buffer)
    }

}