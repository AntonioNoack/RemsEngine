package me.anno.network

import me.anno.utils.pooling.ByteBufferPool
import java.io.OutputStream
import java.nio.channels.SocketChannel
import kotlin.math.min

class SocketChannelOutputStream(val server: Server?, val self: SocketChannel) : OutputStream() {

    private val buffer = ByteBufferPool.allocateDirect(1 shl 12)

    private fun flushIfFull() {
        if (buffer.remaining() == 0) flush()
    }

    override fun write(b: Int) {
        buffer.put(b.toByte())
        flushIfFull()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        var readPos = off
        var remaining = len
        while (remaining > 0 && server?.run != false) {
            val sliceLength = min(remaining, buffer.remaining())
            buffer.put(b, readPos, sliceLength)
            flushIfFull()

            readPos += sliceLength
            remaining -= sliceLength
        }
    }

    override fun flush() {
        buffer.flip()
        if (self.isConnected) self.write(buffer)
        buffer.position(0).limit(buffer.capacity())
    }

    override fun close() {
        flush()
        self.close()
    }

    companion object {
        fun SocketChannel.getOutputStream(server: Server?): OutputStream {
            return SocketChannelOutputStream(server, this)
        }
    }
}