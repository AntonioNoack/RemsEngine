package me.anno.io.zip

import me.anno.io.files.FileReference
import java.io.InputStream

class InnerLinkFile(
    absolutePath: String, relativePath: String, _parent: FileReference,
    val link: FileReference
) : InnerFile(absolutePath, relativePath, false, _parent) {

    constructor(folder: InnerFolder, name: String, content: FileReference) : this(
        "${folder.absolutePath}/$name",
        "${folder.relativePath}/$name",
        folder,
        content
    )

    override fun getInputStream(): InputStream {
        return link.inputStream()
    }

}