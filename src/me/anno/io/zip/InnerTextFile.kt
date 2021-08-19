package me.anno.io.zip

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

    override fun getInputStream(): InputStream {
        return content.byteInputStream()
    }

    override fun readText(): String = content
    override fun readText(charset: Charset): String = content
    override fun readBytes(): ByteArray = content.toByteArray()

}