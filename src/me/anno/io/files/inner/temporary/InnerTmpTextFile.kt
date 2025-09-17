package me.anno.io.files.inner.temporary

import me.anno.utils.async.Callback
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

@Suppress("unused")
class InnerTmpTextFile(text: String, ext: String = "txt") : InnerTmpFile(ext) {

    var text: String = text
        private set

    init {
        writeText(text)
    }

    override fun writeText(text: String, offset: Int, length: Int) {
        this.text = text.substring(offset, length)
        markAsModified()
    }

    override fun length(): Long = text.length.toLong() // a guess

    override fun readText(callback: Callback<String>) {
        callback.ok(text)
    }

    override fun readBytes(callback: Callback<ByteArray>) {
        callback.ok(text.encodeToByteArray())
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.ok(text.byteInputStream())
    }

    override fun outputStream(append: Boolean): OutputStream {
        return object : ByteArrayOutputStream() {
            override fun close() {
                super.close()

                val writtenText = toByteArray().decodeToString()
                if (append) text += writtenText
                else text = writtenText
                markAsModified()
            }
        }
    }
}