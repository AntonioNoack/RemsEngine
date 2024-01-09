package me.anno.mesh.assimp.io

import me.anno.mesh.assimp.io.IFileIOStream.Companion.SEEK_CUR
import me.anno.mesh.assimp.io.IFileIOStream.Companion.SEEK_END
import me.anno.mesh.assimp.io.IFileIOStream.Companion.SEEK_SET
import org.lwjgl.system.MemoryUtil
import kotlin.math.min

class AIFileIOStream2(val bytes: ByteArray) : IFileIOStream {

    override var position: Long = 0L

    override val length = bytes.size.toLong()

    override fun close() {}

    // whence = from where / where to add the offset
    override fun seek(offset: Long, whence: Int): Int {
        val target = when (whence) {
            SEEK_SET -> 0L
            SEEK_CUR -> position
            SEEK_END -> length
            else -> throw RuntimeException("Unknown mode $whence")
        } + offset
        position = target
        // 0 = success
        return 0
    }

    override fun read(buffer: Long, size: Long, count: Long): Long {
        val readableItems = min(count, (length - position) / size)
        return if (readableItems > 0) {
            val numBytes = readableItems * size
            var writePtr = buffer
            var readPtr = position.toInt()
            for (i in 0 until numBytes) {
                MemoryUtil.memPutByte(writePtr++, bytes[readPtr++])
            }
            this.position += numBytes
            readableItems
        } else 0
    }

}