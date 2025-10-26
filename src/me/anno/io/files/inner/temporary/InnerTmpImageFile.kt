package me.anno.io.files.inner.temporary

import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import org.joml.Vector2i
import java.io.ByteArrayOutputStream
import java.io.InputStream

class InnerTmpImageFile(val image: Image, ext: String = "png") : InnerTmpFile(ext), ImageReadable {

    val bytes = lazy {
        val bos = ByteArrayOutputStream(1024)
        image.write(bos, "png")
        bos.toByteArray()
    }

    override fun length(): Long = image.sizeGuess()

    override fun isSerializedFolder(callback: Callback<Boolean>) = callback.ok(false)
    override fun listChildren(callback: Callback<List<FileReference>>) = callback.ok(emptyList())

    override fun readBytes(callback: Callback<ByteArray>) {
        callback.ok(bytes.value)
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.ok(bytes.value.inputStream())
    }

    override fun readText(callback: Callback<String>) {
        callback.ok(bytes.value.decodeToString())
    }

    override fun readCPUImage(): Image = image
    override fun readGPUImage(): Image = image
    override fun readSize(): Vector2i = Vector2i(image.width, image.height)
}