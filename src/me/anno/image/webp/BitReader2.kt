package me.anno.image.webp

class BitReader2(val buffer: IntArray, var offset: Int) {
    var remaining = 0
    var data = 0L
    fun read1(): Boolean = read(1) != 0
    fun read(n: Int): Int {
        while (n > remaining) {
            remaining += 32
            data = (data shl 32) + buffer[offset++]
        }
        val delta = remaining - n
        val mask = ((1 shl n) - 1)
        val value = (data shr delta).toInt() and mask
        remaining -= n
        return value
    }
}