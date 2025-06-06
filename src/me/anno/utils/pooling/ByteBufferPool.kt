package me.anno.utils.pooling

import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.ShortBuffer
import java.util.concurrent.atomic.AtomicLong

/**
 * there should be only a few instances of this;
 * which cache byte buffers for a similar purpose (e.g., textures or audio buffers)
 *
 * the instances should be regularly checked whether they are still in use
 * */
open class ByteBufferPool(size: Int, timeoutMillis: Long = 5000L) :
    BufferPool<ByteBuffer>(size, 1, timeoutMillis) {

    override fun prepare(buffer: ByteBuffer, size: Int) {
        buffer.position(0).limit(size)
        buffer.order(ByteOrder.nativeOrder())
    }

    override fun implClear(buffer: ByteBuffer, size: Int) {
        for (j in 0 until size) {
            buffer.put(j, 0)
        }
    }

    override fun implGetSize(buffer: ByteBuffer): Int {
        return buffer.capacity()
    }

    override fun implCreateBuffer(size: Int): ByteBuffer {
        return allocateDirect(size)
    }

    override fun implDestroy(buffer: ByteBuffer) {
        free(buffer)
    }

    override fun implCopyTo(src: ByteBuffer, dst: ByteBuffer, size: Int) {
        src.position(0).limit(size)
        dst.position(0).limit(size)
        dst.put(src)
        dst.position(0).limit(dst.capacity())
        src.position(0).limit(src.capacity())
    }

    companion object {

        /**
         * memory that was allocated on the c side; is not counted by the JVM
         * */
        @JvmStatic
        fun getNativeAllocated(): Long = nativeAllocated.get()

        @JvmStatic
        private val nativeAllocated = AtomicLong(0L)

        @JvmStatic
        fun allocateDirect(size: Int): ByteBuffer {
            val bytes = MemoryUtil.memAlloc(size)
                .order(ByteOrder.nativeOrder())
            nativeAllocated.addAndGet(bytes.capacity().toLong())
            return bytes
        }

        @JvmStatic
        fun free(buffer: ByteBuffer?) {
            buffer ?: return
            MemoryUtil.memFree(buffer)
            nativeAllocated.addAndGet(-buffer.capacity().toLong())
        }
    }
}