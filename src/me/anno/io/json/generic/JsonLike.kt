package me.anno.io.json.generic

import me.anno.engine.EngineBase
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.SimpleType
import me.anno.io.saveable.Saveable
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.io.xml.generic.XMLWriter
import me.anno.io.xml.saveable.XML2JSON
import me.anno.io.yaml.generic.YAMLReader
import me.anno.io.yaml.saveable.YAML2JSON
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFail
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.AnyToDouble
import me.anno.utils.types.AnyToInt
import me.anno.utils.types.AnyToLong

/**
 * converts values from jsonLike-format to saveable-compatible-json
 * */
object JsonLike {

    const val MAIN_NODE_NAME = "RemsEngine"

    fun decodeJsonLike(jsonLike: Any?, safely: Boolean): List<Saveable> {
        val json = jsonLikeToJson(jsonLike)
        return JsonStringReader.read(json, EngineBase.workspace, safely)
    }

    fun yamlBytesToJsonLike(bytes: ByteArray): Any? {
        val reader = bytes.inputStream().bufferedReader()
        val node = YAMLReader.parseYAML(reader, beautify = false)
        return YAML2JSON.fromYAML(node)
    }

    fun xmlBytesToJsonLike(bytes: ByteArray): Any {
        val str = bytes.inputStream().reader()
        val node = XMLReader().read(str) as XMLNode
        assertEquals(MAIN_NODE_NAME, node.type)
        return XML2JSON.fromXML(node)
    }

    fun jsonToYAML(json: String): String {
        val jsonNode = JsonReader(json).readArray()
        val yamlNode = YAML2JSON.toYAML(MAIN_NODE_NAME, jsonNode, 0)
        return yamlNode.toString()
    }

    fun jsonToXML(json: String, pretty: Boolean): String {
        val jsonNode = JsonReader(json).readArray()
        val xmlNode = XML2JSON.toXML(MAIN_NODE_NAME, jsonNode)
        val indentation = if (pretty) "  " else null
        return XMLWriter.write(xmlNode, indentation, true)
    }

    fun jsonLikeToJson(jsonLike: Any?): String {
        val root = ((jsonLike as? Map<*, *>)?.get(MAIN_NODE_NAME) ?: jsonLike) as List<*>
        val validated = root.map { validateJsonLike(it) }
        return JsonFormatter.format(validated, "", Int.MAX_VALUE)
    }

    fun validateJsonLike(jsonLike: Any?): Any? {
        return when (jsonLike) {
            is Map<*, *> -> validateJsonMap(jsonLike)
            else -> jsonLike
        }
    }

    fun validateJsonMap(jsonLike: Map<*, *>): Map<*, *> {
        return jsonLike.mapValues { (k, value) ->
            val key = k.toString()
            val idx = key.indexOf(':')
            if (idx >= 0) {
                val type = key.substring(0, idx)
                validateJsonProperty(type, value)
            } else value
        }
    }

    private fun skip(value: String, i: Int, str: String): Int {
        assertTrue(value.startsWith(str, i))
        return i + str.length
    }

    private fun splitJsonList(value: String, typeI: String, i0: Int): Pair<Int, ArrayList<Any?>> {
        val result = ArrayList<Any?>()
        assertEquals('[', value[i0])
        var i = i0 + 1
        val i1 = value.length - 1
        while (i < i1) {
            when (val c0 = value[i]) {
                ' ', '\t', '\r', '\n', ',' -> i++
                '\'', '"' -> {
                    val n0 = ++i
                    while (i < i1) {
                        when (value[i++]) {
                            '\\' -> i++
                            c0 -> break
                            else -> {}
                        }
                    }
                    val valueI = value.substring(n0, i - 1)
                    result.add(validateJsonProperty(typeI, valueI))
                }
                in '0'..'9', '+', '-', '.' -> {
                    val n0 = i++
                    while (i < i1) {
                        when (value[i]) {
                            in '0'..'9', '+', '-', '.', 'e', 'E' -> i++
                            else -> break
                        }
                    }
                    val str = value.substring(n0, i)
                    result.add(if ('.' in str) str.toDouble() else str.toLong())
                }
                't' -> {
                    i = skip(value, i, "true")
                    result.add(true)
                }
                'f' -> {
                    i = skip(value, i, "false")
                    result.add(false)
                }
                'n' -> {
                    i = skip(value, i, "null")
                    result.add(null)
                }
                '[' -> {
                    // we could also skip over the array, and let validateJsonProperty handle it
                    // (probably better, but a tiny bit slower)
                    val type1 =
                        if (typeI.endsWith("[]")) typeI.substring(0, typeI.length - 2)
                        else typeI
                    val (ix, listI) = splitJsonList(value, type1, i)
                    if (listI.isNotEmpty() && typeI.endsWith("[]")) {
                        listI[0] = AnyToInt.getInt(listI[0], 0)
                    }
                    result.add(listI)
                    i = ix
                }
                ']' -> return (i + 1) to result
                else -> assertFail("Unknown things")
            }
        }
        return (value.length) to result
    }

    fun validateJsonProperty(type: String, value: Any?): Any? {
        return when {
            isFloatType(type) -> AnyToDouble.getDouble(value, 0.0)
            isIntType(type) -> AnyToLong.getLong(value, 0L)
            type == SimpleType.CHAR.scalar -> value.toString()
            value is Map<*, *> -> validateJsonMap(value)
            value is String && type.endsWith("[]") &&
                    value.startsWith("[") && value.endsWith("]") -> {
                val type1 = type.substring(0, type.length - 2)
                val (i1, result) = splitJsonList(value, type1, 0)
                assertEquals(value.length, i1)
                assertTrue(result.isNotEmpty())
                result[0] = AnyToInt.getInt(result[0], 0)
                return result
            }
            value is String && !isStringType(type) &&
                    value.startsWith("[") && value.endsWith("]") -> {
                val type1 = type.substring(0, type.length - 2)
                val (i1, result) = splitJsonList(value, type1, 0)
                assertEquals(value.length, i1) {
                    "Expected last index to match, $value -> $result"
                }
                return result
            }
            value is List<*> && type.endsWith("[]") -> {
                val elementType = type.substring(0, type.length - 2)
                value.mapIndexed { idx, it ->
                    if (idx == 0) {
                        // first index stores the length
                        it.toString().toInt()
                    } else {
                        validateJsonProperty(elementType, it)
                    }
                }
            }
            value is List<*> && !isStringType(type) -> {
                val elementType = type.substring(0, type.length - 2)
                value.map { validateJsonProperty(elementType, it) }
            }
            value is String && !isStringType(type) && isInt32(value) -> {
                value.toLong()
            }
            else -> value
        }
    }

    private fun isInt32(value: String): Boolean {
        return value.isNotEmpty() && value.length <= 10 &&
                (value[0] in "+-" || value[0] in '0'..'9') &&
                (1 until value.length).all { value[it] in '0'..'9' }
    }

    private fun isFloatType(type: String): Boolean {
        return when (type) {
            SimpleType.FLOAT.scalar,
            SimpleType.DOUBLE.scalar -> true
            else -> false
        }
    }

    private fun isIntType(type: String): Boolean {
        return when (type) {
            SimpleType.BYTE.scalar,
            SimpleType.SHORT.scalar,
            SimpleType.INT.scalar,
            SimpleType.LONG.scalar -> true
            else -> false
        }
    }

    private fun isStringType(type: String): Boolean {
        return when (type) {
            SimpleType.STRING.scalar,
            SimpleType.REFERENCE.scalar,
            SimpleType.CHAR.scalar -> true
            else -> false
        }
    }
}