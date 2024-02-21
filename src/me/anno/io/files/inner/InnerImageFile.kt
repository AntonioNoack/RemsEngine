package me.anno.io.files.inner

import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.image.bmp.BMPWriter
import me.anno.image.bmp.BMPWriter.createBMP
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.utils.structures.tuples.IntPair
import java.io.ByteArrayInputStream
import java.io.InputStream

class InnerImageFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    val content: Image
) : InnerFile(absolutePath, relativePath, false, parent), ImageReadable, SignatureFile {

    override var signature: Signature? = Signature.bmp

    @Suppress("unused")
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

    val bytes by lazy {
        createBMP(content)
    }

    override fun readCPUImage(): Image = content
    override fun readGPUImage(): Image = content
    override fun readSize(): IntPair = IntPair(content.width, content.height)

    override fun readBytesSync(): ByteArray = bytes
    override fun readTextSync(): String = bytes.decodeToString() // what are you doing? ;)
    override fun inputStreamSync(): InputStream = ByteArrayInputStream(bytes)
}