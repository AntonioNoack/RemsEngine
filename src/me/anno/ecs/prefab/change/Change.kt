package me.anno.ecs.prefab.change

import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import org.apache.logging.log4j.LogManager

// todo how do we reference (as variables) to other Entities? probably a path would be correct...
// todo the same for components

abstract class Change(val priority: Int) : Saveable(), Cloneable {

    var path: Path = ROOT_PATH

    abstract fun withPath(path: Path): Change

    fun apply(instance: PrefabSaveable) {
        apply(instance, pathIndex = 0)
    }

    fun apply(instance: PrefabSaveable, pathIndex: Int) {
        val path = path
        if (pathIndex < path.size) {

            // we can go deeper :)
            val chars = instance.listChildTypes()
            val childName = path.getName(pathIndex)
            val childIndex = path.getIndex(pathIndex)
            val childType = path.getType(pathIndex, chars[0])

            val components = instance.getChildListByType(childType)

            if (components.getOrNull(childIndex)?.name == childName) {
                // bingo, easiest way: name and index are matching
                apply(components[childIndex], pathIndex + 1)
            } else {
                val matchesName = components.firstOrNull { it.name == childName }
                when {
                    matchesName != null -> apply(matchesName, pathIndex + 1)
                    childIndex in components.indices -> apply(components[childIndex], pathIndex + 1)
                    else -> LOGGER.warn(
                        "Missing path $pathIndex in $this, " +
                                "only ${components.size} $childType available ${components.joinToString { "'${it["name"]}'" }}"
                    )
                }
            }

        } else applyChange(instance)

    }

    abstract fun applyChange(instance: PrefabSaveable)

    /**
     * shallow copy
     * */
    public abstract override fun clone(): Change

    override fun save(writer: BaseWriter) {
        super.save(writer)
        val path = path
        if (!path.isEmpty()) {
            writer.writeString("path", path.toString(), true)
        }
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "path" -> path = Path.parse(value ?: "")
            else -> super.readString(name, value)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Change::class)
    }

}
