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
    val cpuImage: Lazy<Image>, val gpuImage: () -> Image,
) : InnerFile(absolutePath, relativePath, false, _parent), ImageReadable, SignatureFile {

    override var signature: Signature? = Signature.bmp

    override fun hasInstantGPUImage(): Boolean {
        return false
    }

    override fun hasInstantCPUImage(): Boolean {
        return cpuImage.isInitialized() && super.hasInstantCPUImage()
    }

    init {
        size = Int.MAX_VALUE.toLong() // unknown
        compressedSize = size // unknown until we compress it
    }

    val bytes by lazy {
        BMPWriter.createBMP(readCPUImage())
    }

    override fun readCPUImage(): Image {
        return cpuImage.value
    }

    override fun readGPUImage(): Image {
        return gpuImage()
    }

    override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
        callback(readBytesSync(), null)
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