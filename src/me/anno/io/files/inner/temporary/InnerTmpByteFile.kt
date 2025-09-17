package me.anno.io.files.inner.temporary

import me.anno.utils.async.Callback
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

class InnerTmpByteFile(private var content: ByteArray, ext: String = "bin") : InnerTmpFile(ext) {

    override fun length(): Long = content.size.toLong()

    override fun readBytes(callback: Callback<ByteArray>) {
        callback.ok(content)
    }

    override fun readText(callback: Callback<String>) {
        callback.ok(content.decodeToString())
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.ok(content.inputStream())
    }

    override fun outputStream(append: Boolean): OutputStream {
        return object : ByteArrayOutputStream() {
            override fun close() {
                super.close()

                val writtenBytes = toByteArray()
                if (append) content += writtenBytes
                else content = writtenBytes
                markAsModified()
            }
        }
    }

    override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
        content = if (offset != 0 || length != bytes.size) {
            bytes.copyOfRange(offset, offset + length)
        } else bytes
        markAsModified()
    }
}