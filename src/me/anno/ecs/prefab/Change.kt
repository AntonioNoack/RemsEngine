package me.anno.ecs.prefab

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter

// todo how do we reference (as variables) to other Entities? probably a path would be correct...
// todo the same for components

abstract class Change(val priority: Int) : Saveable() {

    var path: Path? = null

    fun apply(instance: PrefabSaveable) {
        apply(instance, pathIndex = 0)
    }

    fun apply(instance: PrefabSaveable, pathIndex: Int) {
        val path = path ?: throw NullPointerException("Path is null inside $this")
        if (pathIndex < path.indices.size) {

            // we can go deeper :)
            val chars = instance.listChildTypes()
            val childIndex = path.getIndex(pathIndex)
            val childType = path.getType(pathIndex, chars[0])

            val components = instance.getChildListByType(childType)
            if (childIndex !in components.indices)
                throw IndexOutOfBoundsException("Missing path $pathIndex in $this, only ${components.size} $childType available ${components.joinToString { "'${it["name"]}'" }}")

            apply(components[childIndex], pathIndex + 1)

        } else applyChange(instance)

    }

    abstract fun applyChange(instance: PrefabSaveable)

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
