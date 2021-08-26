package me.anno.io.zip

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.io.files.FileReference
import me.anno.io.text.TextWriter
import java.io.InputStream

class InnerPrefabFile(
    absolutePath: String, relativePath: String, _parent: FileReference,
    val prefab: Prefab
) : InnerFile(absolutePath, relativePath, false, _parent), PrefabReadable {

    init {
        val size = Int.MAX_VALUE.toLong()
        this.size = size
        this.compressedSize = size
    }

    val text = lazy { TextWriter.toText(prefab) }
    val bytes = lazy { text.value.toByteArray() }

    override fun readText() = text.value
    override fun readBytes() = bytes.value

    override fun getInputStream(): InputStream {
        return text.value.byteInputStream()
    }

    override fun readPrefab(): Prefab {
        return prefab
    }

}