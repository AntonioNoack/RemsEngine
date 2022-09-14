package me.anno.io.zip

import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.image.bmp.BMPWriter
import me.anno.image.bmp.BMPWriter.createBMP
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
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

    val bytes = lazy {
        createBMP(content.createIntImage())
    }

    override fun readImage(): Image {
        return content
    }

    override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
        callback(bytes.value, null)
    }

    override fun readBytesSync(): ByteArray {
        return bytes.value
    }

    override fun readText(charset: Charset, callback: (String?, Exception?) -> Unit) {
        callback(String(bytes.value, charset), null)
    }

    override fun readTextSync(): String {
        return String(bytes.value) // what are you doing? ;)
    }

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        callback(bytes.value.inputStream(), null)
    }

    override fun inputStreamSync(): InputStream {
        return bytes.value.inputStream()
    }

}