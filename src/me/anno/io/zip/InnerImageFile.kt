package me.anno.io.zip

import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.image.bmp.BMPWriter
import me.anno.image.bmp.BMPWriter.createBMP
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset

class InnerImageFile(
    absolutePath: String, relativePath: String, _parent: FileReference,
    val content: Image
) : InnerFile(absolutePath, relativePath, false, _parent), ImageReadable, SignatureFile {

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

    override fun readImage(): Image {
        return content
    }

    override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
        callback(bytes, null)
    }

    override fun readBytesSync(): ByteArray {
        return bytes
    }

    override fun readText(charset: Charset, callback: (String?, Exception?) -> Unit) {
        callback(String(bytes, charset), null)
    }

    override fun readTextSync(): String {
        return String(bytes) // what are you doing? ;)
    }

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        callback(inputStreamSync(), null)
    }

    override fun inputStreamSync(): InputStream {
        return ByteArrayInputStream(bytes)
    }

}