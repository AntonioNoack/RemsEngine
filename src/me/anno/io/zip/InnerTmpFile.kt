package me.anno.io.zip

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.image.Image
import me.anno.image.ImageReadable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextWriter
import me.anno.utils.types.Strings.isBlank2
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

abstract class InnerTmpFile private constructor(
    name: String,
    @Suppress("unused_parameter") unused: Int // just for convenience
) : InnerFile(name, name, false, InvalidRef) {

    constructor(ext: String) : this("", ext)
    constructor(prefix: String, ext: String) : this(
        if (prefix.isBlank2()) "tmp://${id.incrementAndGet()}.$ext"
        else "tmp://$prefix.${id.incrementAndGet()}.$ext", 0
    )

    @Suppress("unused")
    class InnerTmpByteFile(bytes: ByteArray, ext: String = "bin") : InnerTmpFile(ext) {

        var bytes: ByteArray = bytes
            set(value) {
                field = value
                val size = value.size.toLong()
                this.size = size
                this.compressedSize = size
            }

        override fun getInputStream(): InputStream {
            return bytes.inputStream()
        }

    }

    @Suppress("unused")
    class InnerTmpTextFile(text: String, ext: String = "txt") : InnerTmpFile(ext) {

        var text: String = text
            set(value) {
                field = value
                val size = value.length.toLong()
                this.size = size
                this.compressedSize = size
            }

        override fun readText(): String = text
        override fun readBytes(): ByteArray = text.toByteArray()
        override fun getInputStream(): InputStream {
            return text.byteInputStream()
        }

    }

    class InnerTmpPrefabFile(val prefab: Prefab, name: String, ext: String = "json") : InnerTmpFile(name, ext),
        PrefabReadable {

        constructor(prefab: Prefab) : this(prefab, prefab.getProperty("name") as? String ?: "")

        init {
            val size = Int.MAX_VALUE.toLong()
            this.size = size
            this.compressedSize = size
            prefab.source = this
        }

        val text by lazy { TextWriter.toText(prefab, InvalidRef) }
        val bytes by lazy { text.toByteArray() }

        override fun isSerializedFolder(): Boolean = false
        override fun listChildren(): List<FileReference>? = null

        override fun readText() = text
        override fun readBytes() = bytes

        override fun getInputStream(): InputStream {
            return text.byteInputStream()
        }

        override fun readPrefab(): Prefab {
            return prefab
        }

    }

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
        override fun listChildren(): List<FileReference>? = null

        override fun readText() = text.value
        override fun readBytes(): ByteArray = bytes.value

        override fun getInputStream(): InputStream {
            return text.value.byteInputStream()
        }

        override fun readImage(): Image = image

    }

    companion object {
        var id = AtomicInteger()
    }

}