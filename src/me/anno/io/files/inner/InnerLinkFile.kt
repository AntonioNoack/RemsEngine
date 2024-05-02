package me.anno.io.files.inner

import me.anno.utils.structures.Callback
import me.anno.io.files.FileReference
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
            data = link.data
            lastModified = link.lastModified
            lastAccessed = link.lastAccessed
            creationTime = link.creationTime
            size = link.size
            compressedSize = link.compressedSize
        }
    }

    override val isSomeKindOfDirectory: Boolean
        get() = link.isSomeKindOfDirectory

    override fun inputStreamSync(): InputStream = link.inputStreamSync()
    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) =
        link.inputStream(lengthLimit, callback)

    override fun readBytesSync(): ByteArray = link.readBytesSync()
    override fun readTextSync(): String = link.readTextSync()
    override fun readByteBufferSync(native: Boolean): ByteBuffer = link.readByteBufferSync(native)
    override fun readText(callback: Callback<String>) = link.readText(callback)
    override fun readBytes(callback: Callback<ByteArray>) = link.readBytes(callback)
    override fun readByteBuffer(native: Boolean, callback: Callback<ByteBuffer>) =
        link.readByteBuffer(native, callback)

    override fun listChildren() = link.listChildren()
    override fun length() = link.length()

    override val exists: Boolean = link.exists
}