package me.anno.io

import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter

class SaveableArray() : Saveable(), MutableList<ISaveable> {

    val values = ArrayList<ISaveable>()

    constructor(children: Collection<ISaveable>) : this() {
        values.addAll(children)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        if (name == "values") {
            this.values.clear()
            this.values.addAll(values.filterNotNull())
        } else super.readObjectArray(name, values)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(null, "values", values, false)
    }

    override val className: String get() = "SaveableArray"

    override val size = values.size

    override fun contains(element: ISaveable) = values.contains(element)

    override fun containsAll(elements: Collection<ISaveable>) = values.containsAll(elements)

    override fun get(index: Int) = values[index]

    override fun indexOf(element: ISaveable) = values.indexOf(element)

    override fun isEmpty() = values.isEmpty()

    override fun iterator(): MutableIterator<ISaveable> = values.iterator()

    override fun lastIndexOf(element: ISaveable) = values.lastIndexOf(element)

    override fun add(element: ISaveable) = values.add(element)

    override fun add(index: Int, element: ISaveable) = values.add(index, element)

    override fun addAll(index: Int, elements: Collection<ISaveable>) = values.addAll(index, elements)

    override fun addAll(elements: Collection<ISaveable>) = values.addAll(elements)

    override fun clear() = values.clear()

    override fun listIterator() = values.listIterator()

    override fun listIterator(index: Int) = values.listIterator(index)

    override fun remove(element: ISaveable) = values.remove(element)

    override fun removeAll(elements: Collection<ISaveable>) = values.removeAll(elements.toSet())

    override fun removeAt(index: Int) = values.removeAt(index)

    override fun retainAll(elements: Collection<ISaveable>) = values.retainAll(elements.toSet())

    override fun set(index: Int, element: ISaveable) = values.set(index, element)

    override fun subList(fromIndex: Int, toIndex: Int) = values.subList(fromIndex, toIndex)

    fun clone(): SaveableArray {
        return JsonStringReader.readFirst(JsonStringWriter.toText(this as ISaveable, InvalidRef), InvalidRef, false)
    }

}