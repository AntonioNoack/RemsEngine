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
        val name = name
        val value = value
        if (name == "path") {
            writer.writeString("name", name)
            writer.writeSomething(null, "value", value, true)
        } else {
            // special handling, to save a little space by omitting "name": and "value":
            writer.writeSomething(null, name!!, value, true)
        }
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "name" -> this.name = value
            else -> super.readString(name, value)
        }
    }

    override fun readSomething(name: String, value: Any?) {
        // special handling, to save a little space by omitting "name": and "value":
        if (this.name == null) this.name = name
        this.value = value
    }

    override fun applyChange(instance: PrefabSaveable, chain: MutableSet<FileReference>?) {
        val name = name ?: return
        if (!instance.set(name, value)) {
            LOGGER.warn("${instance::class.simpleName}.$name is unknown")
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
