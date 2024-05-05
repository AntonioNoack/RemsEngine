package me.anno.io.zip.internal

import com.github.junrar.io.IReadOnlyAccess

class RarReadOnlyAccess(val bytes: ByteArray) : IReadOnlyAccess {

    private var pos = 0
    override fun setPosition(p0: Long) {
        pos = p0.toInt()
    }

    override fun getPosition(): Long = pos.toLong()
    override fun read(): Int {
        return if (pos < bytes.size) bytes[pos++].toInt().and(255) else -1
    }

    override fun read(p0: ByteArray, p1: Int, p2: Int): Int {
        if (pos >= bytes.size) return 0
        if (pos + p2 >= bytes.size) {
            // how much is available
            return read(p0, p1, bytes.size - pos)
        }
        for (i in 0 until p2) {
            p0[i + p1] = bytes[pos + i]
        }
        return p2
    }

    override fun readFully(p0: ByteArray, p1: Int): Int {
        return read(p0, p1, p0.size - p1)
    }

    override fun close() {}
}