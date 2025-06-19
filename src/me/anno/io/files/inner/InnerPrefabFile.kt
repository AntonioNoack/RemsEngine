package me.anno.io.files.inner

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.engine.projects.GameEngineProject
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.async.Callback
import java.io.InputStream

open class InnerPrefabFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    var prefab: Prefab
) : InnerFile(absolutePath, relativePath, false, parent), PrefabReadable {

    init {
        prefab.sourceFile = this
    }

    val bytes by lazy {
        GameEngineProject.encoding
            .getForExtension(this)
            .encode(prefab, InvalidRef)
    }

    // it's a prefab, not a zip; never ever
    override fun isSerializedFolder(callback: Callback<Boolean>) = callback.ok(false)
    override fun listChildren(callback: Callback<List<FileReference>>) = callback.ok(emptyList())

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.ok(bytes.inputStream())
    }

    override fun readBytes(callback: Callback<ByteArray>) {
        callback.ok(bytes)
    }

    override fun length(): Long {
        return prefab.adds.size * 64L + prefab.sets.size * 256L // just a wild guess
    }

    override fun readPrefab(): Prefab {
        return prefab
    }
}