package me.anno.io.xml.generic

import me.anno.io.xml.ComparableStringBuilder
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.arrays.ByteArrayList
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

    fun readTypeUntilSpaceOrEnd(builder: ComparableStringBuilder, first: Int): ComparableStringBuilder {
        builder.clear()
        if (first >= 0) builder.append(first.toChar())
        while (true) {
            when (val char = input.read()) {
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
                else -> builder.append(char.toChar())
            }
        }
    }

    fun equals(str: StringBuilder, str1: String): Boolean {
        return str.length == str1.length && str.startsWith(str1)
    }

    fun skipString(startSymbol: Int) {
        while (true) {
            when (input.read()) {
                '\\'.code -> input.read()
                startSymbol, -1 -> return
                else -> {}
            }
        }
    }

    fun readCData(type: ComparableStringBuilder): String {
        type.append(last.toChar())
        type.append(getStringUntil("]]>"))
        appendStringUntil('<'.code, type)
        return type.substring(8)
    }

    fun getStringUntil(end: String): String {
        return getStringUntil(end, ByteArrayList(256)) ?: ""
    }

    fun readUntil(end: String) {
        getStringUntil(end, null)
    }

    fun getStringUntil(end: String, result: ByteArrayList?): String? {
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
            val here = input.read()
            if (here == -1) return result?.decodeToString()

            // add it to the result
            result?.add(here.toByte())
            length++

            // check if the end was reached
            buffer[0] = here.toChar()
            if (length >= endSize) {

                for ((i, target) in reversed.withIndex()) {
                    if (buffer[i] != target) continue@search
                }

                // end is reached -> return the buffer without the end
                return result?.decodeToString(result.size - endSize)
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
        return builder
    }

    fun appendStringUntil(endSymbol: Int, builder: ComparableStringBuilder) {
        while (true) {
            when (val char = input.read()) {
                '&'.code -> builder.append(readEscapeSequence())
                endSymbol, -1 -> return
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
            val c = input.read()
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
            when (val char = input.read()) {
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
        val sthElement = Any()

        @JvmStatic
        val endOfReader = Any()
    }
}