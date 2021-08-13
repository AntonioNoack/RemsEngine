package me.anno.engine.scene

import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Change
import me.anno.ecs.prefab.Path
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

object PrefabHelper {

    fun addE(changes: MutableList<Change>, parentPath: Path, name: String, ref: FileReference = InvalidRef): Path {
        return add(changes, parentPath, 'e', "Entity", name, ref)
    }

    fun addC(changes: MutableList<Change>, parentPath: Path, type: String, name: String = type): Path {
        return add(changes, parentPath, 'c', type, name, InvalidRef)
    }

    fun add(
        changes: MutableList<Change>, parentPath: Path,
        typeChar: Char, type: String, name: String, ref: FileReference
    ): Path {
        val index = changes.count { it is CAdd && it.type == typeChar && it.path == parentPath }
        changes.add(CAdd(parentPath, typeChar, type, name, ref))
        val path = parentPath.added(name, index, typeChar)
        if (type != name) changes.add(CSet(path, "name", name))
        return path
    }

    fun setE(changes: MutableList<Change>, path: Path, name: String, value: Any?) {
        set(changes, path, name, value)
    }

    fun setC(changes: MutableList<Change>, path: Path, name: String, value: Any?) {
        set(changes, path, name, value)
    }

    fun set(changes: MutableList<Change>, path: Path, name: String, value: Any?) {
        changes.add(CSet(path, name, value))
    }

}