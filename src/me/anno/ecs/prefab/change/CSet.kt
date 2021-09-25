package me.anno.ecs.prefab.change

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager

class CSet() : Change(5) {

    constructor(path: Path, name: String?, value: Any?) : this() {
        this.path = path
        this.name = name
        this.value = value
        if (name == "parent") throw IllegalStateException("Name cannot be parent, use CAdd for that")
    }

    override fun withPath(path: Path): Change {
        return CSet(path, name, value)
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
        if (value is PrefabSaveable) {
            TODO("write path for value")
        }
        writer.writeSomething(null, name!!, value, true)
    }

    override fun readSomething(name: String, value: Any?) {
        if (name == "path" && value is Path) {
            path = value
        } else {
            this.name = name
            this.value = value
        }
    }

    override fun applyChange(instance: PrefabSaveable, chain: MutableSet<FileReference>?) {
        val name = name ?: return
        if (!instance.set(name, value)) {
            LOGGER.warn("Property ${instance::class.simpleName}.$name is unknown, path: $path")
        }
    }

    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

    override val className: String = "CSet"

    override fun toString(): String {
        var str = value.toString()
        val maxLength = 100
        if (str.length > maxLength) {
            str = str.substring(0, maxLength * 7 / 10 - 3) +
                    "..." +
                    str.substring(str.length - maxLength * 3 / 10)
        }
        return "CSet($path, $name, $str)"
    }

    companion object {
        private val LOGGER = LogManager.getLogger(CSet::class)
    }

}
