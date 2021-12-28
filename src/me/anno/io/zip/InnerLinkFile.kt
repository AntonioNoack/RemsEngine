package me.anno.io.zip

import me.anno.io.files.FileReference

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

    override val isSomeKindOfDirectory: Boolean
        get() = link.isSomeKindOfDirectory

    override fun getInputStream() = link.inputStream()

    override fun readBytes() = link.readBytes()
    override fun readText() = link.readText()

    override fun length() = link.length()

}