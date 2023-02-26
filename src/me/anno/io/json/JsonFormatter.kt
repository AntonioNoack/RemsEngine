package me.anno.io.json

object JsonFormatter {

    private class FormatHelper(size: Int, val indentation: String) {

        val result = StringBuilder(size * 2)
        val closingBracketStack = StringBuilder(16)

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
                breakLine()
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
                        if (result.endsWith(',')) result.append(' ')
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
                    ':' -> result.append(": ")
                    '"', '\'' -> {
                        // skip a string
                        breakIfHadClosingBracket()
                        if (shouldSwitchLine || closingBracketsInLine > 1) breakLine()
                        else if (result.endsWith(',')) result.append(' ')
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
                        if (shouldSwitchLine) breakLine()
                        else if (result.endsWith(',')) result.append(' ')
                        result.append(char)
                    }
                }
            }

            breakIfHadClosingBracket()
            return result.toString()
        }
    }

    fun format(sth: Any?) = format(sth.toString())

    fun format(map: Map<String, Any?>): String {
        val builder = StringBuilder()
        append(map, builder)
        return format(builder.toString())
    }

    fun append(map: Map<*, *>, builder: StringBuilder) {
        builder.append("{")
        var first = true
        for ((key, value) in map) {
            if (!first) builder.append(',')
            append(key.toString(), builder)
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
            is Char -> append(value.toString(), builder)
            is Byte, is Short, is Int, is Long, is Float, is Double ->
                builder.append(value.toString())
            is ByteArray -> builder.append('[').append(value.joinToString(",")).append(']')
            is CharArray -> append(String(value), builder)
            is ShortArray -> builder.append('[').append(value.joinToString(",")).append(']')
            is IntArray -> builder.append('[').append(value.joinToString(",")).append(']')
            is LongArray -> builder.append('[').append(value.joinToString(",")).append(']')
            is FloatArray -> builder.append('[').append(value.joinToString(",")).append(']')
            is DoubleArray -> builder.append('[').append(value.joinToString(",")).append(']')
            else -> append(value.toString(), builder)
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

    fun append(str: String, builder: StringBuilder) {
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