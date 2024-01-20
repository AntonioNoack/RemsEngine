package me.anno.io.files.inner.temporary

import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.io.files.FileReference
import me.anno.utils.structures.tuples.IntPair
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

class InnerTmpImageFile(val image: Image, ext: String = "png") : InnerTmpFile(ext), ImageReadable {

    init {
        val size = Int.MAX_VALUE.toLong()
        this.size = size
        this.compressedSize = size
    }

    val text = lazy { "" } // we could write a text based image here
    val bytes = lazy {
        val bos = ByteArrayOutputStream(1024)
        image.write(bos, "png")
        bos.toByteArray()
    }

    override fun isSerializedFolder(): Boolean = false
    override fun listChildren(): List<FileReference> = emptyList()

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        callback(inputStreamSync(), null)
    }

    override fun inputStreamSync() = ByteArrayInputStream(readBytesSync())
    override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
        callback(bytes.value, null)
    }

    override fun readBytesSync(): ByteArray = bytes.value

    override fun readCPUImage(): Image = image
    override fun readGPUImage(): Image = image
    override fun readSize(): IntPair = IntPair(image.width, image.height)
}