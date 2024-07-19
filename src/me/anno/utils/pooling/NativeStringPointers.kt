package me.anno.utils.pooling

import me.anno.utils.assertions.assertTrue
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

object NativeStringPointers {

    var allocated = 0L
    val stringBuffers = HashMap<String, ByteBuffer>()

    fun String.ptr(): Long {
        return buffer().ptr()
    }

    /**
     * keeps them in memory, so we don't get any ugly segfaults
     * */
    fun String.buffer(): ByteBuffer {
        return synchronized(stringBuffers) {
            stringBuffers.getOrPut(this) {
                val buffer = MemoryUtil.memUTF8(this)
                allocated += buffer.capacity()
                buffer
            }
        }
    }

    fun ByteBuffer.ptr(): Long {
        assertTrue(isDirect)
        return MemoryUtil.memAddress(this)
    }
}