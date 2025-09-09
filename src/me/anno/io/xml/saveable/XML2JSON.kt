package me.anno.io.xml.saveable

import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.SimpleType
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLWriter.escapeXML
import me.anno.utils.structures.lists.Lists.any2

object XML2JSON {

    // convert XML into JSON and vice versa
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
                node["isList"] = "1"
                for (i in json.indices) {
                    val child = toXML(LIST_ITEM_CLASS, json[i])
                    val clazz = child["class"]
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
                    val k0 = k.toString()
                    var ks = k0
                        .replace("[]", "ZZ")
                        .replaceFirst(':', '.')
                    if (ks == "i.*ptr") ks = "ptr"
                    val type = getType(k0)
                    if (isSimpleType(type) && isSmall(v)) {
                        node[ks] = if (v is Number || v is String || v is Boolean) v.toString()
                        else smallString(type, v)
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

    fun getType(typeName: String): String {
        if (typeName == "class") return typeName
        val i = typeName.indexOf(':')
        if (i < 0) return ""
        val type = typeName.substring(0, i)
        return type
    }

    private fun isSimpleType(type: String): Boolean {
        if (type == "class") return true
        return SimpleType.entries.any2 { it.array2d == type || it.array == type || it.scalar == type }
    }

    private fun smallString(type: String, value: Any?): String {
        return when (value) {
            is String -> {
                if (value.toDoubleOrNull() == null || type == SimpleType.CHAR.scalar) {
                    escape(value)
                } else value
            }
            is List<*> -> {
                val isArray = type.endsWith("[]")
                val type1 = if (isArray) type.substring(0, type.length - 2) else type
                value.indices.joinToString(", ", "[", "]") {
                    smallString(if (isArray && it == 0) "i" else type1, value[it])
                }
            }
            else -> value.toString()
        }
    }

    private fun escape(value: String): String {
        val builder = StringBuilder(value.length + 2) // +2 for quotes
        JsonFormatter.appendEscapedString(value, builder)
        return builder.toString()
    }

    private fun replace(k: String): String {
        if (k == "ptr") return "i:*ptr"
        return k.replace("ZZ", "[]")
            .replaceFirst('.', ':')
    }

    /**
     * returns json-like
     * */
    fun fromXML(xml: XMLNode, setClass: Boolean = true): Any {
        if (xml.attributes.size == 1 && xml.attributes["isList"] == "1" &&
            xml.children.all { it is XMLNode }
        ) {
            // create an array
            return xml.children.filterIsInstance<XMLNode>().map {
                fromXML(it, it.type != LIST_ITEM_CLASS)
            }
        } else if (xml.type == LIST_ITEM_CLASS && xml.attributes.isEmpty() &&
            xml.children.size == 1 && isSmall(xml.children[0])
        ) {
            return xml.children[0].toString().trim()
        } else if (xml.attributes.isEmpty() && xml.children.size == 1 &&
            isSmall(xml.children[0])
        ) {
            return xml.children[0].toString().trim()
        } else {
            // create object
            val json = LinkedHashMap<String, Any?>()
            if (setClass) {
                var type = xml.type
                val dotIdx = type.indexOf('.')
                if (dotIdx > 0) {
                    type = type.substring(0, dotIdx)
                }
                json["class"] = replace(type)
            }
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
            null, is String, is Number, is Boolean, is Char -> true
            is List<*> -> json.all { isSmall(it) }
            else -> false
        }
    }
}