package me.anno.utils.structures

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter

/**
 * a simple class to keep track of stack-like-historic states,
 * with the ability to go forward and backward in time; no branching is supported
 *
 * when you want to use this as a saveable object, V must be saveable as well
 * */
class History<V> : Saveable {

    constructor()

    constructor(default: V) {
        add(default)
    }

    val value get() = values[index]

    private var index = -1
    private val values = ArrayList<V>()

    fun add(folder: V) {
        while (values.lastIndex > index) values.removeAt(values.lastIndex)
        values.add(folder)
        index++
    }

    /**
     * @return true on success
     * */
    fun back(ifEmpty: () -> V?): Boolean {
        if (index > 0) {
            index--
        } else {
            val element = ifEmpty() ?: return false
            values.clear()
            values.add(element)
            index = 0
        }
        return true
    }

    /**
     * @return true on success
     * */
    fun forward(): Boolean {
        return if (index + 1 < values.size) {
            index++
            true
        } else false
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("index", index, true)
        writer.writeObjectList(null, "values", values.filterIsInstance<ISaveable>())
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "index" -> index = value
            else -> super.readInt(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "values" -> {
                this.values.clear()
                @Suppress("UNCHECKED_CAST")
                this.values.addAll(values as List<V>)
            }
            else -> super.readObjectArray(name, values)
        }
    }

    override val className: String = "History"

}