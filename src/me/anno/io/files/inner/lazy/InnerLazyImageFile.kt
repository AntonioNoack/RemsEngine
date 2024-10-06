package me.anno.io.files.inner.lazy

import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.image.bmp.BMPWriter
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.SignatureFile
import me.anno.utils.structures.tuples.IntPair
import java.io.ByteArrayInputStream
import java.io.InputStream

open class InnerLazyImageFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    val cpuImage: Lazy<Image>, val gpuImage: () -> Image,
) : InnerFile(absolutePath, relativePath, false, parent), ImageReadable, SignatureFile {

    override var signature: Signature? = Signature.bmp

    override fun hasInstantGPUImage(): Boolean {
        return false
    }

    override fun hasInstantCPUImage(): Boolean {
        return cpuImage.isInitialized() && super.hasInstantCPUImage()
    }

    init {
        size = 2_000_000L // unknown
        compressedSize = size // unknown until we compress it
    }

    val bytes by lazy {
        BMPWriter.createBMP(readCPUImage())
    }

    override fun readCPUImage(): Image = cpuImage.value
    override fun readGPUImage(): Image = gpuImage()

    override fun readSize(): IntPair {
        val image = cpuImage.value
        return IntPair(image.width, image.height)
    }

    override fun readBytesSync(): ByteArray = bytes
    override fun readTextSync(): String = bytes.decodeToString() // what are you doing? ;)
    override fun inputStreamSync(): InputStream = ByteArrayInputStream(bytes)
}