package me.anno.io.files.inner.temporary

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import java.io.ByteArrayInputStream
import java.lang.ref.WeakReference

class InnerTmpPrefabFile(val prefab: Prefab, name: String, ext: String = "json") :
    InnerTmpFile(name, ext), PrefabReadable {

    constructor(prefab: Prefab) : this(prefab, prefab["name"] as? String ?: "")

    init {
        val size = Int.MAX_VALUE.toLong()
        this.size = size
        this.compressedSize = size
        prefab.source = this
        synchronized(prefabFiles) {
            prefabFiles
                .getOrPut(prefab.clazzName) { ArrayList() }
                .add(WeakReference(this))
        }
    }

    val text by lazy { JsonStringWriter.toText(prefab, InvalidRef) }
    val bytes by lazy { text.encodeToByteArray() }

    override fun isSerializedFolder(): Boolean = false
    override fun listChildren(): List<FileReference> = emptyList()

    override fun readTextSync() = text
    override fun readBytesSync() = bytes
    override fun inputStreamSync() = ByteArrayInputStream(bytes)

    override fun readPrefab(): Prefab {
        return prefab
    }
}