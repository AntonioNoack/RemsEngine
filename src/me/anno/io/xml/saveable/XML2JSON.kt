package me.anno.io.xml.saveable

import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLWriter.escapeXML

object XML2JSON {

    // todo make special symbols in lists work

    // convert XML into JSON and vice-versa
    //  object ->
    //   small values become attributes
    //   big values become nodes
    //  array of primitives ->
    //   could be copied 1:1, child

    const val LIST_ITEM_CLASS = "item"

    fun toXML(name: String, json: Any?): XMLNode {
        val node = XMLNode(name)
        when (json) {
            is List<*> -> {
                node.attributes["isList"] = "1"
                for (i in json.indices) {
                    val child = toXML(LIST_ITEM_CLASS, json[i])
                    val clazz = child.attributes["class"]
                    if (clazz != null) {
                        child.type = clazz
                        child.attributes.remove("class")
                    }
                    node.children.add(child)
                }
            }
            is Map<*, *> -> {
                for ((k, v) in json) {
                    // these key-replacements are to make it XML-spec-compliant
                    var ks = k.toString()
                        .replace("[]", "ZZ")
                        .replaceFirst(':', '.')
                    if (ks == "i.*ptr") ks = "ptr"
                    if (isSmall(v)) {
                        node.attributes[ks] = v.toString()
                    } else {
                        node.children.add(toXML(ks, v))
                    }
                }
            }
            is String -> node.children.add(escapeXML(json))
            else -> node.children.add(json.toString())
        }
        return node
    }

    private fun replace(k: String): String {
        return k.replace("ZZ", "[]")
            .replaceFirst('.', ':')
    }

    fun fromXML(xml: XMLNode, setClass: Boolean = true): Any {
        if (xml.attributes.size == 1 && xml.attributes["isList"] == "1" &&
            xml.children.all { it is XMLNode }
        ) {
            // create an array
            return xml.children.filterIsInstance<XMLNode>().map {
                fromXML(it, it.type != LIST_ITEM_CLASS)
            }
        } else if (xml.type == LIST_ITEM_CLASS && xml.attributes.isEmpty() &&
            xml.children.size == 1 && xml.children[0] is String
        ) { // a string/number value
            return xml.children[0].toString().trim()
        } else {
            // create object
            val json = LinkedHashMap<String, Any?>()
            if (setClass) json["class"] = replace(xml.type)
            for ((k, v) in xml.attributes) {
                json[replace(k)] = v
            }
            for (child in xml.children) {
                if (child is XMLNode) {
                    json[replace(child.type)] = fromXML(child)
                }
            }
            return json
        }
    }

    private fun isSmall(json: Any?): Boolean {
        return when (json) {
            null, is String, is Number, is Boolean -> true
            is List<*> -> json.all { isSmall(it) }
            else -> false
        }
    }
}