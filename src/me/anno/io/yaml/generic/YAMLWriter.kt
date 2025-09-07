package me.anno.io.yaml.generic

import me.anno.io.json.generic.JsonFormatter

object YAMLWriter {
    fun StringBuilder.appendYAML(yaml: Any): String {
        val inset = "  "
        fun write(value: Any?, depth: Int, isInList: Boolean) {
            when (value) {
                is Map<*, *> -> {
                    if (value.isNotEmpty()) {
                        var first = true
                        for ((key, value) in value) {
                            if (!isInList || !first) {
                                append('\n')
                                for (j in 0 until depth) append(inset)
                            }
                            append(key.toString()).append(": ")
                            write(value, depth + 1, false)
                            first = false
                        }
                    } else {
                        append("{}")
                    }
                }
                is List<*> -> {
                    if (value.all { it is Number || (it is String && it.toDoubleOrNull() != null) }) {
                        append("[")
                        for (i in value.indices) {
                            if (i > 0) append(", ")
                            append(value[i].toString())
                        }
                        append("]")
                    } else {
                        for (i in value.indices) {
                            if (!isInList || i > 0) {
                                append('\n')
                                for (j in 0 until depth) append(inset)
                            }
                            append("- ")
                            write(value[i], depth + 1, true)
                        }
                    }
                }
                is Number -> append(value.toString())
                else -> {
                    val value = value.toString()
                    if (value.isEmpty() || value.any {
                            it !in '0'..'9' &&
                                    it !in 'A'..'Z' &&
                                    it !in 'a'..'z' &&
                                    it !in ",.-_+#*!§$%&/="
                        }) {
                        JsonFormatter.appendEscapedString(value, this)
                    } else {
                        append(value)
                    }
                }
            }
        }
        write(yaml, 0, false)
        return toString()
    }

    fun yamlToString(yaml: Any): String {
        val writer = StringBuilder()
        writer.appendYAML(yaml)
        return writer.toString()
    }
}