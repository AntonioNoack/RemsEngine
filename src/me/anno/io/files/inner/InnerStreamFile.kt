package me.anno.io.files.inner

import me.anno.io.files.FileReference
import java.io.InputStream

open class InnerStreamFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    val getStream: () -> InputStream
) : InnerFile(absolutePath, relativePath, false, parent) {

    @Suppress("unused")
    constructor(folder: InnerFolder, name: String, getStream: () -> InputStream) : this(
        "${folder.absolutePath}/$name",
        "${folder.relativePath}/$name",
        folder,
        getStream
    )

    init {
        // unknown...
        size = 100000
        compressedSize = size
    }

    override fun inputStreamSync(): InputStream = getStream()
    override fun readBytesSync(): ByteArray = inputStreamSync().readBytes()
    override fun readTextSync(): String = readBytesSync().decodeToString()
}