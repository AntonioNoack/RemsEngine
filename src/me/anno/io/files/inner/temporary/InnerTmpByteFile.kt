package me.anno.io.files.inner.temporary

import me.anno.utils.async.Callback
import java.io.InputStream

class InnerTmpByteFile(private var content: ByteArray, ext: String = "bin") : InnerTmpFile(ext) {
    override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
        content = if (offset != 0 || length != bytes.size) {
            bytes.copyOfRange(offset, offset + length)
        } else bytes
    }

    override fun length(): Long {
        return content.size.toLong()
    }

    override fun readBytes(callback: Callback<ByteArray>) {
        callback.ok(content)
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.ok(content.inputStream())
    }

    override fun readText(callback: Callback<String>) {
        callback.ok(content.decodeToString())
    }
}