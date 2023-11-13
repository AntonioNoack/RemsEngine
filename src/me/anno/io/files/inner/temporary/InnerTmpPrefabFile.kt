package me.anno.io.files.inner.temporary

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import java.io.InputStream
import java.lang.ref.WeakReference
import java.nio.charset.Charset

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
    val bytes by lazy { text.toByteArray() }

    override fun isSerializedFolder(): Boolean = false
    override fun listChildren(): List<FileReference>? = null

    override fun readText(charset: Charset, callback: (String?, Exception?) -> Unit) {
        callback(text, null)
    }

    override fun readBytes(callback: (it: ByteArray?, exc: Exception?) -> Unit) {
        callback(bytes, null)
    }

    override fun readTextSync() = text
    override fun readBytesSync() = bytes

    override fun getInputStream(callback: (InputStream?, Exception?) -> Unit) {
        callback(text.byteInputStream(), null)
    }

    override fun readPrefab(): Prefab {
        return prefab
    }
}