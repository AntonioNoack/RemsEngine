package me.anno.io.yaml.generic

import me.anno.utils.types.Strings.titlecase
import java.io.BufferedReader
import java.io.IOException

object YAMLReader {

    const val LIST_KEY = ""

    fun findColon(str: String): Int {
        var i = 0
        while (true) {
            val col = str.indexOf(':', i)
            if (col + 1 < str.length && str[col + 1] != ' ') {
                i = col + 1
            } else return col
        }
    }

    /**
     * reads the yaml file
     * @param beautify removed m_ at the start of keys, and makes them all PascalCase for consistency
     * */
    fun parseYAML(reader: BufferedReader, beautify: Boolean = true): YAMLNode {

        val root = YAMLNode("root", -1)
        val stack = ArrayList<YAMLNode>(8)
        stack.add(root)

        var lastTreeDepth = 0
        var lastDepth = -1
        var size = 0

        // will this be inlined? mmh...
        fun add(depth: Int, key: String, value: String?) {
            // find the depth in the stack
            // if the value is JSON-like object, parse it as well
            val node = YAMLNode(key, depth, value)
            when {
                depth == lastDepth -> {
                    // fine, just add to parent
                    stack[lastTreeDepth - 1].add(node)
                    stack[lastTreeDepth] = node
                }
                depth < lastDepth -> {
                    // go back
                    // how much?
                    val treeDepth = stack.binarySearch { it.depth.compareTo(depth) }
                    if (treeDepth < 0) throw IOException("Went backwards, but this depth has never appeared before: $depth out of ${stack.joinToString { "${it.depth}" }}, $key: $value")
                    for (i in stack.size - 1 downTo treeDepth + 1) stack.removeAt(i)
                    stack[treeDepth - 1].add(node)
                    stack[treeDepth] = node
                    lastDepth = depth
                    lastTreeDepth = treeDepth
                }
                else -> {
                    // go deeper
                    stack[lastTreeDepth].add(node)
                    stack.add(node)
                    lastTreeDepth++
                    lastDepth = depth
                }
            }
            size++
        }

        // parse yaml
        while (true) {
            val line = reader.readLine() ?: break
            var trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                var startIndex = line.indexOf(trimmed.first())
                var depth = startIndex
                if (trimmed.startsWith("- ")) {
                    // node in-between for this key-point
                    // this is sometimes needed:
                    // - name: leName
                    //   age: 17
                    //   desc: leDesc
                    // - name: leName2
                    //   age: 21
                    add(depth + 1, LIST_KEY, null)
                    depth += 2
                    startIndex += 2
                    trimmed = trimmed.substring(2)
                }
                // process the line
                val colonIndex = if (trimmed.startsWith("{")) -1 else findColon(trimmed)
                var key: String = trimmed
                var value: String? = null
                if (colonIndex > 0) {
                    key = trimmed.substring(0, colonIndex)
                    value = trimmed.substring(colonIndex + 1).trim()
                    while (value.startsWith("{") && !value.endsWith("}")) {
                        // the line was too long, and unity introduced a line wrap...
                        // I have no idea, whether that is legal in YAML...
                        value += reader.readLine()?.trim() ?: break
                    }
                }
                if (trimmed.startsWith("{")) {
                    value = key
                    key = ""
                }
                if (beautify) key = beautify(key)
                add(depth, key, value)
            }
        }

        return root
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