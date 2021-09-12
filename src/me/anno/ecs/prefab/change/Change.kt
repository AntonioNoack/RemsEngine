package me.anno.ecs.prefab.change

import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.Path.Companion.ROOT_PATH
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager

// todo how do we reference (as variables) to other Entities? probably a path would be correct...
// todo the same for components

abstract class Change(val priority: Int) : Saveable(), Cloneable {

    var path: Path = ROOT_PATH

    abstract fun withPath(path: Path): Change

    fun apply(instance0: PrefabSaveable, chain: MutableSet<FileReference>?) {
        val instance = Hierarchy.getInstanceAt(instance0, path) ?: return
        applyChange(instance, chain)
    }

    abstract fun applyChange(instance: PrefabSaveable, chain: MutableSet<FileReference>?)

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
