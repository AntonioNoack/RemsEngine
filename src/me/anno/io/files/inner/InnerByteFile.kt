package me.anno.io.files.inner

import me.anno.io.files.FileReference
import me.anno.io.files.Signature

class InnerByteFile(absolutePath: String, relativePath: String, parent: FileReference, content: ByteArray) :
    InnerFileWithData(absolutePath, relativePath, parent), SignatureFile {

    override var signature: Signature? = Signature.find(content)

    @Suppress("unused")
    constructor(folder: InnerFolder, name: String, content: ByteArray) : this(
        "${folder.absolutePath}/$name",
        "${folder.relativePath}/$name",
        folder,
        content
    )

    init {
        data = content
        size = content.size.toLong()
        compressedSize = size
    }
}