package me.anno.ecs.prefab.change

import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.utils.strings.StringHelper.shorten2Way
import org.apache.logging.log4j.LogManager

class CSet() : Change() {

    constructor(path: Path, name: String?, value: Any?) : this() {
        this.path = path
        this.name = name
        this.value = value
        if (name == "parent") throw IllegalStateException("Name cannot be parent, use CAdd for that")
    }

    var name: String? = null
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
        val value = value
        if (value is PrefabSaveable) {
            writer.writeObject(null, name, value.prefabPath!!)
        } else {
            writer.writeSomething(null, name!!, value, true)
        }
    }

    override fun readSomething(name: String, value: Any?) {
        this.name = name
        this.value = value
    }

    override fun applyChange(instance: PrefabSaveable, depth: Int) {
        applyChange(instance, path, name!!, value)
        path = instance.prefabPath ?: path // remove a few superfluous instances
    }

    override val approxSize get() = 10
    override fun isDefaultValue(): Boolean = false

    override val className: String get() = "CSet"

    override fun toString(): String {
        val str = value.toString().shorten2Way(100)
        return "CSet($path, $name, $str)"
    }

    companion object {

        private val LOGGER = LogManager.getLogger(CSet::class)

        fun apply(instance0: PrefabSaveable, path: Path, name: String, value: Any?) {
            val instance = Hierarchy.getInstanceAt(instance0, path) ?: return
            applyChange(instance, path, name, value)
        }

        fun applyChange(instance: PrefabSaveable, path: Path, name: String, value0: Any?) {
            var value = value0
            if (value is Path) {
                // it's a prefab saveable; yes, saving paths therefore is no longer supported
                // they just are internal to the change package
                value = Hierarchy.getInstanceAt(instance.root, value)
                LOGGER.debug("Changed path $value0 to instance $value")
            }
            if (!instance.set(name, value)) {
                LOGGER.warn("Property ${instance::class.simpleName}.$name is unknown/faulty, path: $path, prefab: ${instance.root.prefab?.source}")
            }
        }

    }

}
