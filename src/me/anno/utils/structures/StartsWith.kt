package me.anno.utils.structures

object StartsWith {

    fun ByteArray.startsWith(other: ByteArray): Boolean {
        if (size < other.size) return false
        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun CharArray.startsWith(other: CharArray): Boolean {
        if (size < other.size) return false
        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun ShortArray.startsWith(other: ShortArray): Boolean {
        if (size < other.size) return false
        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun IntArray.startsWith(other: IntArray): Boolean {
        if (size < other.size) return false
        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun LongArray.startsWith(other: LongArray): Boolean {
        if (size < other.size) return false
        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun <V> Array<V>.startsWith(other: Array<V>): Boolean {
        if (size < other.size) return false
        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun <V> List<V>.startsWith(other: List<V>): Boolean {
        if (size < other.size) return false
        for (i in 0 until size) {
            if (this[i] != other[i]) return false
        }
        return true
    }

}