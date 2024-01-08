package me.anno.io.files.inner.temporary

import java.io.ByteArrayInputStream
import java.io.InputStream

@Suppress("unused")
class InnerTmpByteFile(bytes: ByteArray, ext: String = "bin") : InnerTmpFile(ext) {

    var bytes: ByteArray = bytes
        set(value) {
            field = value
            val size = value.size.toLong()
            this.size = size
            this.compressedSize = size
        }

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        callback(ByteArrayInputStream(bytes), null)
    }

    override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
        callback(bytes, null)
    }

    override fun readBytesSync() = bytes
}