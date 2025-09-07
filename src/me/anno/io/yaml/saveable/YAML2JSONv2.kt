package me.anno.io.yaml.saveable

import me.anno.io.yaml.generic.YAMLReader.LIST_KEY
import me.anno.io.yaml.generic.YAMLReaderV2.isListWrapper

object YAML2JSONv2 {
    fun toYAML(name: String, json: Any?, depth: Int): Any? {
        return json
    }

    fun fromYAML(node: Any?): Any? {
        return if (node is Map<*, *> && isListWrapper(node)) node[LIST_KEY]
        else node as? List<*> ?: listOf(node)
    }
}