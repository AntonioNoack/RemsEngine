package me.anno.io.toml

import me.anno.io.json.generic.JsonFormatter.appendEscapedString

object TOMLWriter {

    fun write(data: Map<String, Any?>, builder: StringBuilder = StringBuilder()): StringBuilder {

        val groups = data.entries.groupBy { (key, _) ->
            val index = key.lastIndexOf('.')
            if (index < 0) {
                ""
            } else {
                key.substring(0, index)
            }
        }

        val root = groups[""]
        if (root != null) {
            writeGroup(builder, root)
        }

        for ((key, group) in groups) {
            if (key == "") continue
            builder.append('[').append(key).append("]\n")
            writeGroup(builder, group)
        }

        return builder
    }

    fun writeGroup(builder: StringBuilder, data: List<Map.Entry<String, Any?>>) {
        if (builder.isNotEmpty()) builder.append('\n')
        for ((key, value) in data) {
            builder.append(key).append(" = ")
            writeValue(builder, value)
            builder.append('\n')
        }
    }

    fun writeValue(builder: StringBuilder, value: Any?) {
        when (value) {
            null,
            is Boolean,
            is Byte,
            is Short,
            is Int,
            is Long,
            is Float,
            is Double -> builder.append(value)

            is CharSequence -> appendEscapedString(value, builder)

            is Map<*, *> -> {
                builder.append('{')
                for ((key, value) in value) {
                    if (!builder.endsWith('{')) builder.append(", ")
                    appendEscapedString(key.toString(), builder)
                    builder.append(": ")
                    writeValue(builder, value)
                }
                builder.append('}')
            }
            is Iterable<*> -> {
                builder.append('[')
                for (value in value) {
                    if (!builder.endsWith('[')) builder.append(", ")
                    writeValue(builder, value)
                }
                builder.append(']')
            }
            is Array<*> -> {
                builder.append('[')
                for (value in value) {
                    if (!builder.endsWith('[')) builder.append(", ")
                    writeValue(builder, value)
                }
                builder.append(']')
            }

            else -> appendEscapedString(value.toString(), builder)
        }
    }
}