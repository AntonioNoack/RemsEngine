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

    @Throws(IndexOutOfBoundsException::class)
    fun format(sth: Any?) = format(sth.toString())

    @Throws(IndexOutOfBoundsException::class)
    fun format(str: String, indentation: String = "  ", lineBreakLength: Int = 10) =
        FormatHelper(str.length, indentation).format(str, lineBreakLength)

}