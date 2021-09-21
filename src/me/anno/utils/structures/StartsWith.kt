package me.anno.utils.structures

object StartsWith {

    fun ByteArray.startsWith(other: ByteArray, startIndex: Int = 0, endIndex: Int = other.size): Boolean {
        if (size < other.size) return false
        for (i in startIndex until endIndex) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun CharArray.startsWith(other: CharArray, startIndex: Int = 0, endIndex: Int = other.size): Boolean {
        if (size < other.size) return false
        for (i in startIndex until endIndex) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun ShortArray.startsWith(other: ShortArray, startIndex: Int = 0, endIndex: Int = other.size): Boolean {
        if (size < other.size) return false
        for (i in startIndex until endIndex) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun IntArray.startsWith(other: IntArray, startIndex: Int = 0, endIndex: Int = other.size): Boolean {
        if (size < other.size) return false
        for (i in startIndex until endIndex) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun LongArray.startsWith(other: LongArray, startIndex: Int = 0, endIndex: Int = other.size): Boolean {
        if (size < other.size) return false
        for (i in startIndex until endIndex) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun <V> Array<V>.startsWith(other: Array<V>, startIndex: Int = 0, endIndex: Int = other.size): Boolean {
        if (size < other.size) return false
        for (i in startIndex until endIndex) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    fun <V> List<V>.startsWith(other: List<V>, startIndex: Int = 0, endIndex: Int = other.size): Boolean {
        if (size < other.size) return false
        for (i in startIndex until endIndex) {
            if (this[i] != other[i]) return false
        }
        return true
    }

}