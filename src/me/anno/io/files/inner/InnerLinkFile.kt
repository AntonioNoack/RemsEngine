package me.anno.io.files.inner

import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import java.io.InputStream
import java.nio.ByteBuffer

class InnerLinkFile(
    absolutePath: String,
    relativePath: String,
    parent: FileReference,
    val link: FileReference
) : InnerFile(absolutePath, relativePath, link.isDirectory, parent) {

    constructor(folder: InnerFolder, name: String, content: FileReference) : this(
        "${folder.absolutePath}/$name",
        "${folder.relativePath}/$name",
        folder,
        content
    )

    init {
        if (link is InnerFile) {
            lastModified = link.lastModified
            lastAccessed = link.lastAccessed
            creationTime = link.creationTime
        }
    }

    override val isSomeKindOfDirectory: Boolean
        get() = link.isSomeKindOfDirectory

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) =
        link.inputStream(lengthLimit, callback)
    override fun readText(callback: Callback<String>) = link.readText(callback)
    override fun readBytes(callback: Callback<ByteArray>) = link.readBytes(callback)
    override fun readByteBuffer(native: Boolean, callback: Callback<ByteBuffer>) =
        link.readByteBuffer(native, callback)

    override fun listChildren() = link.listChildren()
    override fun length() = link.length()

    override val exists: Boolean = link.exists
}