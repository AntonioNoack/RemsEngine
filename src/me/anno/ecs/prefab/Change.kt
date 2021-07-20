package me.anno.ecs.prefab

import me.anno.ecs.Entity
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import java.lang.NullPointerException

// todo how do we reference (as variables) to other Entities? probably a path would be correct...
// todo the same for components

abstract class Change(val priority: Int) : Saveable() {

    var path: Path? = null

    fun apply(entity: Entity) {
        apply(entity, pathIndex = 0)
    }

    fun apply(entity: Entity, pathIndex: Int) {
        val path = path ?: throw NullPointerException("Path is null inside $this")
        val delta = pathIndex - path.hierarchy.size
        if (delta < 0) {
            val childIndex = path.hierarchy[pathIndex]
            // we can go deeper :)
            if (delta == -1 && this is ChangeSetComponentAttribute) {
                // decide based on type
                applyChange(entity.components[childIndex], path.name)
            } else {
                // just go deeper
                apply(entity.children[childIndex], pathIndex + 1)
            }
        } else {
            applyChange(entity, path.name)
        }
    }

    abstract fun applyChange(element: Any?, name: String?)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        val path = path
        if (path != null) {
            writer.writeString("path", path.toString(), true)
        }
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "path" -> path = Path.parse(value)
            else -> super.readString(name, value)
        }
    }

}
