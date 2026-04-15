package me.anno.mesh.usd

import me.anno.utils.files.Files.formatFileSize
import org.apache.logging.log4j.LogManager

class USDAParser(private val text: String, private val path: String) {

    companion object {
        private val LOGGER = LogManager.getLogger(USDAParser::class)
    }

    init {
        check(path.endsWith(".usda", true)) { "Expected usda file, got $path" }
        println("Reading $path, ${text.length.formatFileSize()}")
    }

    private var index = 0

    fun parse(): USDPrim {

        // parse optional header
        val subLayers = parseHeader()

        val root = USDPrim("Root", "Root")
        root.properties["subLayers"] = subLayers

        while (skipWhitespace()) {
            val prim = parsePrim() ?: break
            root.children.add(prim)
        }

        return root
    }

    private fun parseHeader(): List<String> {

        if (!peek("(")) {
            LOGGER.warn("Missing header")
            return emptyList()
        }

        consume("(")

        val subLayers = ArrayList<String>()
        while (!peek(")")) {
            val key = readIdentifier()
            if (key == "subLayers") {
                consume("=")
                consume("[")

                while (!peek("]")) {
                    subLayers.add(readAssetPath())
                    if (peek(",")) consume(",")
                }

                consume("]")
            } else {
                LOGGER.warn("Ignoring header $key")
                // skip value
                skipUntilNextLineOrComma()
            }
        }

        consume(")")
        return subLayers
    }

    private fun skipUntilNextLineOrComma() {

        var depthParen = 0
        var depthBracket = 0

        while (index < text.length) {

            val c = text[index]

            when (c) {
                '(' -> depthParen++
                ')' -> {
                    if (depthParen == 0) return
                    depthParen--
                }

                '[', '{' -> depthBracket++
                ']', '}' -> {
                    if (depthBracket == 0) return
                    depthBracket--
                }

                ',' -> {
                    if (depthParen == 0 && depthBracket == 0) {
                        index++ // consume comma
                        return
                    }
                }

                '\n' -> {
                    if (depthParen == 0 && depthBracket == 0) {
                        index++ // consume newline
                        return
                    }
                }

                '"' -> {
                    skipString()
                    continue
                }
            }

            index++
        }
    }

    private fun skipString() {
        index++ // skip opening "
        while (index < text.length) {
            val c = text[index]
            if (c == '\\') {
                index += 2 // skip escaped char
            } else if (c == '"') {
                index++ // closing "
                return
            } else {
                index++
            }
        }
    }

    private fun readAssetPath(): String {
        consume("@")
        val start = index
        while (text[index] != '@') index++
        val path = text.substring(start, index)
        consume("@")
        println("read asset path: $path")
        return path
    }

    private fun parsePrim(): USDPrim? {
        skipWhitespace()
        if (!peek("def")) return null
        consume("def")

        val type = readIdentifier()
        val name = readString()
        println("reading prim $type $name")

        val prim = USDPrim(type, name)
        if (peek("(")) {
            parseMetadata(prim)
        }

        consume("{")
        while (!peek("}")) {
            skipWhitespace()
            if (peek("def")) {
                prim.children.add(parsePrim()!!)
            } else {
                parseProperty(prim)
            }
        }
        consume("}")
        return prim
    }

    private fun parseMetadata(prim: USDPrim) {
        consume("(")
        while (!peek(")")) {

            skipWhitespace()

            // handle list ops like "prepend apiSchemas"
            val first = readIdentifier()

            val key = if (first in listOf("prepend", "append", "delete")) {
                val actual = readIdentifier()
                "$first $actual"
            } else first

            consume("=")

            val value = when {
                peek("[") -> readArrayGeneric("[", "]")
                peek("(") -> readArrayGeneric("(", ")")
                peek("\"") -> readString()
                peek("@") -> readAssetPath()
                peek("{") -> readDictionaryGeneric()
                else -> readNumberOrIdentifier()
            }

            // store in properties for now (simplest)
            prim.properties[key] = value

            skipWhitespace()
            if (peek(",")) consume(",")
        }

        consume(")")
    }

    private fun readArrayGeneric(start: String, end: String): List<Any> {

        consume(start)
        val list = ArrayList<Any>()

        while (index < text.length && !peek(end)) {

            val value = when {
                peek("\"") -> readString()
                peek("@") -> readAssetPath()
                peek("[") -> readArrayGeneric("[", "]")
                peek("(") -> readArrayGeneric("(", ")")
                else -> readNumberOrIdentifier()
            }

            println("read array entry $value")
            list.add(value)

            skipWhitespace()
            if (peek(",")) consume(",")
            else break
        }

        consume(end)
        return list
    }

