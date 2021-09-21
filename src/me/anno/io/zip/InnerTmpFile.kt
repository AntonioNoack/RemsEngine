package me.anno.io.zip

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextWriter
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

abstract class InnerTmpFile(id1: Int = id.incrementAndGet()) :
    InnerFile("fake/$id1", "fake/$id1", false, InvalidRef) {

    class InnerTmpByteFile(bytes: ByteArray) : InnerTmpFile() {

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

    class InnerTmpTextFile(text: String) : InnerTmpFile() {

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

    class InnerTmpPrefabFile(val prefab: Prefab) : InnerTmpFile(), PrefabReadable {

        init {
            val size = Int.MAX_VALUE.toLong()
            this.size = size
            this.compressedSize = size
        }

        val text = lazy { TextWriter.toText(prefab) }
        val bytes = lazy { text.value.toByteArray() }

        override fun isSerializedFolder(): Boolean = false
        override fun listChildren(): List<FileReference>? = null

        override fun readText() = text.value
        override fun readBytes() = bytes.value

        override fun getInputStream(): InputStream {
            return text.value.byteInputStream()
        }

        override fun readPrefab(): Prefab {
            return prefab
        }

    }

    companion object {
        var id = AtomicInteger()
    }

}