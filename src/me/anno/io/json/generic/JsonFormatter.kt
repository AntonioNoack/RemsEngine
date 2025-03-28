package me.anno.io.json.generic

object JsonFormatter {

    private class FormatHelper(size: Int, val indentation: String) {

        val result = StringBuilder(size * 2)
        val closingBracketStack = StringBuilder(16)
        val pretty = indentation.isNotEmpty()

        var shouldSwitchLine = false
        var depth = 0
        var lineStartIndex = 0
        var closingBracketsInLine = 0

        fun breakLine() {
            shouldSwitchLine = false
            result.append('\n')
            for (j in 0 until depth) {
                result.append(indentation)
            }
            lineStartIndex = result.length
            closingBracketsInLine = 0
        }

        fun breakIfHadClosingBracket() {
            if (closingBracketStack.isNotEmpty()) {
                if (pretty) breakLine()
                result.append(closingBracketStack)
                closingBracketsInLine = closingBracketStack.length
                closingBracketStack.clear()
            }
        }

        fun format(str: String, lineBreakLength: Int): String {

            var i = 0

            val size = str.length
            while (i < size) {
                when (val char = str[i++]) {
                    '[', '{' -> {
                        if (pretty && result.endsWith(',')) result.append(' ')
                        result.append(char)
                        depth++
                        // quicker ascent than directly switching lines
                        shouldSwitchLine = true
                    }
                    ']', '}' -> {
                        // quicker descent
                        if (closingBracketStack.length > 1) breakIfHadClosingBracket()
                        closingBracketStack.append(char)
                        depth--
                    }
                    ' ', '\t', '\r', '\n' -> {
                    } // skip, done automatically
                    ':' -> result.append(if (pretty) ": " else ":")
                    '"', '\'' -> {
                        // skip a string
                        breakIfHadClosingBracket()
                        if (pretty) {
                            if (shouldSwitchLine || closingBracketsInLine > 1) breakLine()
                            else if (result.endsWith(',')) result.append(' ')
                        }
                        result.append(char)
                        while (i < size) {
                            when (val c2 = str[i++]) {
                                char -> break
                                // just skip the next '"', could throw an IndexOutOfBoundsException
                                '\\' -> {
                                    result.append(c2)
                                    result.append(str[i++])
                                }
                                else -> result.append(c2)
                            }
                        }
                        result.append(char)
                    }
                    ',' -> {
                        breakIfHadClosingBracket()
                        result.append(char)
                        if (result.length - lineStartIndex > lineBreakLength) {
                            shouldSwitchLine = true
                        }
                    }
                    else -> {
                        breakIfHadClosingBracket()
                        if (pretty) {
                            if (shouldSwitchLine) breakLine()
                            else if (result.endsWith(',')) result.append(' ')
                        }
                        result.append(char)
                    }
                }
            }

            breakIfHadClosingBracket()
            return result.toString()
        }
    }

    fun format(v: Any?, indentation: String = "  ", lineBreakLength: Int = 50): String {
        return when (v) {
            is Map<*, *> -> format(v, indentation, lineBreakLength)
            is List<*> -> format(v, indentation, lineBreakLength)
            else -> format(v.toString(), indentation, lineBreakLength)
        }
    }

    fun format(v: Map<*, *>, indentation: String = "  ", lineBreakLength: Int = 50): String {
        val builder = StringBuilder()
        append(v, builder)
        return format(builder.toString(), indentation, lineBreakLength)
    }

    fun format(v: List<*>, indentation: String = "  ", lineBreakLength: Int = 50): String {
        val builder = StringBuilder()
        append(v, builder)
        return format(builder.toString(), indentation, lineBreakLength)
    }

    fun append(map: Map<*, *>, builder: StringBuilder) {
        builder.append("{")
        var first = true
        for ((key, value) in map) {
            if (!first) builder.append(',')
            appendEscapedString(key.toString(), builder)
            builder.append(":")
            append(value, builder)
            first = false
        }
        builder.append("}")
    }

    fun append(value: Any?, builder: StringBuilder) {
        when (value) {
            null, true, false -> builder.append(value)
            is List<*> -> append(value, builder)
            is Map<*, *> -> append(value, builder)
            is Char -> appendEscapedString(value.toString(), builder)
            is Byte, is Short, is Int, is Long, is Float, is Double ->
                builder.append(value.toString())
            is ByteArray -> builder.append('[').append(value.joinToString(",")).append(']')
            is CharArray -> appendEscapedString(value.concatToString(), builder)
            is ShortArray -> builder.append('[').append(value.joinToString(",")).append(']')
            is IntArray -> builder.append('[').append(value.joinToString(",")).append(']')
            is LongArray -> builder.append('[').append(value.joinToString(",")).append(']')
            is FloatArray -> builder.append('[').append(value.joinToString(",")).append(']')
            is DoubleArray -> builder.append('[').append(value.joinToString(",")).append(']')
            else -> appendEscapedString(value.toString(), builder)
        }
    }

    fun append(list: List<*>, builder: StringBuilder) {
        builder.append('[')
        var first = true
        for (value in list) {
            if (!first) builder.append(',')
            append(value, builder)
            first = false
        }
        builder.append(']')
    }

    fun appendEscapedString(str: String, builder: StringBuilder) {
        builder.ensureCapacity(builder.length + str.length + 2)
        builder.append('"')
        for (c in str) {
            when (c) {
                '\\', '"' -> builder.append('\\').append(c)
                '\n' -> builder.append('\\').append('n')
                '\r' -> builder.append('\\').append('r')
                else -> builder.append(c)
            }
        }
        builder.append('"')
    }

    fun format(str: String, indentation: String = "  ", lineBreakLength: Int = 10) =
        FormatHelper(str.length, indentation).format(str, lineBreakLength)
}