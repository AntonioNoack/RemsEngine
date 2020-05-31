package me.anno.io.xml

import me.anno.utils.Tabs

class XMLElement(val type: String){
    val properties = HashMap<String, String>()
    val children = ArrayList<Any>()

    operator fun get(key: String): String? = properties[key]
    operator fun set(key: String, value: String){
        properties[key] = value
    }

    fun toString(depth: Int): String {
        val tabs = Tabs.spaces(depth * 2)
        return if(children.isEmpty()){
            "$tabs<$type ${properties.entries.joinToString(" "){ "${it.key}=\"${it.value}\"" }}/>" +
                    if(depth == 0) "" else "\n"
        } else {
            "$tabs<$type ${properties.entries.joinToString(" "){ "${it.key}=\"${it.value}\"" }}>\n" +
                    children.joinToString(""){ (it as? XMLElement)?.toString(depth+1) ?: it.toString() } +
                    if(depth == 0) "$tabs</$type>" else "$tabs</$type>\n"
        }
    }
    override fun toString() = toString(0)

}