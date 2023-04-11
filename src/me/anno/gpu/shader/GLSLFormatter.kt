package me.anno.gpu.shader

object GLSLFormatter {

    fun String.formatGLSL(): String {
        return FormatHelper(length, "  ").format(this)
    }

    private class FormatHelper(size: Int, val indentation: String) {

        val result = StringBuilder(size * 2)

        var shouldSwitchLine = false
        var depth = 0

        fun breakLine() {
            shouldSwitchLine = false
            result.append('\n')
            for (j in 0 until depth) {
                result.append(indentation)
            }
        }

        fun format(str: String): String {
            var i = 0
            val size = str.length
            while (i < size) {
                when (val char = str[i++]) {
                    '{' -> {
                        if (result.endsWith(',')) result.append(' ')
                        result.append(char)
                        depth++
                        // quicker ascent than directly switching lines
                        shouldSwitchLine = true
                    }
                    '}' -> {
                        depth--
                        breakLine()
                        result.append(char)
                    }
                    '\r', '\n' -> {
                        shouldSwitchLine = true
                    }
                    ' ', '\t' -> {
                        if (shouldSwitchLine) breakLine()
                        if (!(result.endsWith(' ') || result.endsWith('\t'))) {
                            result.append(' ')
                        }
                    }
                    else -> {
                        if (shouldSwitchLine) breakLine()
                        else if (result.endsWith(',')) result.append(' ')
                        result.append(char)
                    }
                }
            }
            return result.toString()
        }
    }
}