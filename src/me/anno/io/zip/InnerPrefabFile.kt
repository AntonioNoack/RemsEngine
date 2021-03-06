package me.anno.io.zip

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextWriter
import java.io.InputStream

open class InnerPrefabFile(
    absolutePath: String, relativePath: String, _parent: FileReference,
    var prefab: Prefab
) : InnerFile(absolutePath, relativePath, false, _parent), PrefabReadable {

    init {
        val size = Int.MAX_VALUE.toLong()
        this.size = size
        this.compressedSize = size
        prefab.source = this
    }

    val text by lazy { TextWriter.toText(prefab, InvalidRef) }
    val bytes by lazy { text.toByteArray() }

    // it's a prefab, not a zip; never ever
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