package me.anno.io.files.inner.lazy

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFile
import me.anno.io.json.saveable.JsonStringWriter
import java.io.InputStream
import java.nio.charset.Charset

open class InnerLazyPrefabFile(
    absolutePath: String, relativePath: String, _parent: FileReference,
    val prefab: Lazy<Prefab>
) : InnerFile(absolutePath, relativePath, false, _parent), PrefabReadable {

    val prefab2 = lazy {
        val v = prefab.value
        v.source = this
        v
    }

    init {
        val size = Int.MAX_VALUE.toLong()
        this.size = size
        this.compressedSize = size
    }

    val text by lazy { JsonStringWriter.toText(prefab2.value, InvalidRef) }
    val bytes by lazy { text.toByteArray() }

    // it's a prefab, not a zip; never ever
    override fun isSerializedFolder(): Boolean = false
    override fun listChildren(): List<FileReference>? = null

    override fun readTextSync() = text
    override fun readBytesSync() = bytes

    override fun readText(charset: Charset, callback: (String?, Exception?) -> Unit) {
        callback(text, null)
    }

    override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
        callback(bytes, null)
    }

    override fun inputStreamSync(): InputStream {
        return text.byteInputStream()
    }

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        callback(inputStreamSync(), null)
    }

    override fun readPrefab(): Prefab {
        return prefab2.value
    }
}