    private fun readDictionaryGeneric(): Map<String, Any?> {
        consume("{")
        val list = HashMap<String, Any?>()

        while (index < text.length && !peek("}")) {
            var token = readIdentifier()
            println("identifier $token")
            // handle optional specifiers like "custom", "uniform"
            if (token == "custom" || token == "uniform") {
                token = readIdentifier()
            }

            // otherwise: attribute
            val name = readIdentifier()
            println("reading $token $name")
            consume("=")

            val value = parseValue()

            println("read dict entry $name = $value")
            list[name] = value

            skipWhitespace()
            if (peek(",")) consume(",")
            else break
        }

        consume("}")
        return list
    }

    private fun parseProperty(prim: USDPrim) {
        var token = readIdentifier()
        // handle optional specifiers like "custom", "uniform"
        if (token == "custom" || token == "uniform") {
            token = readIdentifier()
        }

        when (token) {
            "rel" -> {
                parseRelationship(prim)
                return
            }
            "instanceable" -> {
                consume("=")
                prim.isInstance = readIdentifier() == "true"
                return
            }
            "references" -> {
                consume("=")
                prim.references.add(parseReference())
                return
            }
        }

        // otherwise: attribute
        val name = readIdentifier()
        println("reading $token $name")
        consume("=")

        val value = parseValue()
        prim.properties[name] = value
    }

    private fun parseRelationship(prim: USDPrim) {

        val name = readIdentifier()
        consume("=")

        val value = parseValue()

        // USD allows None or paths
        if (value is String) {
            prim.relationships[name] = value
        } else {
            prim.relationships[name] = null // None case
        }
    }

    private fun parseValue(): Any? {
        return when {
            peek("[") -> readArrayGeneric("[", "]")
            peek("(") -> readArrayGeneric("(", ")")
            peek("\"") -> readString()
            peek("@") -> readAssetPath()
            peek("<") -> readPath()
            peek("{") -> readDictionaryGeneric()

            else -> {
                when (val id = readIdentifier()) {
                    "None" -> null
                    "true" -> true
                    "false" -> false
                    else -> {
                        println("id: '$id'")
                        id.toDoubleOrNull() ?: id
                    }
                }
            }
        }
    }

    private fun readPath(): String {
        consume("<")
        val start = index
        while (text[index] != '>') index++
        val path = text.substring(start, index)
        consume(">")
        return path
    }

    // --- helpers ---

    private fun readArray(): List<Float> {
        consume("[")
        val list = ArrayList<Float>()
        while (!peek("]")) {
            list.add(readNumber().toFloat())
            skipWhitespace()
            if (peek(",")) consume(",")
        }
        consume("]")
        return list
    }

    private fun readNumber(): Double {
        val start = index
        while (index < text.length && text[index] in "-+.0123456789eE") index++
        return text.substring(start, index).toDouble()
    }

    private fun readNumberOrIdentifier(): Any {
        val start = index
        while (index < text.length && !text[index].isWhitespace() && text[index] !in "})],") index++
        check(index > start) { "Expected identifier at ${err(index)}" }
        val str = text.substring(start, index)
        return str.toDoubleOrNull() ?: str
    }

    private fun readIdentifier(): String {
        skipWhitespace()
        val start = index
        while (index < text.length && (text[index].isLetterOrDigit() || text[index] in "_:[].+-")) index++
        check(index > start) { "Expected identifier at ${err(index)}" }
        return text.substring(start, index)
    }

    private fun readString(): String {
        consume("\"")
        val start = index
        while (text[index] != '"') index++
        val str = text.substring(start, index)
        consume("\"")
        return str
    }

    private fun skipWhitespace(): Boolean {
        while (true) {
            while (index < text.length && text[index].isWhitespace()) index++
            // skip comment
            if (index < text.length && text[index] == '#') {
                index = text.indexOf('\n', index)
                if (index < 0) index = text.length
            } else break
        }
        return index < text.length
    }

    private fun peek(s: String): Boolean {
        skipWhitespace()
        return text.startsWith(s, index)
    }

    private fun consume(s: String) {
        check(s.isNotEmpty())

        skipWhitespace()
        if (!text.startsWith(s, index)) {
            error("Expected $s, got ${err(index)}")
        }
        index += s.length
    }

    private fun parseReference(): USDReference {

        // @file.usda@</Path>
        val file = if (peek("@")) {
            consume("@")
            val start = index
            while (text[index] != '@') index++
            val file = text.substring(start, index)
            consume("@")
            file
        } else null

        val path = readPath()
        println("Parsed reference: $file,$path")
        return USDReference(file, path)
    }

    private fun err(index: Int): String {
        val sub = text.substring(0, index)
        val lineIndex = 1 + sub.count { it == '\n' }
        val prevLB = text.lastIndexOf('\n', index)
        return "${if (index < text.length) "'${text[index]}'" else "EOF"}, $lineIndex:${index - prevLB} in $path"
    }
}