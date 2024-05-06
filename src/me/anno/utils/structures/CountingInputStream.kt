package me.anno.utils.structures

import java.io.EOFException
import java.io.FilterInputStream
import java.io.InputStream

/**
 * counts how many bytes have been read already
 * */
class CountingInputStream(src: InputStream) : FilterInputStream(src) {

    var position = 0L
        private set

    override fun read(): Int {
        val b = super.read()
        if (b != -1) position++
        return b
    }

    override fun read(buffer: ByteArray, off: Int, len: Int): Int {
        val r = super.read(buffer, off, len)
        if (r > 0) position += r
        return r
    }

    override fun skip(n: Long): Long {
        var done = 0L
        while (done < n) {
            val skipped = super.skip(n - done)
            @Suppress("KotlinConstantConditions")
            when {
                skipped < 0L -> throw EOFException() // should not happen
                skipped == 0L -> {
                    val sample = read()
                    if (sample < 0) throw EOFException()
                    done++
                }
                else -> done += skipped
            }
        }
        position += done.toInt()
        return done
    }
}