package me.anno.utils.structures

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter

/**
 * a simple class to keep track of stack-like-historic states,
 * with the ability to go forward and backward in time; no branching is supported
 *
 * when you want to use this as a saveable object, V must be saveable as well
 * */
class History<V> : Saveable {

    @Suppress("unused")
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
        writer.writeObjectList(null, "values", values.filterIsInstance<Saveable>())
    }

    override fun setProperty(name: String, value: Any?) {
        when(name){
            "index" -> index = value as? Int ?: return
            "values" -> {
                val values = value as? List<*> ?: return
                this.values.clear()
                @Suppress("unchecked_cast")
                this.values.addAll(values.toList() as List<V>)
            }
            else -> super.setProperty(name, value)
        }
    }
}