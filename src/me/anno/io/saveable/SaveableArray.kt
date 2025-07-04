package me.anno.io.saveable

import me.anno.io.base.BaseWriter
import me.anno.io.json.saveable.JsonStringReader

class SaveableArray() : Saveable(), MutableList<Saveable> {

    val values = ArrayList<Saveable>()

    constructor(children: Collection<Saveable>) : this() {
        values.addAll(children)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(null, "values", values, false)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "values" -> {
                val values = value as? List<*> ?: return
                this.values.clear()
                this.values.addAll(values.filterIsInstance<Saveable>())
            }
            else -> super.setProperty(name, value)
        }
    }

    override val size = values.size

    override fun get(index: Int) = values[index]
    override fun set(index: Int, element: Saveable) = values.set(index, element)

    override fun indexOf(element: Saveable) = values.indexOf(element)
    override fun lastIndexOf(element: Saveable) = values.lastIndexOf(element)

    override fun contains(element: Saveable) = values.contains(element)
    override fun containsAll(elements: Collection<Saveable>) = values.containsAll(elements)

    override fun iterator(): MutableIterator<Saveable> = values.iterator()

    override fun add(element: Saveable) = values.add(element)
    override fun add(index: Int, element: Saveable) = values.add(index, element)
    override fun addAll(index: Int, elements: Collection<Saveable>) = values.addAll(index, elements)
    override fun addAll(elements: Collection<Saveable>) = values.addAll(elements)

    override fun remove(element: Saveable) = values.remove(element)
    override fun removeAll(elements: Collection<Saveable>) = values.removeAll(elements.toSet())
    override fun removeAt(index: Int) = values.removeAt(index)

    override fun retainAll(elements: Collection<Saveable>) = values.retainAll(elements.toSet())

    override fun clear() = values.clear()
    override fun isEmpty() = values.isEmpty()

    override fun listIterator() = values.listIterator()
    override fun listIterator(index: Int) = values.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int) = values.subList(fromIndex, toIndex)

}