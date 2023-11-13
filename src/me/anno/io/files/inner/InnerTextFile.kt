package me.anno.io.files.inner

import me.anno.io.files.FileReference
import java.io.InputStream
import java.nio.charset.Charset

class InnerTextFile(
    absolutePath: String, relativePath: String, _parent: FileReference,
    var content: String
) : InnerFile(absolutePath, relativePath, false, _parent) {

    init {
        size = content.length.toLong()
        compressedSize = size
    }

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        callback(inputStreamSync(), null)
    }

    override fun inputStreamSync() = content.byteInputStream()

    override fun readText(charset: Charset, callback: (String?, Exception?) -> Unit) {
        callback(content, null)
    }

    override fun readTextSync() = content

    override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
        callback(readBytesSync(), null)
    }

    override fun readBytesSync() = content.toByteArray()
}