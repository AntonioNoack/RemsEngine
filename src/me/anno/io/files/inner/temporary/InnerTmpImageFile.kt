package me.anno.io.files.inner.temporary

import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import me.anno.utils.structures.tuples.IntPair
import java.io.ByteArrayOutputStream
import java.io.InputStream

class InnerTmpImageFile(val image: Image, ext: String = "png") : InnerTmpFile(ext), ImageReadable {

    val bytes = lazy {
        val bos = ByteArrayOutputStream(1024)
        image.write(bos, "png")
        bos.toByteArray()
    }

    override fun length(): Long {
        return image.sizeGuess()
    }

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
    override fun readSize(): IntPair = IntPair(image.width, image.height)
}