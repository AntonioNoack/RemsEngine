package me.anno.io.toml

import me.anno.io.json.generic.JsonReader
import org.apache.logging.log4j.LogManager

object TOMLReader {

    private val LOGGER = LogManager.getLogger(TOMLReader::class)

    fun read(text: CharSequence): Map<String, Any> {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("//") }
        var group = ""
        val result = HashMap<String, Any>()
        for (line in lines) {
            when {
                line.startsWith('[') && line.endsWith(']') -> {
                    group = line.substring(1, line.length - 1) + "."
                    if (group == ".") group = ""
                }
                line.contains('=') -> {
                    val index = line.indexOf('=')
                    val key = line.substring(0, index).trim()
                    val value = line.substring(index + 1).trim()
                    val value1 = parseValue(value)
                    if (value1 == null) {
                        LOGGER.warn("Could not parse value '$value' for $group$key")
                        continue
                    }
                    result["$group$key"] = value1
                }
            }
        }
        return result
    }

    fun parseValue(line: String): Any? {
        return when {
            line.isEmpty() -> null
            line == "true" -> true
            line == "false" -> false
            line.startsWith('"') && line.endsWith('"') -> parseString(line)
            line.startsWith("0b") -> line.substring(2).toLongOrNull(2)
            line.startsWith("0x") -> line.substring(2).toLongOrNull(16)
            line.first() in "{[" && line.last() in "]}" -> JsonReader(line).read()
            '.' in line -> line.toDoubleOrNull()
            else -> line.toLongOrNull()
        }
    }

    fun parseString(line: String): String {
        return JsonReader(line).readString()
    }
}