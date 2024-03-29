package me.anno.io.xml.generic

import me.anno.utils.types.Strings

class XMLNode(val type: String) {

    val attributes: HashMap<String, String> = HashMap()
    val children: ArrayList<Any> = ArrayList()

    operator fun get(key: String): String? = attributes[key]
    operator fun set(key: String, value: String?) {
        if (value == null) attributes.remove(key)
        else attributes[key] = value
    }

    operator fun contains(key: String): Boolean = key in attributes

    fun toString(depth: Int): String {
        val tabs = Strings.spaces(depth * 2)
        return if (children.isEmpty()) {
            "$tabs<$type ${attributes.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }}/>" +
                    if (depth == 0) "" else "\n"
        } else {
            "$tabs<$type ${attributes.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }}>\n" +
                    children.joinToString("") { (it as? XMLNode)?.toString(depth + 1) ?: it.toString() } +
                    if (depth == 0) "$tabs</$type>" else "$tabs</$type>\n"
        }
    }

    override fun toString() = toString(0)

    @Suppress("unused")
    fun deepClone(): XMLNode {
        val clone = XMLNode(type)
        clone.attributes.putAll(attributes)
        clone.children.addAll(children.map {
            if (it is XMLNode) it.deepClone()
            else it
        })
        return clone
    }

    fun shallowClone(): XMLNode {
        val clone = XMLNode(type)
        clone.attributes.putAll(attributes)
        clone.children.addAll(children)
        return clone
    }

}