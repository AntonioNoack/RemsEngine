package me.anno.utils.types

import me.anno.utils.structures.lists.Lists.createList

object Arrays {

    @JvmStatic
    fun ByteArray?.resize(size: Int) =
        if (this == null || this.size != size) ByteArray(size) else this

    @JvmStatic
    fun IntArray?.resize(size: Int) =
        if (this == null || this.size != size) IntArray(size) else this

    @JvmStatic
    fun FloatArray?.resize(size: Int) =
        if (this == null || this.size != size) FloatArray(size) else this

    @JvmStatic
    fun LongArray.rotateRight(shift: Int) {
        val wrapAround = LongArray(shift)
        copyInto(wrapAround, 0, size - shift, size)
        copyInto(this, shift, 0, size - shift)
        wrapAround.copyInto(this)
    }

    @JvmStatic
    fun <V> ArrayList<V>.rotateRight(shift: Int) {
        val wrapAround = createList(shift) { this[size - shift + it] }
        copyInto(this, shift, 0, size - shift)
        wrapAround.copyInto(this)
    }

    @JvmStatic
    fun <V> List<V>.copyInto(dst: MutableList<V>, dstI0: Int, srcI0: Int, srcI1: Int) {
        if (srcI0 < dstI0) {
            var dstI = dstI0
            for (i in srcI0 until srcI1) {
                dst[dstI++] = this[i]
            }
        } else {
            var dstI = dstI0 + (srcI1 - srcI0)
            for (i in srcI1 - 1 downTo srcI0) {
                dst[dstI--] = this[i]
            }
        }
    }

    @JvmStatic
    fun <V> List<V>.copyInto(dst: MutableList<V>) {
        copyInto(dst, 0, 0, size)
    }

    fun ByteArray.startsWith(other: ByteArray, i: Int): Boolean {
        if (i < 0 || i > size - other.size) return false
        for (j in other.indices) {
            if (this[i + j] != other[j]) {
                return false
            }
        }
        return true
    }

    fun ByteArray.startsWith(other: String, i: Int): Boolean {
        if (i < 0 || i > size - other.length) return false
        for (j in other.indices) {
            if (this[i + j].toInt() != other[j].code) {
                return false
            }
        }
        return true
    }

    fun ByteArray.indexOf2(other: ByteArray, i0: Int): Int {
        for (i in i0 until size - other.size) {
            if (startsWith(other, i)) {
                return i
            }
        }
        return -1
    }

    fun ByteArray.read(i: Int, default: Int = 0): Int {
        if (i < 0 || i + 1 > size) return default
        return this[i].toInt().and(255)
    }

    fun ByteArray.readLE16(i: Int, default: Int = 0): Int {
        if (i < 0 || i + 2 > size) return default
        return read(i) + read(i + 1).shl(8)
    }

    fun ByteArray.readLE32(i: Int, default: Int = 0): Int {
        if (i < 0 || i + 4 > size) return default
        return readLE16(i) + readLE16(i + 2).shl(16)
    }

    fun ByteArray.readLE64(i: Int, default: Long = 0): Long {
        if (i < 0 || i + 8 > size) return default
        return readLE32(i).toLong().and(0xffffffffL) + readLE32(i + 4).toLong().shl(32)
    }

    fun ByteArray.readLE32F(i: Int, default: Float = 0f): Float {
        if (i < 0 || i + 4 > size) return default
        return Float.fromBits(readLE32(i))
    }

    fun ByteArray.readLE64F(i: Int, default: Double = 0.0): Double {
        if (i < 0 || i + 8 > size) return default
        return Double.fromBits(readLE64(i))
    }
}