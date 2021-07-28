package me.anno.engine.scene

import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Change
import me.anno.ecs.prefab.Path
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

object PrefabHelper {

    fun addE(changes: MutableList<Change>, parentPath: Path, name: String, ref: FileReference = InvalidRef): Path {
        val index = changes.count { it is CAdd && it.clazzName == "Entity" && it.path == parentPath }
        // println("found entity index $index for $name/$ref")
        changes.add(CAdd(parentPath, 'e', "Entity", ref))
        val path = parentPath + (index to 'e')
        changes.add(CSet(path, "name", name))
        return path
    }

    fun addC(changes: MutableList<Change>, parentPath: Path, type: String, name: String? = null): Path {
        // todo need to check className for all components
        val index = changes.count { it is CAdd && it.clazzName != "Entity" && it.path == parentPath }
        // println("found component index $index for $type/$name")
        changes.add(CAdd(parentPath, 'c', type))
        val path = parentPath.add(index, 'c')
        if (name != null) changes.add(CSet(path, "name", name))
        return path
    }

    fun setE(changes: MutableList<Change>, path: Path, name: String, value: Any?) {
        changes.add(CSet(path, name, value))
    }

    fun setC(changes: MutableList<Change>, path: Path, name: String, value: Any?) {
        changes.add(CSet(path, name, value))
    }

}