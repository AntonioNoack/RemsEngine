package me.anno.io.files.inner.lazy

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.utils.structures.Callback
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFile
import me.anno.io.json.saveable.JsonStringWriter
import java.io.ByteArrayInputStream
import java.io.InputStream

open class InnerLazyPrefabFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    val prefab: Lazy<Prefab>
) : InnerFile(absolutePath, relativePath, false, parent), PrefabReadable {

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
    val bytes by lazy { text.encodeToByteArray() }

    // it's a prefab, not a zip; never ever
    override fun isSerializedFolder(): Boolean = false
    override fun listChildren(): List<FileReference> = emptyList()

    override fun readTextSync() = text
    override fun readBytesSync() = bytes

    override fun readText(callback: Callback<String>) {
        callback.ok(text)
    }

    override fun readBytes(callback: Callback<ByteArray>) {
        callback.ok(bytes)
    }

    override fun inputStreamSync(): InputStream {
        return ByteArrayInputStream(bytes)
    }

    override fun readPrefab(): Prefab {
        return prefab2.value
    }
}