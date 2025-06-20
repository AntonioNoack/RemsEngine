package me.anno.ecs.prefab.change

import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertEquals

/**
 * denotes a change in a prefab
 * */
abstract class Change : Saveable() {

    var path: Path = ROOT_PATH

    fun apply(prefab0: Prefab, instance0: PrefabSaveable, depth: Int): Exception? {
        assertEquals(ROOT_PATH, instance0.prefabPath, "Root instance must have root path")
        val instance = Hierarchy.getInstanceAt(instance0, path) ?: return null
        assertEquals(path, instance.prefabPath, "Path does not match!")
        return applyChange(prefab0, instance, depth)
    }

    abstract fun applyChange(prefab0: Prefab, instance: PrefabSaveable, depth: Int): Exception?

    /**
     * shallow copy
     * */
    abstract override fun clone(): Change

    override fun save(writer: BaseWriter) {
        super.save(writer)
        val path = path
        writer.writeObject(null, "path", path)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "path" -> when (value) {
                is Path -> path = value
                is String -> path = Path.fromString(value) ?: return
                // else ignored
            }
            else -> super.setProperty(name, value)
        }
    }
}
