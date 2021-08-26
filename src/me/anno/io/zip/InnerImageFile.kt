package me.anno.io.zip

import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.image.bmp.BMPWriter.createBMP
import me.anno.io.files.FileReference
import java.io.InputStream

class InnerImageFile(
    absolutePath: String, relativePath: String, _parent: FileReference,
    val content: Image
) : InnerFile(absolutePath, relativePath, false, _parent), ImageReadable {

    constructor(folder: InnerFolder, name: String, content: Image) : this(
        "${folder.absolutePath}/$name",
        "${folder.relativePath}/$name",
        folder,
        content
    )

    init {
        size = Int.MAX_VALUE.toLong()
        compressedSize = size
    }

    val bytes = lazy {
        createBMP(content.createBufferedImage())
    }

    override fun readImage(): Image {
        return content
    }

    override fun readBytes(): ByteArray {
        return bytes.value
    }

    override fun readText(): String {
        return String(bytes.value)
    }

    override fun getInputStream(): InputStream {
        return bytes.value.inputStream()
    }

}