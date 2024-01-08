package me.anno.graph.hdb

import me.anno.graph.hdb.allocator.size
import java.io.ByteArrayInputStream

class ByteSlice(val bytes: ByteArray, val range: IntRange) {
    constructor(bytes: ByteArray) : this(bytes, bytes.indices)

    val size get() = range.size
    fun stream(): ByteArrayInputStream {
        return ByteArrayInputStream(bytes, range.first, range.size)
    }

    fun getAsString(): String {
        return bytes.decodeToString(range.first, range.last + 1)
    }
}