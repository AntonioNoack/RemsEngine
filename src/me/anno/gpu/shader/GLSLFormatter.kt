package me.anno.gpu.shader

import me.anno.utils.types.Strings.splitLines

// todo could be moved to utils...
/**
 * formats GLSL (our shading language used in the engine) to improve its readability
 * */
object GLSLFormatter {

    fun String.formatGLSL(): String {
        return FormatHelper(length, "  ").format(this)
    }

    /***
     * a little auto-formatting
     */
    fun indent(text: String): String {
        val lines = text.splitLines()
        val result = StringBuilder(lines.size * 3)
        var depth = 0
        for (i in lines.indices) {
            if (i > 0) result.append('\n')
            val line0 = lines[i]
            val line1 = line0.trim()
            var endIndex = line1.indexOf("//")
            if (endIndex < 0) endIndex = line1.length
            val line2 = line1.substring(0, endIndex)
            val depthDelta =
                line2.count { it == '(' || it == '{' || it == '[' } - line2.count { it == ')' || it == ']' || it == '}' }
            val firstDepth = when (line2.getOrElse(0) { '-' }) {
                ')', '}', ']' -> true
                else -> false
            }
            if (firstDepth) depth += depthDelta
            for (j in 0 until depth) {
                result.append("  ")
            }
            if (!firstDepth) depth += depthDelta
            result.append(line2)
        }
        return result.toString()
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