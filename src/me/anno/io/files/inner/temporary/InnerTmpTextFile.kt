package me.anno.io.files.inner.temporary

import java.io.ByteArrayInputStream

@Suppress("unused")
class InnerTmpTextFile(text: String, ext: String = "txt") : InnerTmpFile(ext) {

    var text: String = text
        private set

    init {
        writeText(text)
    }

    override fun writeText(text: String, offset: Int, length: Int) {
        this.text = text.substring(offset,length)
        size = length.toLong()
        compressedSize = size
    }

    override fun readTextSync() = text
    override fun readBytesSync() = text.encodeToByteArray()
    override fun inputStreamSync() = ByteArrayInputStream(readBytesSync())
}