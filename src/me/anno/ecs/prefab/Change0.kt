package me.anno.ecs.prefab

import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.utils.LOGGER

class Change0(
    var type: ChangeType0,
    var value: Any?,
    var index: Int // more reliable
) : Saveable(), Iterable<Change0> {

    constructor() : this(ChangeType0.SET_VALUE, null, 0)
    constructor(value: Any?) : this(ChangeType0.SET_VALUE, value, 0)

    var previousChange: Change0? = null
    var nextChange: Change0? = null

    fun removeSelf() {
        previousChange?.nextChange = nextChange
        nextChange?.previousChange = previousChange
    }

    fun append(change: Change0) {
        if (nextChange != null) {
            nextChange!!.append(change)
        } else {
            nextChange = change
            change.previousChange = change
        }
    }

    override fun iterator(): Iterator<Change0> {
        return object : Iterator<Change0> {
            var element: Change0? = this@Change0
            override fun hasNext(): Boolean = element != null
            override fun next(): Change0 {
                val result = element!!
                element = result.nextChange
                return result
            }
        }
    }

    fun <V : NamedSaveable> addElement(list: MutableList<V>) {
        list.add(index, value as V)
    }

    fun applyChange(instance: ISaveable, name: String) {
        val reflections = instance.getReflections()
        val property = reflections[name]
        if (property == null) {
            // todo only warn once
            LOGGER.warn("Missing property $name")
            return
        }
        when (type) {
            ChangeType0.SET_VALUE -> {
                property[instance] = value
            }
            ChangeType0.ADD_ELEMENT -> {
                // creating a new list for many elements is inefficient... we use an arraylist, if available
                var oldList = property[instance] as? List<Any?>
                if (index < 0 || oldList == null) {
                    if (oldList is MutableList) {
                        // muss ganz am Anfang sein,
                        // da wir sonst nichts zu Index 0 bewegen könnten
                        oldList.add(0, value)
                    } else {
                        property[instance] = if (oldList == null) arrayListOf(value)
                        else oldList + value
                    }
                } else {
                    if (oldList is MutableList) {
                        oldList.add(index + 1, value)
                    } else {
                        oldList = ArrayList(oldList)
                        oldList.add(index + 1, value)
                        property[instance] = oldList
                    }
                }
            }
            ChangeType0.REMOVE_ELEMENT -> {
                var oldList = property[instance] as? List<Any?> ?: return
                if (index < 0 || index >= oldList.size) return
                if (oldList is MutableList) {
                    oldList.removeAt(index)
                } else {
                    oldList = ArrayList(oldList)
                    oldList.removeAt(index)
                    property[instance] = oldList
                }
            }
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeInt("type", type.id)
        // the value must be written here to keep the order -> self = null
        writer.writeSomething(null, "value", value, true)
        writer.writeInt("index", index)
        writer.writeObject(null, "nextChange", nextChange)
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "type" -> type = ChangeType0.values.firstOrNull { it.id == value } ?: return
            "index" -> index = value
            else -> super.readInt(name, value)
        }
    }

    override fun readSomething(name: String, value: Any?) {
        when (name) {
            "value" -> this.value = value
            else -> super.readSomething(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "nextChange" -> {
                if (value !is Change0) return
                append(value)
            }
            else -> super.readObject(name, value)
        }
    }

    override val className: String = "Change"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}