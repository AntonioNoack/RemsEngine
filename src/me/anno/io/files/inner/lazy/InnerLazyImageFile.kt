package me.anno.io.files.inner.lazy

import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.SignatureFile
import me.anno.utils.async.Callback
import org.joml.Vector2i
import java.io.ByteArrayOutputStream
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

    val bytes: ByteArray by lazy {
        val bos = ByteArrayOutputStream()
        readCPUImage().write(bos, lcExtension)
        bos.toByteArray()
    }

    override fun readCPUImage(): Image = cpuImage.value
    override fun readGPUImage(): Image = gpuImage()

    override fun readSize(): Vector2i {
        val image = cpuImage.value
        return Vector2i(image.width, image.height)
    }

    override fun readBytes(callback: Callback<ByteArray>) {
        callback.ok(bytes)
    }

    override fun readText(callback: Callback<String>) {
        callback.ok(bytes.decodeToString()) // what are you doing? ;)
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.ok(bytes.inputStream())
    }

    override fun length(): Long {
        return if (cpuImage.isInitialized()) cpuImage.value.sizeGuess() else 100_000L // just a guess
    }
}