package me.anno.io.files.inner.lazy

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.files.Signature.Companion.json
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.SignatureFile
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.utils.async.Callback
import java.io.InputStream

open class InnerLazyPrefabFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    prefab: Lazy<Prefab>
) : InnerFile(absolutePath, relativePath, false, parent),
    PrefabReadable, SignatureFile {

    val prefab = lazy {
        val value = prefab.value
        value.sourceFile = this
        value
    }

    override var signature: Signature? = json

    override fun length(): Long {
        return Int.MAX_VALUE.toLong()
    }

    val text by lazy { JsonStringWriter.toText(prefab.value, InvalidRef) }
    val bytes by lazy { text.encodeToByteArray() }

    // it's a prefab, not a zip; never ever
    override fun isSerializedFolder(callback: Callback<Boolean>) = callback.ok(false)
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
        return prefab.value
    }
}