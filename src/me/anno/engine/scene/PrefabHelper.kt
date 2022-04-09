package me.anno.engine.scene

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

object PrefabHelper {

    fun addE(prefab: Prefab, parentPath: Path, name: String, ref: FileReference = InvalidRef): Path {
        return add(prefab, parentPath, 'e', "Entity", name, ref)
    }

    fun addC(prefab: Prefab, parentPath: Path, type: String, name: String = type): Path {
        return add(prefab, parentPath, 'c', type, name, InvalidRef)
    }

    fun add(
        prefab: Prefab, parentPath: Path,
        typeChar: Char, type: String, name: String, ref: FileReference
    ): Path {
        return prefab.add(parentPath, typeChar, type, name, ref)
    }

    fun setX(prefab: Prefab, path: Path, name: String, value: Any?) {
        prefab[path, name] = value
    }

}