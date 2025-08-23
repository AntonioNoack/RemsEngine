package me.anno.network

import me.anno.utils.Sleep
import me.anno.utils.pooling.ByteBufferPool
import java.io.InputStream
import java.nio.channels.SocketChannel
import kotlin.math.max
import kotlin.math.min

class SocketChannelInputStream(val server: Server?, val self: SocketChannel, capacity: Int) : InputStream() {

    private var eof = false
    private val buffer = ByteBufferPool.allocateDirect(capacity)

    init {
        buffer.limit(0)
    }

    private fun readImpl(): Int {
        return buffer.get().toInt() and 0xff
    }

    override fun read(): Int {
        println("reading a byte, ${buffer.remaining()} remaining [eof: $eof]")
        if (buffer.remaining() > 0) return readImpl()

        while (server?.run != false && !eof) {
            val numRead = tryRead(true)
            if (numRead < 0) return -1
            if (numRead > 0) return readImpl()
        }
        return -1
    }

    private fun tryRead(sleep: Boolean): Int {
        buffer.position(0).limit(buffer.capacity())
        val numRead = self.read(buffer)
        buffer.flip()
        println("read bytes: $numRead, ${(0 until buffer.limit()).map { buffer[it] }}")
        if (numRead < 0) eof = true
        if (numRead == 0 && sleep) Sleep.sleepShortly(true)
        return numRead
    }

    private fun readImpl(dst: ByteArray, off: Int, len: Int): Int {
        val numRead = min(len, buffer.remaining())
        buffer.get(dst, off, numRead)
        return numRead
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        println("reading $len @$off")
        if (buffer.remaining() > 0) return readImpl(b, off, len)

        if (server?.run ?: false && !eof) {
            val numRead = tryRead(false)
            if (numRead < 0) return -1
            if (numRead > 0) return readImpl(b, off, len)
            return 0 // ok here, should we wait in here instead?
        }
        return -1
    }

    override fun available(): Int {
        if (eof) return 0
        return max(1, buffer.remaining())
    }

    override fun close() {
        eof = true
        self.close()
    }

    companion object {
        fun SocketChannel.getInputStream(server: Server?, capacity: Int = 1 shl 12): InputStream {
            return SocketChannelInputStream(server, this, capacity)
        }
    }
}