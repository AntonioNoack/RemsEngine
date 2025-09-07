package me.anno.io.yaml.generic

import me.anno.io.json.generic.JsonReader
import me.anno.io.yaml.generic.YAMLReader.LIST_KEY
import me.anno.io.yaml.generic.YAMLReader.findColon
import me.anno.utils.types.Strings.titlecase
import org.apache.logging.log4j.LogManager
import java.io.BufferedReader
import kotlin.math.max

/**
 * todo: replace our existing YAMLReader with this one in all places
 * */
object YAMLReaderV2 {

    private val LOGGER = LogManager.getLogger(YAMLReader::class)

    /**
     * reads the yaml file
     * @param beautify removed m_ at the start of keys, and makes them all PascalCase for consistency
     * */
    fun parseYAML(reader: BufferedReader, beautify: Boolean): Any {

        val root = LinkedHashMap<String, Any>()
        val stack = ArrayList<Pair<Int, Any>>(8)
        stack.add(-1 to root)

        fun findParent(depth: Int): Any {
            while (stack.size > 1 && stack.last().first >= depth) {
                stack.removeLast()
            }
            return stack.last().second
        }

        fun startList(depth: Int) {
            val parent = findParent(depth)
            when (parent) {
                is HashMap<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    parent as HashMap<String, Any>

                    val list = parent.getOrPut(LIST_KEY) { ArrayList<Any>() }
                    if (list is ArrayList<*>) {
                        @Suppress("UNCHECKED_CAST")
                        list as ArrayList<Any>
                        val childMap = LinkedHashMap<String, Any>()
                        list.add(childMap)
                        stack.add(depth to childMap)
                    } else LOGGER.warn("Type conflict ${list.javaClass} vs ArrayList")
                }
                is ArrayList<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    parent as ArrayList<Any>
                    val childMap = LinkedHashMap<String, Any>()
                    parent.add(childMap)
                    stack.add(depth to childMap)
                }
                else -> LOGGER.warn("Cannot start list on ${parent.javaClass}")
            }
        }

        fun addProperty(depth: Int, key: String, value: Any) {
            val parent = findParent(depth)
            when (parent) {
                is HashMap<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    parent as HashMap<String, Any>
                    parent[key] = value
                    stack.add(depth to value)
                }
                is ArrayList<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    parent as ArrayList<Any>
                    val wrapped = hashMapOf(key to value)
                    parent.add(wrapped)
                    stack.add(depth to wrapped)
                }
                else -> LOGGER.warn("Cannot append property to ${parent.javaClass}")
            }
        }

        fun addValue(depth: Int, value: Any) {
            val parent = findParent(depth)
            when (parent) {
                is HashMap<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    parent as HashMap<String, Any>
                    parent[LIST_KEY] = value
                }
                is ArrayList<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    parent as ArrayList<Any>
                    parent.add(value)
                }
                else -> LOGGER.warn("Cannot append value to ${parent.javaClass}")
            }
        }

        fun parseValue(value: String): Any {
            var value = value.trim()
            return when {
                value.startsWith('[') -> {
                    while (!value.endsWith("]")) {
                        // the line was too long, and unity introduced a line wrap...
                        // I have no idea, whether that is legal in YAML...
                        value += reader.readLine()?.trim() ?: break
                    }
                    JsonReader(value).readArray()
                }
                value.startsWith('{') -> {
                    while (!value.endsWith("}")) {
                        // the line was too long, and unity introduced a line wrap...
                        // I have no idea, whether that is legal in YAML...
                        value += reader.readLine()?.trim() ?: break
                    }
                    JsonReader(value).readObject()
                }
                value.startsWith('\'') -> {
                    // todo unescape things
                    while (!value.endsWith('\'')) {
                        // the line was too long, and unity introduced a line wrap...
                        // I have no idea, whether that is legal in YAML...
                        value += reader.readLine()?.trim() ?: break
                    }
                    value.substring(1, value.length - 1)
                }
                value.startsWith('"') -> {
                    // todo unescape things
                    while (!value.endsWith('"')) {
                        // the line was too long, and unity introduced a line wrap...
                        // I have no idea, whether that is legal in YAML...
                        value += reader.readLine()?.trim() ?: break
                    }
                    value.substring(1, value.length - 1)
                }
                value == "" -> LinkedHashMap<String, Any>()
                else -> value
            }
        }

        // parse yaml
        while (true) {
            val line = reader.readLine() ?: break
            var trimmed = line.trim()
            if (trimmed.isNotEmpty()) {

                var startIndex = line.indexOf(trimmed.first())
                var depth = startIndex
                while (trimmed.startsWith("- ")) {
                    // node in-between for this key-point
                    // this is sometimes needed:
                    // - name: leName
                    //   age: 17
                    //   desc: leDesc
                    // - name: leName2
                    //   age: 21
                    startList(depth + 1)
                    depth += 2
                    startIndex += 2
                    trimmed = trimmed.substring(2)
                }

                if (trimmed.startsWith('[') || trimmed.startsWith('{')) {
                    addValue(depth, parseValue(trimmed))
                } else {
                    // process the line
                    val colonIndex = findColon(trimmed, true)
                    var key: String = trimmed

                    if (colonIndex > 0) {
                        key = trimmed.substring(0, colonIndex).trim()
                        val value = parseValue(trimmed.substring(colonIndex + 1))
                        if (beautify) key = beautify(key)
                        addProperty(depth, key, value)
                    } else {
                        if (beautify) key = beautify(key)
                        addValue(depth, key)
                    }
                }
            }
        }

        return cleanValue(root)
    }

    fun cleanValue(value: Any): Any {
        return when (value) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                value as Map<String, Any>
                if (value.size == 1 && value.keys.first() == LIST_KEY) {
                    // replace the map with the list contained
                    cleanValue(value.values.first())
                } else {
                    value.mapValues { (key, valueI) ->
                        cleanValue(valueI)
                    }
                }
            }
            is List<*> -> {
                if (value.all { map -> map is Map<*, *> && map.size == 1 && map.keys.first() == LIST_KEY }) {
                    cleanValue(value.map { (it as Map<*, *>).values.first() })
                } else {
                    value.map { child -> cleanValue(child!!) }
                }
            }
            else -> value
        }
    }

    fun isListWrapper(value: Map<*, *>): Boolean {
        return value.size == 1 && value.keys.first() == LIST_KEY && value.values.first() is List<*>
    }

    fun beautify(key0: String): String {
        var key = key0
        if (key.startsWith("m_")) {
            key = key.substring(2)
        }
        if (key.startsWith("_")) {
            key = key.substring(1)
        }
        return key.titlecase().toString()
    }
}