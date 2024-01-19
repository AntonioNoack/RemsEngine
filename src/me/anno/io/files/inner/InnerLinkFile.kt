package me.anno.io.files.inner

import me.anno.io.files.FileReference
import java.io.InputStream
import java.nio.ByteBuffer

class InnerLinkFile(
    absolutePath: String,
    relativePath: String,
    _parent: FileReference,
    val link: FileReference
) : InnerFile(absolutePath, relativePath, link.isDirectory, _parent) {

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
    override fun inputStream(lengthLimit: Long, callback: (it: InputStream?, exc: Exception?) -> Unit) =
        link.inputStream(lengthLimit, callback)

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) =
        link.inputStream(Long.MAX_VALUE, callback)

    override fun readBytesSync(): ByteArray = link.readBytesSync()
    override fun readTextSync(): String = link.readTextSync()
    override fun readByteBufferSync(native: Boolean): ByteBuffer = link.readByteBufferSync(native)
    override fun readText(callback: (String?, Exception?) -> Unit) = link.readText(callback)
    override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) = link.readBytes(callback)
    override fun readByteBuffer(native: Boolean, callback: (ByteBuffer?, Exception?) -> Unit) =
        link.readByteBuffer(native, callback)

    override fun listChildren() = link.listChildren()
    override fun length() = link.length()

    override val exists: Boolean = link.exists
}