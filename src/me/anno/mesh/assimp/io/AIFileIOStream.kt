package me.anno.mesh.assimp.io

import me.anno.io.files.FileReference
import org.lwjgl.system.MemoryUtil
import java.io.InputStream
import kotlin.math.min

class AIFileIOStream(val file: FileReference) : IFileIOStream {

    companion object {
        private const val SEEK_SET = 0
        private const val SEEK_CUR = 1
        private const val SEEK_END = 2
    }

    var input: InputStream? = null
    override var position: Long = 0L

    override val length = file.length()

    override fun close() {
        input?.close()
        input = null
    }

    // whence = from where / where to add the offset
    override fun seek(offset: Long, whence: Int): Int {
        ensureInput()
        val target = when (whence) {
            SEEK_SET -> 0L
            SEEK_CUR -> position
            SEEK_END -> file.length()
            else -> throw RuntimeException("Unknown mode $whence")
        } + offset
        val delta = target - position
        if (delta < 0L) throw RuntimeException("Skipping back hasn't yet been implemented")
        val input = input!!
        var done = 0L
        while (done < delta) {
            val skipped = input.skip(delta)
            if (skipped < 0L) return -1 // eof
            done += skipped
        }
        position = target
        // 0 = success
        return 0
    }

    private fun ensureInput() {
        if (input == null) input = file.inputStream()
    }

    override fun read(buffer: Long, size: Long, count: Long): Long {
        ensureInput()
        val readableItems = min(count, (length - position) / size)
        return if (readableItems > 0) {
            val numBytes = readableItems * size
            var writePtr = buffer
            for (i in 0 until numBytes) {
                MemoryUtil.memPutByte(writePtr++, input!!.read().toByte())
            }
            this.position += numBytes
            readableItems
        } else 0
    }

}