package me.anno.io.yaml.saveable

import me.anno.io.json.saveable.SimpleType
import me.anno.io.xml.saveable.XML2JSON
import me.anno.io.yaml.generic.YAMLNode
import me.anno.io.yaml.generic.YAMLReader.LIST_KEY

object YAML2JSON {
    fun toYAML(name: String, json: Any?, depth: Int): YAMLNode {
        return when (json) {
            is List<*> -> {
                // optimization for long arrays, so they don't produce too many long lists
                val type = XML2JSON.getType(name)
                val typeAllowsInline = type != SimpleType.CHAR.array && type != SimpleType.CHAR.array2d
                val valueAllowsInline = json.all { it is Number } ||
                        json.all { it is String && it.toDoubleOrNull() != null }
                if (typeAllowsInline && valueAllowsInline) {
                    YAMLNode(name, depth, json.joinToString(",", "[", "]"))
                } else {
                    YAMLNode(name, depth, null, json.map {
                        toYAML(LIST_KEY, it, depth + 2)
                    })
                }
            }
            is Map<*, *> -> {
                YAMLNode(name, depth, null, json.map { (k, value) ->
                    val key = k.toString()
                    toYAML(key, value, depth + 2)
                })
            }
            else -> YAMLNode(name, depth, json.toString())
        }
    }

    fun fromYAML(node: YAMLNode): Any? {
        return if (node.children.isEmpty()) {
            val value = node.value
            value?.toLongOrNull() ?: value?.toDoubleOrNull() ?: value // prefer numbers to strings
        } else if (node.children.all { it.key == LIST_KEY }) {
            // create an array
            node.children.map(::fromYAML)
        } else if (node.children.size == 1 && node.children.all { it.value == null && it.children.isEmpty() }) {
            node.children[0].key
        } else {
            // create object
            node.children.associate {
                it.key to fromYAML(it)
            }
        }
    }
}