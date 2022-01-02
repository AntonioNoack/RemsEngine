package me.anno.image.webp

import java.nio.ByteBuffer

class BitReader(val buffer: ByteBuffer) {
    var remaining = 0
    var data = 0L
    fun read1(): Boolean = read(1) != 0
    fun read(n: Int): Int {
        while (n > remaining) {
            remaining += 8
            data = (data shl 8) + buffer.get()
        }
        val delta = remaining - n
        val mask = ((1 shl n) - 1)
        val value = (data shr delta).toInt() and mask
        remaining -= n
        return value
    }
}