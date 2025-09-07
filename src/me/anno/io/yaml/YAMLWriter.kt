package me.anno.io.yaml

import me.anno.io.json.generic.JsonFormatter

object YAMLWriter {
    fun yamlToString(yaml: Any): String {
        val writer = StringBuilder()
        val inset = "  "
        fun write(value: Any?, depth: Int, isInList: Boolean) {
            when (value) {
                is Map<*, *> -> {
                    if (value.isNotEmpty()) {
                        var first = true
                        for ((key, value) in value) {
                            if (!isInList || !first) {
                                writer.append('\n')
                                for (j in 0 until depth) writer.append(inset)
                            }
                            writer.append(key.toString()).append(": ")
                            write(value, depth + 1, false)
                            first = false
                        }
                    } else {
                        writer.append("{}")
                    }
                }
                is List<*> -> {
                    if (value.all { it is Number || (it is String && it.toDoubleOrNull() != null) }) {
                        writer.append("[")
                        for (i in value.indices) {
                            if (i > 0) writer.append(", ")
                            writer.append(value[i].toString())
                        }
                        writer.append("]")
                    } else {
                        for (i in value.indices) {
                            if (!isInList || i > 0) {
                                writer.append('\n')
                                for (j in 0 until depth) writer.append(inset)
                            }
                            writer.append("- ")
                            write(value[i], depth + 1, true)
                        }
                    }
                }
                is Number -> writer.append(value.toString())
                else -> {
                    val value = value.toString()
                    if (value.isEmpty() || value.any {
                            it !in '0'..'9' &&
                                    it !in 'A'..'Z' &&
                                    it !in 'a'..'z' &&
                                    it !in ",.-_+#*!§$%&/="
                        }) {
                        JsonFormatter.appendEscapedString(value, writer)
                    } else {
                        writer.append(value)
                    }
                }
            }
        }
        write(yaml, 0, false)
        return writer.toString()
    }
}