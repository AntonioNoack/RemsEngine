package me.anno.io.zip

import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.image.bmp.BMPWriter
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
        size = BMPWriter.calculateSize(content) // very simple calculation
        compressedSize = size // unknown until we compress it
    }

    val bytes = lazy {
        createBMP(content.createIntImage())
    }

    override fun readImage(): Image {
        return content
    }

    override fun readBytes(): ByteArray {
        return bytes.value
    }

    override fun readText(): String {
        return String(bytes.value) // what are you doing? ;)
    }

    override fun getInputStream(): InputStream {
        return bytes.value.inputStream()
    }

}