package me.anno.engine.scene

import me.anno.ecs.prefab.*
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

object PrefabHelper {

    fun addE(changes: MutableList<Change>, parentPath: IntArray, name: String, ref: FileReference = InvalidRef):
            IntArray {
        val index = changes.count { it is ChangeAddEntity && it.path!!.hierarchy.contentEquals(parentPath) }
        changes.add(ChangeAddEntity(parentPath, ref))
        val path = parentPath + index
        changes.add(ChangeSetEntityAttribute(path, "name", name))
        return path
    }

    fun addC(changes: MutableList<Change>, parentPath: IntArray, type: String, name: String? = null): IntArray {
        val index = changes.count { it is ChangeAddComponent && it.path!!.hierarchy.contentEquals(parentPath) }
        changes.add(ChangeAddComponent(parentPath, type))
        val path = parentPath + index
        if (name != null) {
            changes.add(ChangeSetComponentAttribute(path, "name", name))
        }
        return path
    }

    fun setE(changes: MutableList<Change>, path: IntArray, name: String, value: Any?) {
        changes.add(ChangeSetEntityAttribute(path, name, value))
    }

    fun setC(changes: MutableList<Change>, path: IntArray, name: String, value: Any?) {
        changes.add(ChangeSetComponentAttribute(path, name, value))
    }

}