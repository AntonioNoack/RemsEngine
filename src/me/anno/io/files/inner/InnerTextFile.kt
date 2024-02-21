package me.anno.io.files.inner

import me.anno.io.files.FileReference
import java.io.ByteArrayInputStream

class InnerTextFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    var content: String
) : InnerFile(absolutePath, relativePath, false, parent) {

    init {
        size = content.length.toLong()
        compressedSize = size
    }

    override fun inputStreamSync() = ByteArrayInputStream(readBytesSync())
    override fun readBytesSync() = content.encodeToByteArray()
    override fun readTextSync() = content
}