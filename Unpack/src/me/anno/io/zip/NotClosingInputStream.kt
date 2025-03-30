package me.anno.io.zip

import java.io.InputStream

class NotClosingInputStream(val input: InputStream) : InputStream() {
    override fun read(): Int = input.read()
    override fun read(p0: ByteArray): Int {
        return input.read(p0)
    }

    override fun read(p0: ByteArray, p1: Int, p2: Int): Int {
        return input.read(p0, p1, p2)
    }

    override fun available(): Int {
        return input.available()
    }

    override fun markSupported(): Boolean {
        return input.markSupported()
    }

    override fun mark(readlimit: Int) {
        input.mark(readlimit)
    }

    override fun reset() {
        input.reset()
    }
}