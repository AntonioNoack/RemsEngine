package me.anno.io.json

class JsonArray : JsonNode(), List<Any?> {

    val content = ArrayList<Any?>()

    fun add(any: Any?) = content.add(any)

    override fun get(key: String): JsonNode? {
        val index = JsonValue.asInt(key, 0)
        if (index !in content.indices) return null
        return get(index).toJsonNode()
    }

    override fun toString() = content.toString()

    override fun get(index: Int): Any? = content[index]
    override val size: Int
        get() = content.size

    override fun contains(element: Any?) = element in content
    override fun containsAll(elements: Collection<Any?>) = content.containsAll(elements)
    override fun indexOf(element: Any?) = content.indexOf(element)
    override fun lastIndexOf(element: Any?) = content.lastIndexOf(element)
    override fun isEmpty() = content.isEmpty()
    override fun iterator() = content.iterator()
    override fun listIterator() = content.listIterator()
    override fun listIterator(index: Int) = content.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int) = content.subList(fromIndex, toIndex)

}