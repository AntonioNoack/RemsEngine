package me.anno.io.xml.generic

class XMLNode(var type: String) {

    val attributes: MutableMap<String, String> = LinkedHashMap()
    val children: ArrayList<Any> = ArrayList()

    operator fun get(key: String): String? = attributes[key]
    operator fun set(key: String, value: String?) {
        if (value == null) attributes.remove(key)
        else attributes[key] = value
    }

    operator fun contains(key: String): Boolean = key in attributes
    override fun toString(): String = XMLWriter.write(this, "  ", true)

    fun shallowClone(): XMLNode {
        val clone = XMLNode(type)
        clone.attributes.putAll(attributes)
        clone.children.addAll(children)
        return clone
    }
}