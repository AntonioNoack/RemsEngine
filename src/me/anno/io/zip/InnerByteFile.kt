package me.anno.io.zip

import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import java.io.IOException
import java.io.InputStream

class InnerByteFile(absolutePath: String, relativePath: String, parent: FileReference, content: ByteArray) :
    InnerFile(absolutePath, relativePath, false, parent), SignatureFile {

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

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        throw IOException("Missing data")
    }

}