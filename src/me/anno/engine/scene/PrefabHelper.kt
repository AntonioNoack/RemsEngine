package me.anno.engine.scene

import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.prefab.Prefab
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
        val index = prefab.adds.count { it.type == typeChar && it.path == parentPath }
        prefab.add(CAdd(parentPath, typeChar, type, name, ref))
        return parentPath.added(name, index, typeChar)
    }

    fun setX(prefab: Prefab, path: Path, name: String, value: Any?) {
        val changes = prefab.sets as MutableList<CSet>
        changes.add(CSet(path, name, value))
    }

}