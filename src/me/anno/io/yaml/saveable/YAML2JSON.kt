package me.anno.io.yaml.saveable

import me.anno.io.yaml.generic.YAMLNode
import me.anno.io.yaml.generic.YAMLReader.LIST_KEY

object YAML2JSON {
    fun toYAML(name: String, json: Any?, depth: Int): YAMLNode {
        return when (json) {
            is List<*> -> {
                // optimization for long arrays, so they don't produce too many long lists
                if (json.all { it is Number } || json.all { it is String && it.toDoubleOrNull() != null }) {
                    YAMLNode(name, depth, json.joinToString(",", "[", "]"))
                } else {
                    YAMLNode(name, depth, null, json.map {
                        toYAML(LIST_KEY, it, depth + 1)
                    })
                }
            }
            is Map<*, *> -> {
                YAMLNode(name, depth, null, json.map { (k, v) ->
                    toYAML(k.toString(), v, depth + 2)
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