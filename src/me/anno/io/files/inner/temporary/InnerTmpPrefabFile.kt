package me.anno.io.files.inner.temporary

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.files.Signature.Companion.json
import me.anno.io.files.inner.SignatureFile
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.utils.async.Callback
import java.io.InputStream
import java.lang.ref.WeakReference

class InnerTmpPrefabFile(val prefab: Prefab, name: String, ext: String = "json") :
    InnerTmpFile(name, ext), PrefabReadable, SignatureFile {

    constructor(prefab: Prefab) : this(prefab, prefab["name"] as? String ?: "")

    init {
        prefab.sourceFile = this
        synchronized(prefabFiles) {
            prefabFiles
                .getOrPut(prefab.clazzName, ::ArrayList)
                .add(WeakReference(this))
        }
    }

    override var signature: Signature? = json
    val text by lazy { JsonStringWriter.toText(prefab, InvalidRef) }
    val bytes by lazy { text.encodeToByteArray() }

    override fun length(): Long {
        return 100_000L // just a guess
    }

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
        return prefab
    }
}