package net.sf.image4j.io

import java.io.EOFException
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

class CountingInputStream(src: InputStream) : FilterInputStream(src) {

    var count = 0
        private set

    @Throws(IOException::class)
    override fun read(): Int {
        val b = super.read()
        if (b != -1) count++
        return b
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val r = super.read(b, off, len)
        if (r > 0) count += r
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
        count += done.toInt()
        return done
    }

}