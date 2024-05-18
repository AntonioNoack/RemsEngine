package me.anno.ecs.prefab.change

import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.utils.types.Strings.shorten2Way
import org.apache.logging.log4j.LogManager

/**
 * a property is set
 * */
class CSet() : Change() {

    constructor(path: Path, name: String, value: Any?) : this() {
        this.path = path
        this.name = name
        this.value = value
        if (name == "parent") throw IllegalStateException("Name cannot be parent, use CAdd for that")
    }

    var name: String = ""
    var value: Any? = null

    /**
     * shallow copy
     * */
    override fun clone(): Change {
        val clone = CSet()
        clone.path = path
        clone.name = name
        clone.value = value
        return clone
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // special handling, to save a little space by omitting "name": and "value":
        if (name.isNotBlank()) {
            val value = value
            if (value is PrefabSaveable) {
                writer.writeObject(null, name, value.prefabPath)
            } else {
                writer.writeSomething(null, name, value, true)
            }
        }
    }

    override fun setProperty(name: String, value: Any?) {
        if (name != "path" || value !is Path) {
            this.name = name
            this.value = value
        } else super.setProperty(name, value)
    }

    override fun applyChange(prefab0: Prefab, instance: PrefabSaveable, depth: Int) {
        applyChange(instance, name, value)
        path = instance.prefabPath
    }

    override val approxSize get() = 10

    override fun toString(): String {
        val str = value.toString().shorten2Way(100)
        return "CSet($path, $name, $str)"
    }

    override fun equals(other: Any?): Boolean {
        return other is CSet &&
                other.name == name &&
                other.path == path &&
                other.value == value
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + path.hashCode()
        // value.hashCode() may be expensive
    }

    companion object {

        private val LOGGER = LogManager.getLogger(CSet::class)

        fun apply(instance0: PrefabSaveable, path: Path, name: String, value: Any?) {
            val instance = Hierarchy.getInstanceAt(instance0, path) ?: return
            applyChange(instance, name, value)
        }

        private fun applyChange(instance: PrefabSaveable, name: String, value0: Any?) {
            var value = value0
            if (value is Path) {
                // it's a prefab saveable; yes, saving paths therefore is no longer supported
                // they just are internal to the change package
                value = Hierarchy.getInstanceAt(instance.root, value)
                LOGGER.debug("Changed path {} to instance {}", value0, value)
            }
            instance.setProperty(name, value)
        }
    }
}
