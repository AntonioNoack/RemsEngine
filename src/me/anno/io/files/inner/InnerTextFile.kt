package me.anno.io.files.inner

import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import java.io.InputStream

class InnerTextFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    var content: String
) : InnerFile(absolutePath, relativePath, false, parent) {

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.ok(content.byteInputStream())
    }

    override fun readBytes(callback: Callback<ByteArray>) {
        callback.ok(content.encodeToByteArray())
    }

    override fun readText(callback: Callback<String>) {
        callback.ok(content)
    }

    override fun length(): Long {
        return content.length.toLong() // a guess
    }
}