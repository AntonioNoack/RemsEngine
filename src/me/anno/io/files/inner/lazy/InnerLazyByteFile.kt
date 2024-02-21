package me.anno.io.files.inner.lazy

import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.SignatureFile
import java.io.ByteArrayInputStream
import java.io.InputStream

class InnerLazyByteFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    val content: Lazy<ByteArray>
) : InnerFile(absolutePath, relativePath, false, parent), SignatureFile {

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
    override fun readTextSync(): String = readBytesSync().decodeToString()
    override fun inputStreamSync(): InputStream = ByteArrayInputStream(readBytesSync())
}