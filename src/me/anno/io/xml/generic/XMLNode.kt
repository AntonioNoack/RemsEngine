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
    override fun toString(): String = XMLWriter.write(this)

    fun shallowClone(): XMLNode {
        val clone = XMLNode(type)
        clone.attributes.putAll(attributes)
        clone.children.addAll(children)
        return clone
    }

    override fun hashCode(): Int {
        var hash = type.hashCode()
        hash = hash * 31 + attributes.size
        hash = hash * 31 + children.size
        return hash
    }

    override fun equals(other: Any?): Boolean {
        return other === this || other is XMLNode &&
                other.type == type &&
                other.attributes == attributes &&
                other.children == children
    }
}