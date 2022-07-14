package me.anno.io.json

object JsonFormatter {

    fun format(sth: Any?) = format(sth.toString())

    @Throws(IndexOutOfBoundsException::class)
    fun format(str: String, indentation: String = "  ", lineBreakLength: Int = 10): String {

        val size = str.length
        val sizeGuess = size * 2
        val res = StringBuilder(sizeGuess)
        val eoo = StringBuilder(16)

        var shouldSwitchLine = false
        var depth = 0
        var lineStartIndex = 0
        var closingBracketsInLine = 0

        fun breakLine() {
            shouldSwitchLine = false
            res.append('\n')
            for (j in 0 until depth) {
                res.append(indentation)
            }
            lineStartIndex = res.length
            closingBracketsInLine = 0
        }

        fun handleEOO() {
            if (eoo.isNotEmpty()) {
                breakLine()
                res.append(eoo)
                closingBracketsInLine = eoo.length
                eoo.clear()
            }
        }

        var i = 0

        fun handleString(char: Char) {
            handleEOO()
            if (shouldSwitchLine || closingBracketsInLine > 1) breakLine()
            else if (res.endsWith(',')) res.append(' ')
            res.append(char)
            while (i < size) {
                when (val c2 = str[i++]) {
                    char -> break
                    // just skip the next '"', could throw an IndexOutOfBoundsException
                    '\\' -> {
                        res.append(c2)
                        res.append(str[i++])
                    }
                    else -> res.append(c2)
                }
            }
            res.append(char)
        }

        while (i < size) {
            when (val char = str[i++]) {
                '[', '{' -> {
                    if (res.endsWith(',')) res.append(' ')
                    res.append(char)
                    depth++
                    // quicker ascent than directly switching lines
                    shouldSwitchLine = true
                }
                ']', '}' -> {
                    // quicker descent
                    if (eoo.length > 1) handleEOO()
                    eoo.append(char)
                    depth--
                }
                ' ', '\t', '\r', '\n' -> {
                } // skip, done automatically
                ':' -> res.append(": ")
                '"', '\'' -> handleString(char)
                ',' -> {
                    handleEOO()
                    res.append(char)
                    if (res.length - lineStartIndex > lineBreakLength) {
                        shouldSwitchLine = true
                    }
                }
                else -> {
                    handleEOO()
                    if (shouldSwitchLine) breakLine()
                    else if (res.endsWith(',')) res.append(' ')
                    res.append(char)
                }
            }
        }

        handleEOO()

        return res.toString()

    }

}