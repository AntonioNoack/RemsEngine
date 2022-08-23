package me.anno.io.yaml

import me.anno.io.files.FileReference
import me.anno.utils.strings.StringHelper.indexOf2
import me.anno.utils.strings.StringHelper.titlecase
import java.io.IOException
import kotlin.math.min

object YAMLReader {

    const val listKey = ""

    @Suppress("unused")
    fun parseYAML(file: FileReference, beautify: Boolean = true): YAMLNode {
        return parseYAML(file.readText(), beautify)
    }

    /**
     * reads the yaml file
     * @param beautify removed m_ at the start of keys, and makes them all PascalCase for consistency
     * */
    fun parseYAML(text: String, beautify: Boolean = true): YAMLNode {

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
        val lines = text.split('\n')
        var lineIndex = -1
        val lineCount = lines.size
        while (++lineIndex < lineCount) {
            val line = lines[lineIndex]
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
                    add(depth + 1, listKey, null)
                    depth += 2
                    startIndex += 2
                    trimmed = trimmed.substring(2)
                }
                // process the line
                val colonIndex = if (trimmed.startsWith("{")) -1 else trimmed.indexOf(':')
                var key: String = trimmed
                var value: String? = null
                if (colonIndex > 0) {
                    key = trimmed.substring(0, colonIndex)
                    value = trimmed.substring(colonIndex + 1).trim()
                    while (value.startsWith("{") && !value.endsWith("}")) {
                        // the line was too long, and unity introduced a line wrap...
                        // I have no idea, whether that is legal in YAML...
                        value += lines[++lineIndex].trim()
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


    fun parseYAMLxJSON(json: String, callback: (String, String) -> Unit) {
        parseYAMLxJSON(json, false, callback)
    }

    /**
     * decodes stuff like "{fileID: 42575496, guid: ee81afb80bd, type: 2}" or {x:12, y:4, z: 13}
     * */
    fun parseYAMLxJSON(json: String, beautify: Boolean, callback: (String, String) -> Unit) {
        val start = json.indexOf('{')
        if (start < 0) return
        var i = start + 1
        val length = json.length
        while (i < length) {
            while (i < length && json[i] == ' ') i++
            var colonIndex = json.indexOf(':', i + 1)
            if (colonIndex < 0) return
            var key = json.substring(i, colonIndex).trim()
            if (json[colonIndex + 1] == ' ') colonIndex++ // skip that space, that's always there
            val commaIndex = json.indexOf2(',', colonIndex + 1)
            val bracketIndex = json.indexOf2('}', colonIndex + 1)
            val endIndex = min(commaIndex, bracketIndex)
            val value = json.substring(colonIndex + 1, endIndex).trim()
            if (beautify) key = beautify(key)
            callback(key, value)
            i = endIndex + 1
        }
    }

    fun beautify(key0: String): String {
        var key = key0
        if (key.startsWith("m_")) {
            key = key.substring(2)
        }
        if (key.startsWith("_")) {
            key = key.substring(1)
        }
        return key.titlecase()
    }

}