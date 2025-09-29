package me.anno.io.yaml.generic

import me.anno.io.json.generic.JsonReader.Companion.toHex
import me.anno.utils.types.Strings.joinChars
import me.anno.utils.types.Strings.titlecase
import java.io.BufferedReader
import java.io.IOException
import kotlin.math.max

@Deprecated("Please use YAMLReaderV2")
object YAMLReader {

    const val LIST_KEY = ""
    const val ROOT_NODE_KEY = "root"

    fun findColon(str: String, needsSpaceAfterColon: Boolean): Int {
        val i0 = str.indexOf(':')
        if (needsSpaceAfterColon) {
            var i = i0
            while (true) {
                if (i >= 0 && i + 1 < str.length && str[i + 1] != ' ') {
                    i = str.indexOf(':', i + 1) // find the next place
                } else return max(i, i0) // i will be -1 or a good position, i0 is the first colon
            }
        } else return i0
    }

    fun parseEscapedString(source: CharSequence, i0: Int, i1: Int): String {
        var i = i0
        val builder = StringBuilder(max(i1 - i0, 16))
        while (i < i1 - 1) { // check for escape sequences; last one doesn't need that care
            fun undoReadingEscapeSequence() {
                i-- // undo reading value
                builder.append('\\')
            }
            when (val c = source[i++]) {
                '\\' -> when (val ci = source[i++]) {
                    '\'', '\\', '"' -> builder.append(ci)
                    'n' -> builder.append('\n')
                    'r' -> builder.append('\r')
                    't' -> builder.append('\t')
                    'b' -> builder.append('\b')
                    'f' -> builder.append(12.toChar())
                    'u' -> {
                        if (i + 4 < i1) {
                            val c0 = source[i++]
                            val c1 = source[i++]
                            val c2 = source[i++]
                            val c3 = source[i++]
                            val code = toHex(c0, c1, c2, c3)
                            builder.append(code.joinChars())
                        } else undoReadingEscapeSequence()
                    }
                    else -> undoReadingEscapeSequence()
                }
                else -> builder.append(c)
            }
        }
        if (i < i1) {
            builder.append(source[i++])
        }
        return builder.toString()
    }

    /**
     * reads the yaml file
     * @param beautify removed m_ at the start of keys, and makes them all PascalCase for consistency
     * */
    fun parseYAML(reader: BufferedReader, beautify: Boolean = true): YAMLNode {

        val root = YAMLNode(ROOT_NODE_KEY, -1)
        val stack = ArrayList<YAMLNode>(8)
        stack.add(root)

        var lastTreeDepth = 0
        var lastDepth = -1
        var size = 0

        // will this be inlined? mmh...
        fun add(depth: Int, key: String, value0: String?) {
            var value1 = value0
            if (value0 != null &&
                (value0.startsWith('\'') || value0.startsWith('"')) &&
                value0.first() == value0.last()
            ) {
                value1 = parseEscapedString(value0, 1, value0.length - 1)
            }
            // find the depth in the stack
            // if the value is JSON-like object, parse it as well
            val node = YAMLNode(key, depth, value1)
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
                    if (treeDepth < 0) throw IOException("Went backwards, but this depth has never appeared before: $depth out of ${stack.joinToString { "${it.depth}" }}, $key: $value1")
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
                while (trimmed.startsWith("- ")) {
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
                val colonIndex = if (trimmed.startsWith("{")) -1 else findColon(trimmed, true)
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