package me.anno.io.zip

import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.image.bmp.BMPWriter
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset

open class InnerLazyImageFile(
    absolutePath: String, relativePath: String, _parent: FileReference,
    image: Lazy<Image>
) : InnerFile(absolutePath, relativePath, false, _parent), ImageReadable, SignatureFile {

    override var signature: Signature? = Signature.bmp

    val image = lazy {
        val image2 = image.value
        image2.source = this
        image2
    }

    init {
        size = Int.MAX_VALUE.toLong() // unknown
        compressedSize = size // unknown until we compress it
    }

    val bytes by lazy {
        BMPWriter.createBMP(readImage())
    }

    override fun readImage(): Image {
        return image.value
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