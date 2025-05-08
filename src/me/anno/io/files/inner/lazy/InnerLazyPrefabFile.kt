package me.anno.io.files.inner.lazy

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFile
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.utils.async.Callback
import java.io.InputStream

open class InnerLazyPrefabFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    val prefab: Lazy<Prefab>
) : InnerFile(absolutePath, relativePath, false, parent), PrefabReadable {

    val prefab2 = lazy {
        val v = prefab.value
        v.sourceFile = this
        v
    }

    override fun length(): Long {
        return Int.MAX_VALUE.toLong()
    }

    val text by lazy { JsonStringWriter.toText(prefab2.value, InvalidRef) }
    val bytes by lazy { text.encodeToByteArray() }

    // it's a prefab, not a zip; never ever
    override fun isSerializedFolder(): Boolean = false
    override fun listChildren(callback: Callback<List<FileReference>>) = callback.ok(emptyList())

    override fun readText(callback: Callback<String>) {
        callback.ok(text)
    }

    override fun readBytes(callback: Callback<ByteArray>) {
        callback.ok(bytes)
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.ok(bytes.inputStream())
    }

    override fun readPrefab(): Prefab {
        return prefab2.value
    }
}