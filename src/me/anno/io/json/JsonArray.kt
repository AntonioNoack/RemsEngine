package me.anno.io.json

import java.lang.RuntimeException

class JsonArray: JsonNode(), List<Any> {

    val content = ArrayList<Any>()
    fun add(any: Any) = content.add(any)
    override fun get(key: String): JsonNode? {
        throw RuntimeException("Not supported, this is an array node!")
    }
    override fun toString() = content.toString()

    override fun get(index: Int): Any = content[index]
    override val size: Int
        get() = content.size

    override fun contains(element: Any): Boolean = element in content
    override fun containsAll(elements: Collection<Any>): Boolean = content.containsAll(elements)
    override fun indexOf(element: Any): Int = content.indexOf(element)
    override fun lastIndexOf(element: Any): Int = content.lastIndexOf(element)
    override fun isEmpty(): Boolean = content.isEmpty()
    override fun iterator(): Iterator<Any> = content.iterator()
    override fun listIterator(): ListIterator<Any> = content.listIterator()
    override fun listIterator(index: Int): ListIterator<Any> = content.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<Any> = content.subList(fromIndex, toIndex)

}