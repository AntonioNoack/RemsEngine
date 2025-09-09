package me.anno.io.xml.generic

import me.anno.io.xml.ComparableStringBuilder
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Strings.joinChars
import org.apache.logging.log4j.LogManager
import java.io.Reader
import kotlin.math.min

/**
 * Provides base functions for XMLReader and XMLScanner
 * */
abstract class XMLReaderBase(val input: Reader) {

    var last: Int = 0
    val type0 = ComparableStringBuilder()

    val keyBuilder = ComparableStringBuilder()
    val valueBuilder = ComparableStringBuilder()

    fun readChar(): Int {
        val prev = last
        if (prev != 0) {
            if (prev > 0) last = 0
            return prev
        }
        return input.read()
    }

    fun readTypeUntilSpaceOrEnd(builder: ComparableStringBuilder, first: Int): ComparableStringBuilder {
        builder.clear()
        if (first >= 0) builder.append(first.toChar())
        while (true) {
            when (val char = readChar()) {
                ' '.code, '\t'.code, '\r'.code, '\n'.code,
                '>'.code, '='.code,
                -1 -> {
                    last = char
                    return builder
                }
                '/'.code -> {
                    if (builder.isEmpty()) {
                        builder.append(char.toChar())
                    } else {
                        last = char
                        return builder
                    }
                }
                else -> {
                    builder.append(char.toChar())
                    if (builder.equals("![cdata[")) {
                        last = 0
                        return builder
                    }
                }
            }
        }
    }

    fun equals(str: StringBuilder, str1: String): Boolean {
        return str.length == str1.length && str.startsWith(str1)
    }

    fun readCData(type: ComparableStringBuilder): String {
        if (last > 0) type.append(last.toChar())
        last = 0
        getStringUntil("]]>", type)
        appendStringUntil('<'.code, type)
        return type.substring(8)
    }

    fun readUntil(end: String) {
        getStringUntil(end)
    }

    fun getStringUntil(end: String) {
        val endSize = end.length
        val reversed = end.reversed()
        val buffer = CharArray(endSize)
        var length = 0
        search@ while (true) {

            // push the checking buffer forward
            for (i in 1 until min(length + 1, endSize)) {
                buffer[i] = buffer[i - 1]
            }

            // read the next char
            val here = readChar()
            if (here == -1) return

            length++

            // check if the end was reached
            buffer[0] = here.toChar()
            if (length >= endSize) {

                for ((i, target) in reversed.withIndex()) {
                    if (buffer[i] != target) continue@search
                }

                // end is reached -> return the buffer without the end
                return
            }
        }
    }

    fun getStringUntil(end: String, result: ComparableStringBuilder) {
        var length = 0
        search@ while (true) {

            // read the next char
            val here = readChar()
            if (here == -1) return

            // add it to the result
            result.append(here.toChar())
            length++

            // check if the end was reached
            if (result.endsWith(end)) {
                // end is reached -> return the buffer without the end
                result.length -= end.length
                return
            }
        }
    }

    fun readStringUntilNextNode(first: Int): String {
        val builder = valueBuilder
        builder.clear()
        if (first != 0) builder.append(first.toChar())
        appendStringUntil('<'.code, builder)
        // trim trailing whitespace
        while (builder.isNotEmpty() && builder.last().isWhitespace()) builder.length--
        return builder.toString()
    }

    fun readStringUntilQuotes(quotesSymbol: Int): ComparableStringBuilder {
        val builder = valueBuilder
        builder.clear()
        appendStringUntil(quotesSymbol, builder)
        last = 0
        return builder
    }

    fun appendStringUntil(endSymbol: Int, builder: ComparableStringBuilder) {
        while (true) {
            when (val char = readChar()) {
                '&'.code -> builder.append(readEscapeSequence())
                endSymbol, -1 -> {
                    last = char
                    return
                }
                else -> builder.append(char.toChar())
            }
        }
    }

    fun readEscapeSequence(): String {
        // apos -> '
        // quot -> "
        // #102 -> decimal
        // #x4f -> hex
        val builder = StringBuilder()
        while (true) {
            val c = readChar()
            if (c < 0 || c == ';'.code) break
            builder.append(c.toChar())
        }
        return when {
            equals(builder, "apos") -> "'"
            equals(builder, "quot") -> "\""
            equals(builder, "amp") -> "&"
            equals(builder, "lt") -> "<"
            equals(builder, "gt") -> ">"
            builder.startsWith("#x") -> {
                builder.substring(2).toInt(16)
                    .joinChars().toString()
            }
            builder.startsWith("#") -> {
                builder.substring(1).toInt()
                    .joinChars().toString()
            }
            else -> {
                LOGGER.warn("Unknown escape sequence $builder")
                return ""
            }
        }
    }

    fun assert(a: Int, b: Char, c: Char) {
        val ac = a.toChar()
        assertTrue(ac == b || ac == c) {
            "Expected $b or $c, but got ${a.toChar()}"
        }
    }

    fun skipSpaces(): Int {
        while (true) {
            when (val char = readChar()) {
                ' '.code,
                '\t'.code,
                '\r'.code,
                '\n'.code -> {
                }
                else -> return char // includes -1
            }
        }
    }

    companion object {

        private val LOGGER = LogManager.getLogger(XMLReaderBase::class)

        @JvmStatic
        val endElement = Any()

        @JvmStatic
        val endOfReader = Any()
    }
}