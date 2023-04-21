package me.anno.io.xml

import me.anno.utils.Tabs

class XMLNode(val type: String) {

    val properties: HashMap<String, String> = HashMap()
    val children: ArrayList<Any> = ArrayList()

    operator fun get(key: String): String? = properties[key]
    operator fun set(key: String, value: String?) {
        if (value == null) properties.remove(key)
        else properties[key] = value
    }

    operator fun contains(key: String): Boolean = key in properties

    fun toString(depth: Int): String {
        val tabs = Tabs.spaces(depth * 2)
        return if (children.isEmpty()) {
            "$tabs<$type ${properties.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }}/>" +
                    if (depth == 0) "" else "\n"
        } else {
            "$tabs<$type ${properties.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }}>\n" +
                    children.joinToString("") { (it as? XMLNode)?.toString(depth + 1) ?: it.toString() } +
                    if (depth == 0) "$tabs</$type>" else "$tabs</$type>\n"
        }
    }

    override fun toString() = toString(0)

    fun deepClone(): XMLNode {
        val clone = XMLNode(type)
        clone.properties.putAll(properties)
        clone.children.addAll(children.map {
            if (it is XMLNode) it.deepClone()
            else it
        })
        return clone
    }

    fun shallowClone(): XMLNode {
        val clone = XMLNode(type)
        clone.properties.putAll(properties)
        clone.children.addAll(children)
        return clone
    }

}