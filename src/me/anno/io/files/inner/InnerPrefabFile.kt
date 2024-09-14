package me.anno.io.files.inner

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.projects.GameEngineProject
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import java.io.InputStream

open class InnerPrefabFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    var prefab: Prefab
) : InnerFile(absolutePath, relativePath, false, parent), PrefabReadable {

    init {
        val size = Int.MAX_VALUE.toLong()
        this.size = size
        this.compressedSize = size
        prefab.source = this
    }

    val bytes by lazy {
        GameEngineProject.encoding
            .getForExtension(this)
            .encode(prefab, InvalidRef)
    }

    // it's a prefab, not a zip; never ever
    override fun isSerializedFolder(): Boolean = false
    override fun listChildren(): List<FileReference> = emptyList()

    override fun readBytesSync() = bytes
    override fun inputStreamSync(): InputStream = bytes.inputStream()

    override fun readPrefab(): Prefab {
        return prefab
    }
}