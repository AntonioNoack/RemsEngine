package me.anno.io.zip

import me.anno.io.files.FileReference
import java.io.InputStream

class InnerStreamFile(
    absolutePath: String, relativePath: String, _parent: FileReference,
    val getStream: () -> InputStream
) : InnerFile(absolutePath, relativePath, false, _parent) {

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

    override fun getInputStream(): InputStream {
        return getStream()
    }

}