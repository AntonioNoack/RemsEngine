package me.anno.io.zip

import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import java.io.InputStream
import java.nio.charset.Charset

class InnerLazyByteFile(
    absolutePath: String, relativePath: String, _parent: FileReference,
    val content: Lazy<ByteArray>
) : InnerFile(absolutePath, relativePath, false, _parent), SignatureFile {

    @Suppress("unused")
    constructor(folder: InnerFolder, name: String, content: Lazy<ByteArray>) : this(
        "${folder.absolutePath}/$name",
        "${folder.relativePath}/$name",
        folder,
        content
    )

    init {
        size = Int.MAX_VALUE.toLong()
        compressedSize = size
    }

    override var signature: Signature?
        get() = Signature.find(content.value)
        set(_) {}

    override fun readBytesSync() = content.value
    override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
        callback(content.value, null)
    }

    override fun readTextSync(): String {
        return String(content.value)
    }

    override fun readText(charset: Charset, callback: (String?, Exception?) -> Unit) {
        callback(String(content.value), null)
    }

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        callback(content.value.inputStream(), null)
    }

}