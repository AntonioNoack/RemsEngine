package me.anno.io.zip

import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import java.io.InputStream

class InnerLazyByteFile(
    absolutePath: String, relativePath: String, _parent: FileReference,
    val content: Lazy<ByteArray>
) : InnerFile(absolutePath, relativePath, false, _parent), SignatureFile {

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
        set(value) {}

    override fun readBytes(): ByteArray {
        return content.value
    }

    override fun readText(): String {
        return String(content.value)
    }

    override fun getInputStream(): InputStream {
        return content.value.inputStream()
    }

}