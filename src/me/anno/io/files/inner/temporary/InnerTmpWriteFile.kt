package me.anno.io.files.inner.temporary

import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFile
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import kotlin.math.min

class InnerTmpWriteFile(name: String) : InnerFile(name, name, false, InvalidRef) {
    val written = ByteArrayOutputStream()
    override fun outputStream(append: Boolean): OutputStream {
        return written
    }

    override fun writeBytes(bytes: ByteArray) {
        written.write(bytes)
    }

    override fun writeBytes(bytes: ByteBuffer) {
        val tmp = ByteArray(min(bytes.remaining(), 1024))
        val pos = bytes.position()
        while (true) {
            val length = min(bytes.remaining(), tmp.size)
            if (length == 0) break
            bytes.get(tmp)
            written.write(tmp, 0, length)
        }
        bytes.position(pos)
    }
}