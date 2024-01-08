package me.anno.io.files.inner.temporary

import java.io.ByteArrayInputStream
import java.io.InputStream

@Suppress("unused")
class InnerTmpTextFile(text: String, ext: String = "txt") : InnerTmpFile(ext) {

    var text: String = text
        set(value) {
            field = value
            val size = value.length.toLong()
            this.size = size
            this.compressedSize = size
        }

    init {
        size = text.length.toLong()
        compressedSize = size
    }

    override fun readTextSync() = text
    override fun readBytesSync() = text.encodeToByteArray()
    override fun inputStreamSync() = ByteArrayInputStream(readBytesSync())

    override fun readText(callback: (String?, Exception?) -> Unit) {
        callback(readTextSync(), null)
    }

    override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
        callback(readBytesSync(), null)
    }

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        callback(inputStreamSync(), null)
    }
}