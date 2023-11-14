package me.anno.ecs.prefab.change

import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import java.text.ParseException

abstract class Change : Saveable() {

    var path: Path = ROOT_PATH

    fun apply(prefab0: Prefab, instance0: PrefabSaveable, depth: Int) {
        if (instance0.prefabPath != ROOT_PATH) throw RuntimeException("Root instance must have root path, got ${instance0.prefabPath}")
        val instance = Hierarchy.getInstanceAt(instance0, path) ?: return
        if (instance.prefabPath != path) throw RuntimeException("Path does not match! ${instance.prefabPath} != $path")
        applyChange(prefab0, instance, depth)
    }

    abstract fun applyChange(prefab0: Prefab, instance: PrefabSaveable, depth: Int)

    /**
     * shallow copy
     * */
    abstract fun clone(): Change

    override fun save(writer: BaseWriter) {
        super.save(writer)
        val path = path
        writer.writeObject(null, "path", path)
    }

    override fun readObject(name: String, value: ISaveable?) {
        if (name == "path" && value is Path) {
            path = value
        } else super.readObject(name, value)
    }

    override fun readString(name: String, value: String?) {
        if (name == "path") {
            try {
                path = Path.parse(value)
            } catch (e: ParseException) {
                super.readString(name, value)
            }
        } else super.readString(name, value)
    }

}
